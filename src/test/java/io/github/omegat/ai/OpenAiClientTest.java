package io.github.omegat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class OpenAiClientTest {
    @Test
    void expandsPromptVariablesWithoutInterpretingReplacementCharacters() {
        assertEquals("English > Chinese: price $5\\unit",
                OpenAiClient.expand("{{sourceLanguage}} > {{targetLanguage}}: {{text}}",
                        "English", "Chinese", "price $5\\unit"));
    }

    @Test
    void leavesPlaceholderTextInsideTheSegmentUntouched() {
        assertEquals("Literal {{glossary}}; terms: source → 译法",
                OpenAiClient.expand("{{text}}; terms: {{glossary}}", "English", "Chinese",
                        "Literal {{glossary}}", "source → 译法"));
    }

    @Test
    void sendsMatchedTermsWithExistingCustomPrompt() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"choices\":[{\"message\":{\"content\":\"运行时\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (java.io.OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        });
        server.start();
        try {
            ProviderConfig provider = new ProviderConfig();
            provider.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
            provider.setUserPrompt("Translate: {{text}}");
            String result = new OpenAiClient().translate(provider, "", "English", "Chinese",
                    "runtime", Map.of("runtime", "运行时"));
            assertEquals("运行时", result);
            assertTrue(requestBody.get().contains("Translate: runtime"));
            assertTrue(requestBody.get().contains("runtime → 运行时"));

            new OpenAiClient().translate(provider, "", "English", "Chinese", "ordinary text", Map.of());
            assertFalse(requestBody.get().contains("Terminology matched"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotForwardKeyOrSegmentAcrossRedirect() throws Exception {
        AtomicBoolean redirected = new AtomicBoolean();
        HttpServer destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        destination.createContext("/receive", exchange -> {
            redirected.set(true);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        destination.start();
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/v1/chat/completions", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:"
                    + destination.getAddress().getPort() + "/receive");
            exchange.sendResponseHeaders(307, -1);
            exchange.close();
        });
        provider.start();
        try {
            ProviderConfig config = new ProviderConfig();
            config.setBaseUrl("http://127.0.0.1:" + provider.getAddress().getPort() + "/v1");
            assertThrows(IOException.class, () -> new OpenAiClient().translate(config,
                    "dummy-review-key", "English", "Chinese", "dummy-review-segment", Map.of()));
            assertFalse(redirected.get());
        } finally {
            provider.stop(0);
            destination.stop(0);
        }
    }
}
