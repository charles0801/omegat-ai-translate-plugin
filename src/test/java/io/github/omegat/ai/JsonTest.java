package io.github.omegat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class JsonTest {
    @Test
    void quotesControlCharacters() {
        assertEquals("\"a\\n\\\"b\\\\c\"", Json.quote("a\n\"b\\c"));
    }

    @Test
    void readsChatCompletionContent() {
        String json = "{\"choices\":[{\"message\":{\"role\":\"assistant\","
                + "\"content\":\"Bonjour\\nmonde\"}}]}";
        assertEquals("Bonjour\nmonde", Json.firstStringValue(json, "content"));
    }

    @Test
    void returnsNullForMissingValue() {
        assertNull(Json.firstStringValue("{\"id\":\"one\"}", "content"));
    }
}

