package io.github.omegat.ai;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.omegat.core.data.EntryKey;
import org.omegat.core.data.ProtectedPart;
import org.omegat.core.data.SourceTextEntry;

class DocumentBatchTranslatorTest {
    @Test
    void acceptsReorderedTagsButRejectsMissingAndDuplicatedTags() {
        SourceTextEntry entry = entry("A <x0/> B <x1/>", "<x0/>", "<x1/>");
        assertTrue(DocumentBatchTranslator.tagsMatch(entry, "乙 <x1/> 甲 <x0/>"));
        assertFalse(DocumentBatchTranslator.tagsMatch(entry, "乙 <x0/>"));
        assertFalse(DocumentBatchTranslator.tagsMatch(entry, "乙 <x0/> <x0/> <x1/>"));
    }

    @Test
    void rejectsInventedOmegaTTag() {
        SourceTextEntry entry = entry("Plain text");
        assertFalse(DocumentBatchTranslator.tagsMatch(entry, "译文 <x0/>"));
    }

    private static SourceTextEntry entry(String source, String... tags) {
        List<ProtectedPart> parts = new java.util.ArrayList<>();
        for (String tag : tags) {
            ProtectedPart part = new ProtectedPart();
            part.setTextInSourceSegment(tag);
            parts.add(part);
        }
        EntryKey key = new EntryKey("test.txt", source, null, null, null, null);
        return new SourceTextEntry(key, 1, new String[0], null, parts);
    }
}
