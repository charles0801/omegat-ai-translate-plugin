package io.github.omegat.ai;

import java.awt.Window;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import org.omegat.core.Core;
import org.omegat.core.machinetranslators.BaseTranslate;
import org.omegat.util.Language;

public final class AiTranslate extends BaseTranslate {
    private static final String ENABLED_PREFERENCE = "ai_translate_enabled";
    private static final String CREDENTIAL_PREFIX = "omegat.ai.provider.";
    private static final ProviderRepository REPOSITORY = new ProviderRepository();

    private final OpenAiClient client = new OpenAiClient();

    public AiTranslate() {
        super();
        if (Core.getMainWindow() != null) {
            JMenu menu = Core.getMainWindow().getMainMenu().getMachineTranslationMenu();
            JMenuItem refresh = new JMenuItem("Request machine translation again");
            int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
                    | InputEvent.ALT_DOWN_MASK;
            refresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, modifiers));
            refresh.addActionListener(event -> {
                if (Core.getProject() != null && Core.getProject().isProjectLoaded()
                        && Core.getMachineTranslatePane() != null) {
                    Core.getMachineTranslatePane().forceLoad();
                }
            });
            menu.addSeparator();
            menu.add(refresh);
            JMenuItem translateDocument = new JMenuItem("Translate current document with AI...");
            translateDocument.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, modifiers));
            translateDocument.addActionListener(event -> DocumentBatchTranslator.start(this, client,
                    findActive(REPOSITORY.load())));
            menu.add(translateDocument);
        }
    }

    @Override
    public String getName() {
        List<ProviderConfig> providers = REPOSITORY.load();
        ProviderConfig active = findActive(providers);
        return "AI Translate" + (active == null ? "" : " (" + active.getName() + ")");
    }

    @Override
    protected String getPreferenceName() {
        return ENABLED_PREFERENCE;
    }

    @Override
    protected String translate(Language sourceLanguage, Language targetLanguage, String text)
            throws Exception {
        List<ProviderConfig> providers = REPOSITORY.load();
        ProviderConfig provider = findActive(providers);
        if (provider == null) {
            throw new IllegalStateException("No AI translation provider is configured");
        }
        provider.validate();
        Map<String, String> glossary = glossarySupplier == null
                ? Collections.emptyMap() : glossarySupplier.get();
        return client.translate(provider, getApiKey(provider.getId()),
                sourceLanguage.toString(), targetLanguage.toString(), text, glossary);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public void showConfigurationUI(Window parent) {
        ProviderDialog.show(parent, this, REPOSITORY);
    }

    String getApiKey(String providerId) {
        return getCredential(CREDENTIAL_PREFIX + providerId);
    }

    void setApiKey(String providerId, String apiKey, boolean temporary) {
        setCredential(CREDENTIAL_PREFIX + providerId, apiKey, temporary);
    }

    boolean isApiKeyTemporary(String providerId) {
        return isCredentialStoredTemporarily(CREDENTIAL_PREFIX + providerId);
    }

    private ProviderConfig findActive(List<ProviderConfig> providers) {
        String id = REPOSITORY.activeId(providers);
        return providers.stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElse(providers.isEmpty() ? null : providers.get(0));
    }
}
