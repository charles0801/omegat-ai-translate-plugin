package io.github.omegat.ai;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;

final class ProviderDialog extends JDialog {
    private final AiTranslate translator;
    private final ProviderRepository repository;
    private final List<ProviderConfig> providers;
    private final DefaultListModel<ProviderConfig> listModel = new DefaultListModel<>();
    private final JList<ProviderConfig> providerList = new JList<>(listModel);

    private final JTextField name = new JTextField();
    private final JTextField baseUrl = new JTextField();
    private final JTextField model = new JTextField();
    private final JTextField authHeader = new JTextField();
    private final JTextField authPrefix = new JTextField();
    private final JPasswordField apiKey = new JPasswordField();
    private final JCheckBox temporaryKey = new JCheckBox("Keep API key only until OmegaT exits");
    private final JSpinner temperature = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 2.0, 0.1));
    private final JSpinner timeout = new JSpinner(new SpinnerNumberModel(60, 1, 600, 1));
    private final JTextArea systemPrompt = new JTextArea();
    private final JTextArea userPrompt = new JTextArea();
    private ProviderConfig selected;

    static void show(Window parent, AiTranslate translator, ProviderRepository repository) {
        ProviderDialog dialog = new ProviderDialog(parent, translator, repository);
        dialog.setVisible(true);
    }

    private ProviderDialog(Window parent, AiTranslate translator, ProviderRepository repository) {
        super(parent, "AI Translate Providers", ModalityType.APPLICATION_MODAL);
        this.translator = translator;
        this.repository = repository;
        this.providers = new ArrayList<>(repository.load());
        buildUi();
        providers.forEach(listModel::addElement);
        String activeId = repository.activeId(providers);
        for (int i = 0; i < providers.size(); i++) {
            if (providers.get(i).getId().equals(activeId)) providerList.setSelectedIndex(i);
        }
        if (providerList.getSelectedIndex() < 0 && !providers.isEmpty()) providerList.setSelectedIndex(0);
        setMinimumSize(new Dimension(820, 600));
        setSize(900, 680);
        setLocationRelativeTo(parent);
    }

    private void buildUi() {
        setLayout(new BorderLayout(8, 8));
        providerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        providerList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) switchSelection(providerList.getSelectedValue());
        });

        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 0));
        left.add(new JLabel("Providers (selected = active)"), BorderLayout.NORTH);
        left.add(new JScrollPane(providerList), BorderLayout.CENTER);
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Add");
        add.addActionListener(e -> addProvider());
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> removeProvider());
        listButtons.add(add);
        listButtons.add(remove);
        left.add(listButtons, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Connection", connectionPanel());
        tabs.addTab("Prompts", promptPanel());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, tabs);
        split.setDividerLocation(235);
        split.setResizeWeight(0);
        add(split, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JButton save = new JButton("Save");
        save.addActionListener(e -> saveAndClose());
        actions.add(cancel);
        actions.add(save);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel connectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        int row = 0;
        row = addRow(panel, c, row, "Name", name);
        row = addRow(panel, c, row, "Base URL", baseUrl);
        row = addRow(panel, c, row, "Model", model);
        row = addRow(panel, c, row, "Authentication header", authHeader);
        row = addRow(panel, c, row, "Authentication prefix", authPrefix);
        row = addRow(panel, c, row, "API key", apiKey);
        c.gridx = 1; c.gridy = row++; panel.add(temporaryKey, c);
        row = addRow(panel, c, row, "Temperature", temperature);
        row = addRow(panel, c, row, "Timeout (seconds)", timeout);
        JLabel hint = new JLabel("<html>Examples: OpenAI: https://api.openai.com/v1<br>"
                + "Ollama: http://localhost:11434/v1 &nbsp; LM Studio: http://localhost:1234/v1</html>");
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weighty = 1; c.anchor = GridBagConstraints.NORTHWEST;
        panel.add(hint, c);
        return panel;
    }

    private JPanel promptPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        systemPrompt.setLineWrap(true);
        systemPrompt.setWrapStyleWord(true);
        userPrompt.setLineWrap(true);
        userPrompt.setWrapStyleWord(true);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("System prompt"), c);
        c.gridy = 1; c.fill = GridBagConstraints.BOTH; c.weightx = 1; c.weighty = 0.45;
        panel.add(new JScrollPane(systemPrompt), c);
        c.gridy = 2; c.fill = GridBagConstraints.HORIZONTAL; c.weighty = 0; c.insets = new Insets(10, 0, 0, 0);
        panel.add(new JLabel("User prompt"), c);
        c.gridy = 3; c.fill = GridBagConstraints.BOTH; c.weighty = 0.45; c.insets = new Insets(0, 0, 0, 0);
        panel.add(new JScrollPane(userPrompt), c);
        c.gridy = 4; c.fill = GridBagConstraints.HORIZONTAL; c.weighty = 0;
        panel.add(new JLabel("Variables: {{sourceLanguage}}, {{targetLanguage}}, {{text}} (required), {{glossary}}"), c);
        return panel;
    }

    private int addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(field, c);
        return row + 1;
    }

    private void switchSelection(ProviderConfig next) {
        if (selected != null) readForm(selected);
        selected = next;
        if (next != null) writeForm(next);
    }

    private void writeForm(ProviderConfig p) {
        name.setText(p.getName());
        baseUrl.setText(p.getBaseUrl());
        model.setText(p.getModel());
        authHeader.setText(p.getAuthHeader());
        authPrefix.setText(p.getAuthPrefix());
        apiKey.setText(translator.getApiKey(p.getId()));
        temporaryKey.setSelected(translator.isApiKeyTemporary(p.getId()));
        temperature.setValue(p.getTemperature());
        timeout.setValue(p.getTimeoutSeconds());
        systemPrompt.setText(p.getSystemPrompt());
        userPrompt.setText(p.getUserPrompt());
    }

    private void readForm(ProviderConfig p) {
        p.setName(name.getText());
        p.setBaseUrl(baseUrl.getText());
        p.setModel(model.getText());
        p.setAuthHeader(authHeader.getText());
        p.setAuthPrefix(authPrefix.getText());
        p.setTemperature(((Number) temperature.getValue()).doubleValue());
        p.setTimeoutSeconds(((Number) timeout.getValue()).intValue());
        p.setSystemPrompt(systemPrompt.getText());
        p.setUserPrompt(userPrompt.getText());
    }

    private void addProvider() {
        if (selected != null) readForm(selected);
        ProviderConfig provider = new ProviderConfig();
        provider.setName("New provider");
        providers.add(provider);
        listModel.addElement(provider);
        providerList.setSelectedIndex(providers.size() - 1);
    }

    private void removeProvider() {
        int index = providerList.getSelectedIndex();
        if (index < 0 || providers.size() == 1) {
            JOptionPane.showMessageDialog(this, "At least one provider is required.");
            return;
        }
        selected = null;
        providers.remove(index);
        listModel.remove(index);
        providerList.setSelectedIndex(Math.min(index, providers.size() - 1));
    }

    private void saveAndClose() {
        try {
            if (selected != null) readForm(selected);
            for (ProviderConfig provider : providers) provider.validate();
            ProviderConfig active = providerList.getSelectedValue();
            if (active == null) throw new IllegalArgumentException("Select an active provider");
            // Keys are committed only when the dialog is saved.
            for (ProviderConfig provider : providers) {
                if (provider == active) {
                    translator.setApiKey(provider.getId(), new String(apiKey.getPassword()),
                            temporaryKey.isSelected());
                }
            }
            repository.save(providers, active.getId());
            dispose();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Invalid provider",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
