package io.github.omegat.ai;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

final class ProviderRepository {
    private static final String CONFIG_KEY = "providers.v1";
    private static final String ACTIVE_KEY = "activeProvider";
    private final Preferences preferences = Preferences.userNodeForPackage(AiTranslate.class);

    List<ProviderConfig> load() {
        String encoded = preferences.get(CONFIG_KEY, "");
        if (encoded.isBlank()) {
            return new ArrayList<>(List.of(new ProviderConfig(), ProviderConfig.localLlama()));
        }
        try {
            Properties p = new Properties();
            byte[] bytes = Base64.getDecoder().decode(encoded);
            p.load(new ByteArrayInputStream(bytes));
            int count = Integer.parseInt(p.getProperty("count", "0"));
            List<ProviderConfig> result = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                result.add(ProviderConfig.fromProperties(p, "provider." + i + "."));
            }
            return result.isEmpty() ? new ArrayList<>(List.of(new ProviderConfig())) : result;
        } catch (RuntimeException | IOException e) {
            return new ArrayList<>(List.of(new ProviderConfig()));
        }
    }

    void save(List<ProviderConfig> providers, String activeId) {
        Properties p = new Properties();
        p.setProperty("count", Integer.toString(providers.size()));
        for (int i = 0; i < providers.size(); i++) {
            p.putAll(providers.get(i).toProperties("provider." + i + "."));
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            p.store(output, "OmegaT AI Translate providers");
            preferences.put(CONFIG_KEY, Base64.getEncoder().encodeToString(output.toByteArray()));
            preferences.put(ACTIVE_KEY, activeId);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    String activeId(List<ProviderConfig> providers) {
        String fallback = providers.get(0).getId();
        return preferences.get(ACTIVE_KEY, fallback);
    }
}

