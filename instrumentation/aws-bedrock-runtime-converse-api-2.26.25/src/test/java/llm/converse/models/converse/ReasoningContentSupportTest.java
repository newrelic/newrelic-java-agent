/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package llm.converse.models.converse;

import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningTextBlock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests ReasoningContentSupport's reflective lookups against the bumped test-scope SDK version (2.30.27, the
 * exact version reasoningContent was introduced in). The "SDK predates 2.30.27" no-op path is exercised by the
 * try/catch in the static initializer but isn't independently testable without a second, older SDK jar on the
 * test classpath.
 */
public class ReasoningContentSupportTest {

    @Test
    public void testFromContentBlockWithReasoningText() {
        ReasoningTextBlock reasoningTextBlock = ReasoningTextBlock.builder().text("because reasons").signature("sig-abc").build();
        ContentBlock block = ContentBlock.builder()
                .reasoningContent(ReasoningContentBlock.builder().reasoningText(reasoningTextBlock).build())
                .build();

        ReasoningContentSupport.ReasoningData data = ReasoningContentSupport.fromContentBlock(block);

        assertTrue(ReasoningContentSupport.isReasoningBlock(block));
        assertEquals("because reasons", data.getText());
        assertEquals("sig-abc", data.getSignature());
        assertFalse(data.isRedacted());
    }

    @Test
    public void testFromContentBlockWithRedactedContent() {
        ContentBlock block = ContentBlock.builder()
                .reasoningContent(ReasoningContentBlock.builder().redactedContent(SdkBytes.fromUtf8String("blob")).build())
                .build();

        ReasoningContentSupport.ReasoningData data = ReasoningContentSupport.fromContentBlock(block);

        assertTrue(data.isRedacted());
        assertNull(data.getText());
        assertNull(data.getSignature());
    }

    @Test
    public void testFromContentBlockWithPlainText() {
        ContentBlock block = ContentBlock.builder().text("just text").build();

        assertFalse(ReasoningContentSupport.isReasoningBlock(block));
        assertNull(ReasoningContentSupport.fromContentBlock(block));
    }

    @Test
    public void testFromContentBlockWithNull() {
        assertNull(ReasoningContentSupport.fromContentBlock(null));
        assertFalse(ReasoningContentSupport.isReasoningBlock(null));
    }

    @Test
    public void testFromContentBlockDeltaWithText() {
        ContentBlockDelta delta = ContentBlockDelta.builder()
                .reasoningContent(ReasoningContentBlockDelta.builder().text("partial thought").build())
                .build();

        ReasoningContentSupport.ReasoningData data = ReasoningContentSupport.fromContentBlockDelta(delta);

        assertEquals("partial thought", data.getText());
        assertNull(data.getSignature());
        assertFalse(data.isRedacted());
    }

    @Test
    public void testFromContentBlockDeltaWithSignature() {
        ContentBlockDelta delta = ContentBlockDelta.builder()
                .reasoningContent(ReasoningContentBlockDelta.builder().signature("sig-xyz").build())
                .build();

        ReasoningContentSupport.ReasoningData data = ReasoningContentSupport.fromContentBlockDelta(delta);

        assertEquals("sig-xyz", data.getSignature());
        assertNull(data.getText());
    }

    @Test
    public void testFromContentBlockDeltaWithRedactedContent() {
        ContentBlockDelta delta = ContentBlockDelta.builder()
                .reasoningContent(ReasoningContentBlockDelta.builder().redactedContent(SdkBytes.fromUtf8String("blob")).build())
                .build();

        ReasoningContentSupport.ReasoningData data = ReasoningContentSupport.fromContentBlockDelta(delta);

        assertTrue(data.isRedacted());
    }

    @Test
    public void testFromContentBlockDeltaWithPlainText() {
        ContentBlockDelta delta = ContentBlockDelta.builder().text("plain delta text").build();

        assertNull(ReasoningContentSupport.fromContentBlockDelta(delta));
    }

    @Test
    public void testFromContentBlockDeltaWithNull() {
        assertNull(ReasoningContentSupport.fromContentBlockDelta(null));
    }
}
