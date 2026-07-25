package io.github.omegat.ai;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/** Non-secret configuration for one OpenAI-compatible endpoint. */
public final class ProviderConfig {
    public static final String DEFAULT_SYSTEM_PROMPT =
            "You are a professional translator. Preserve meaning, tone, formatting, "
                    + "placeholders, and OmegaT tags. Return only the translation.";
    public static final String DEFAULT_USER_PROMPT =
            "Translate from {{sourceLanguage}} to {{targetLanguage}}:\n\n{{text}}";

    private String id = UUID.randomUUID().toString();
    private String name = "OpenAI";
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4.1-mini";
    private String authHeader = "Authorization";
    private String authPrefix = "Bearer ";
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;
    private String userPrompt = DEFAULT_USER_PROMPT;
    private double temperature = 0.1;
    private int timeoutSeconds = 60;

    public static ProviderConfig localLlama() {
        ProviderConfig value = new ProviderConfig();
        value.name = "Local Llama";
        value.baseUrl = "http://localhost:11434/v1";
        value.model = "llama3.2";
        return value;
    }

    Properties toProperties(String prefix) {
        Properties p = new Properties();
        p.setProperty(prefix + "id", id);
        p.setProperty(prefix + "name", name);
        p.setProperty(prefix + "baseUrl", baseUrl);
        p.setProperty(prefix + "model", model);
        p.setProperty(prefix + "authHeader", authHeader);
        p.setProperty(prefix + "authPrefix", authPrefix);
        p.setProperty(prefix + "systemPrompt", systemPrompt);
        p.setProperty(prefix + "userPrompt", userPrompt);
        p.setProperty(prefix + "temperature", Double.toString(temperature));
        p.setProperty(prefix + "timeoutSeconds", Integer.toString(timeoutSeconds));
        return p;
    }

    static ProviderConfig fromProperties(Properties p, String prefix) {
        ProviderConfig value = new ProviderConfig();
        value.id = p.getProperty(prefix + "id", value.id);
        value.name = p.getProperty(prefix + "name", value.name);
        value.baseUrl = p.getProperty(prefix + "baseUrl", value.baseUrl);
        value.model = p.getProperty(prefix + "model", value.model);
        value.authHeader = p.getProperty(prefix + "authHeader", value.authHeader);
        value.authPrefix = p.getProperty(prefix + "authPrefix", value.authPrefix);
        value.systemPrompt = p.getProperty(prefix + "systemPrompt", value.systemPrompt);
        value.userPrompt = p.getProperty(prefix + "userPrompt", value.userPrompt);
        value.temperature = parseDouble(p.getProperty(prefix + "temperature"), value.temperature);
        value.timeoutSeconds = parseInt(p.getProperty(prefix + "timeoutSeconds"), value.timeoutSeconds);
        return value;
    }

    private static int parseInt(String input, int fallback) {
        try {
            return Integer.parseInt(input);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String input, double fallback) {
        try {
            return Double.parseDouble(input);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public ProviderConfig copy() {
        ProviderConfig value = new ProviderConfig();
        value.id = id;
        value.name = name;
        value.baseUrl = baseUrl;
        value.model = model;
        value.authHeader = authHeader;
        value.authPrefix = authPrefix;
        value.systemPrompt = systemPrompt;
        value.userPrompt = userPrompt;
        value.temperature = temperature;
        value.timeoutSeconds = timeoutSeconds;
        return value;
    }

    public String chatCompletionsUrl() {
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        return trimmed.endsWith("/chat/completions") ? trimmed : trimmed + "/chat/completions";
    }

    public void validate() {
        Objects.requireNonNull(name);
        if (name.isBlank()) throw new IllegalArgumentException("Provider name is required");
        if (!(baseUrl.startsWith("https://") || baseUrl.startsWith("http://"))) {
            throw new IllegalArgumentException("Base URL must start with http:// or https://");
        }
        if (model.isBlank()) throw new IllegalArgumentException("Model is required");
        if (!userPrompt.contains("{{text}}")) {
            throw new IllegalArgumentException("User prompt must contain {{text}}");
        }
        if (timeoutSeconds < 1) throw new IllegalArgumentException("Timeout must be positive");
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("Temperature must be between 0 and 2");
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value.trim(); }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value.trim(); }
    public String getModel() { return model; }
    public void setModel(String value) { model = value.trim(); }
    public String getAuthHeader() { return authHeader; }
    public void setAuthHeader(String value) { authHeader = value.trim(); }
    public String getAuthPrefix() { return authPrefix; }
    public void setAuthPrefix(String value) { authPrefix = value; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String value) { systemPrompt = value; }
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String value) { userPrompt = value; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double value) { temperature = value; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int value) { timeoutSeconds = value; }

    @Override
    public String toString() {
        return name;
    }
}

