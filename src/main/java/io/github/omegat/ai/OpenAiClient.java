package io.github.omegat.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class OpenAiClient {
    private final HttpClient client;

    OpenAiClient() {
        client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    String translate(ProviderConfig provider, String apiKey, String sourceLanguage,
            String targetLanguage, String sourceText) throws IOException, InterruptedException {
        String system = expand(provider.getSystemPrompt(), sourceLanguage, targetLanguage, sourceText);
        String user = expand(provider.getUserPrompt(), sourceLanguage, targetLanguage, sourceText);
        String body = "{"
                + "\"model\":" + Json.quote(provider.getModel()) + ","
                + "\"temperature\":" + provider.getTemperature() + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + Json.quote(system) + "},"
                + "{\"role\":\"user\",\"content\":" + Json.quote(user) + "}"
                + "]}";

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(provider.chatCompletionsUrl()))
                .timeout(Duration.ofSeconds(provider.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (!apiKey.isBlank() && !provider.getAuthHeader().isBlank()) {
            request.header(provider.getAuthHeader(), provider.getAuthPrefix() + apiKey);
        }
        HttpResponse<String> response =
                client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String message = Json.firstStringValue(response.body(), "message");
            if (message == null) message = abbreviate(response.body(), 500);
            throw new IOException("Provider returned HTTP " + response.statusCode() + ": " + message);
        }
        String content = Json.firstStringValue(response.body(), "content");
        if (content == null) {
            throw new IOException("Provider response did not contain choices[0].message.content");
        }
        return content.trim();
    }

    static String expand(String template, String sourceLanguage, String targetLanguage, String text) {
        return template.replace("{{sourceLanguage}}", sourceLanguage)
                .replace("{{targetLanguage}}", targetLanguage)
                .replace("{{text}}", text);
    }

    private static String abbreviate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}

