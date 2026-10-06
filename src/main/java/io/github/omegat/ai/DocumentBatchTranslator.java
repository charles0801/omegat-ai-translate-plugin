package io.github.omegat.ai;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.omegat.core.Core;
import org.omegat.core.data.IProject;
import org.omegat.core.data.PrepareTMXEntry;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.data.TMXEntry;
import org.omegat.gui.editor.IEditor;
import org.omegat.gui.glossary.GlossaryEntry;
import org.omegat.util.TMXProp;
import org.omegat.util.TagUtil;

/** Runs requests sequentially without blocking the OmegaT event thread. */
final class DocumentBatchTranslator {
    private final OpenAiClient client;
    private final IProject project;
    private final ProviderConfig provider;
    private final String apiKey;
    private final List<WorkItem> items;
    private final JDialog dialog;
    private final JProgressBar progress;
    private final JLabel status;
    private final JButton cancelButton;
    private final AtomicBoolean cancelRequested = new AtomicBoolean();
    private final AtomicBoolean saving = new AtomicBoolean();
    private Thread worker;

    private DocumentBatchTranslator(OpenAiClient client, IProject project,
            ProviderConfig provider, String apiKey, List<WorkItem> items) {
        this.client = client;
        this.project = project;
        this.provider = provider;
        this.apiKey = apiKey;
        this.items = items;
        dialog = new JDialog(Core.getMainWindow().getApplicationFrame(),
                "Translate current document with AI", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                requestCancel();
            }
        });
        progress = new JProgressBar(0, items.size());
        progress.setStringPainted(true);
        status = new JLabel("Starting translation...");
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(event -> requestCancel());
        JPanel body = new JPanel(new BorderLayout(8, 8));
        body.add(status, BorderLayout.NORTH);
        body.add(progress, BorderLayout.CENTER);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(cancelButton);
        body.add(controls, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.setSize(440, 140);
        dialog.setLocationRelativeTo(Core.getMainWindow().getApplicationFrame());
    }

    static void start(AiTranslate adapter, OpenAiClient client, ProviderConfig provider) {
        IProject project = Core.getProject();
        IEditor editor = Core.getEditor();
        Component parent = Core.getMainWindow().getApplicationFrame();
        if (project == null || !project.isProjectLoaded() || editor == null
                || editor.getCurrentFile() == null) {
            JOptionPane.showMessageDialog(parent, "Open a project document first.");
            return;
        }
        if (provider == null) {
            JOptionPane.showMessageDialog(parent, "Configure an AI translation provider first.");
            return;
        }
        try {
            provider.validate();
        } catch (IllegalArgumentException error) {
            JOptionPane.showMessageDialog(parent, error.getMessage(), "AI Translate",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Commit an in-progress human edit before deciding which entries are untranslated.
        editor.commitAndLeave();
        String currentFile = editor.getCurrentFile();
        IProject.FileInfo file = project.getProjectFiles().stream()
                .filter(candidate -> candidate.filePath.equals(currentFile)).findFirst().orElse(null);
        if (file == null || file.entries == null) {
            JOptionPane.showMessageDialog(parent, "The current document is not available.");
            return;
        }
        List<WorkItem> items = plan(project, file);
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No untranslated segments in this document.");
            return;
        }
        String message = "Translate " + items.size() + " untranslated segment(s) in:\n"
                + currentFile + "\n\nExisting translations will not be replaced. "
                + "Requests may incur provider charges.\n"
                + "Cancel keeps any translations already completed.";
        if (JOptionPane.showConfirmDialog(parent, message, "Translate current document",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        DocumentBatchTranslator batch = new DocumentBatchTranslator(client, project,
                provider.copy(), adapter.getApiKey(provider.getId()), items);
        batch.worker = new Thread(batch::run, "OmegaT AI document translation");
        batch.worker.setDaemon(true);
        batch.worker.start();
        batch.dialog.setVisible(true);
    }

    private static List<WorkItem> plan(IProject project, IProject.FileInfo currentFile) {
        Set<String> sourcesInOtherFiles = new HashSet<>();
        for (IProject.FileInfo file : project.getProjectFiles()) {
            if (file == currentFile || file.entries == null) continue;
            for (SourceTextEntry entry : file.entries) {
                sourcesInOtherFiles.add(entry.getSrcText());
            }
        }
        List<WorkItem> result = new ArrayList<>();
        Set<String> plannedDefaults = new HashSet<>();
        for (SourceTextEntry entry : currentFile.entries) {
            if (entry.getSrcText().isBlank() || project.getTranslationInfo(entry).isTranslated()) {
                continue;
            }
            TMXEntry alternative = project.getAllTranslations(entry).getAlternativeTranslation();
            boolean existingAlternative = alternative != null
                    && entry.getSrcText().equals(alternative.getSourceText());
            boolean isDefault = !sourcesInOtherFiles.contains(entry.getSrcText())
                    && !existingAlternative;
            if (isDefault && !plannedDefaults.add(entry.getSrcText())) continue;
            result.add(new WorkItem(entry, isDefault));
        }
        return result;
    }

    private void requestCancel() {
        if (saving.get()) return;
        if (cancelRequested.compareAndSet(false, true)) {
            status.setText("Stopping after the current request...");
            cancelButton.setEnabled(false);
            if (worker != null) worker.interrupt();
        }
    }

    private void run() {
        int written = 0;
        int invalidTags = 0;
        String error = null;
        int completed = 0;
        try {
            for (WorkItem item : items) {
                if (cancelRequested.get()) break;
                if (Core.getProject() != project || !project.isProjectLoaded()) {
                    error = "The project changed during translation.";
                    break;
                }
                // A default translation may have filled a duplicate earlier in this run.
                if (project.getTranslationInfo(item.entry).isTranslated()) {
                    completed++;
                    updateProgress(completed);
                    continue;
                }
                Map<String, String> glossary = glossaryFor(item.entry);
                String translation = client.translate(provider, apiKey,
                        project.getProjectProperties().getSourceLanguage().toString(),
                        project.getProjectProperties().getTargetLanguage().toString(),
                        item.entry.getSrcText(), glossary);
                if (cancelRequested.get()) break;
                if (translation.isBlank() || !tagsMatch(item.entry, translation)) {
                    invalidTags++;
                } else if (writeIfStillUntranslated(item, translation)) {
                    written++;
                }
                completed++;
                updateProgress(completed);
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (!cancelRequested.get()) error = "Translation was interrupted.";
        } catch (Exception failure) {
            // Provider exceptions may contain request or response data; never display or log them.
            error = "Translation stopped because the provider request or project update failed.";
        } finally {
            Thread.interrupted(); // Allow a save of the completed portion after cancellation.
            if (written > 0 && Core.getProject() == project && project.isProjectLoaded()) {
                try {
                    saving.set(true);
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Saving completed translations...");
                        cancelButton.setEnabled(false);
                    });
                    Core.executeExclusively(true, () -> project.saveProject(false));
                    SwingUtilities.invokeAndWait(() -> Core.getEditor().refreshView(true));
                } catch (Exception saveFailure) {
                    error = "Translations were added, but saving failed. Save the project manually.";
                }
            }
            final int saved = written;
            final int skipped = invalidTags;
            final String finalError = error;
            final boolean cancelled = cancelRequested.get();
            SwingUtilities.invokeLater(() -> {
                dialog.dispose();
                String summary = saved + " segment(s) translated."
                        + (skipped == 0 ? "" : "\n" + skipped
                                + " segment(s) skipped because the response was empty or tags changed.")
                        + (cancelled ? "\nCancelled; completed translations were kept." : "")
                        + (finalError == null ? "" : "\n" + finalError);
                JOptionPane.showMessageDialog(Core.getMainWindow().getApplicationFrame(), summary,
                        "AI document translation", finalError == null
                                ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
            });
        }
    }

    private void updateProgress(int completed) {
        SwingUtilities.invokeLater(() -> {
            progress.setValue(completed);
            status.setText("Processed " + completed + " of " + items.size() + " segment(s)");
        });
    }

    private Map<String, String> glossaryFor(SourceTextEntry entry) {
        if (Core.getGlossaryManager() == null) return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<>();
        for (GlossaryEntry term : Core.getGlossaryManager().searchSourceMatches(entry)) {
            if (term.getSrcText() != null && term.getLocText() != null) {
                result.putIfAbsent(term.getSrcText(), term.getLocText());
            }
        }
        return result;
    }

    private boolean writeIfStillUntranslated(WorkItem item, String translation)
            throws InvocationTargetException, InterruptedException {
        AtomicBoolean result = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            if (cancelRequested.get() || Core.getProject() != project || !project.isProjectLoaded()
                    || project.getTranslationInfo(item.entry).isTranslated()) return;
            IProject.AllTranslations all = project.getAllTranslations(item.entry);
            TMXEntry previous = item.isDefault
                    ? all.getDefaultTranslation() : all.getAlternativeTranslation();
            PrepareTMXEntry prepared = new PrepareTMXEntry(previous);
            prepared.source = item.entry.getSrcText();
            prepared.translation = translation;
            prepared.changeDate = System.currentTimeMillis();
            prepared.changer = "AI Translate";
            if (prepared.creator == null) {
                prepared.creator = "AI Translate";
                prepared.creationDate = prepared.changeDate;
            }
            prepared.otherProperties = previous.getProperties() == null
                    ? new ArrayList<>() : new ArrayList<>(previous.getProperties());
            prepared.otherProperties.removeIf(prop -> "origin".equals(prop.getType()));
            prepared.otherProperties.add(new TMXProp("origin", "MT:[AI Translate]"));
            project.setTranslation(item.entry, prepared, item.isDefault, null);
            result.set(true);
        });
        return result.get();
    }

    static boolean tagsMatch(SourceTextEntry entry, String translation) {
        List<TagUtil.Tag> sourceTags = TagUtil.buildTagList(entry.getSrcText(),
                entry.getProtectedParts());
        List<TagUtil.Tag> targetTags = new ArrayList<>(TagUtil.buildTagList(translation,
                entry.getProtectedParts()));
        TagUtil.addExtraTags(targetTags, sourceTags, translation);
        return tagCounts(sourceTags).equals(tagCounts(targetTags));
    }

    private static Map<String, Integer> tagCounts(List<TagUtil.Tag> tags) {
        Map<String, Integer> counts = new HashMap<>();
        for (TagUtil.Tag tag : tags) counts.merge(tag.tag, 1, Integer::sum);
        return counts;
    }

    private static final class WorkItem {
        final SourceTextEntry entry;
        final boolean isDefault;

        WorkItem(SourceTextEntry entry, boolean isDefault) {
            this.entry = entry;
            this.isDefault = isDefault;
        }
    }
}
