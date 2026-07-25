package io.github.omegat.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class OpenAiClientTest {
    @Test
    void expandsPromptVariablesWithoutInterpretingReplacementCharacters() {
        assertEquals("English > Chinese: price $5\\unit",
                OpenAiClient.expand("{{sourceLanguage}} > {{targetLanguage}}: {{text}}",
                        "English", "Chinese", "price $5\\unit"));
    }
}

