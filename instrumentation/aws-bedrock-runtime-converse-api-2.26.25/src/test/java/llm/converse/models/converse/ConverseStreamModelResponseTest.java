/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package llm.converse.models.converse;

import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlockDelta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Direct unit tests of ConverseStreamModelResponse's reasoning content accumulation, independent of the full
 * instrumentation test harness.
 */
public class ConverseStreamModelResponseTest {

    @Test
    public void testStreamWithReasoningAndText() {
        ConverseStreamModelResponse response = new ConverseStreamModelResponse();

        response.apply(MessageStartEvent.builder().role("assistant").build());
        response.apply(reasoningTextDelta(0, "Thinking "));
        response.apply(reasoningTextDelta(0, "further..."));
        response.apply(reasoningSignatureDelta(0, "sig-123"));
        response.apply(textDelta(1, "The answer is blue."));
        response.apply(MessageStopEvent.builder().stopReason("end_turn").build());

        assertEquals(2, response.getNumberOfResponseMessages());

        assertTrue(response.isReasoningMessage(0));
        assertEquals("Thinking further...", response.getResponseReasoningContent(0));
        assertEquals("sig-123", response.getResponseReasoningSignature(0));
        assertFalse(response.isResponseReasoningRedacted(0));
        assertEquals("", response.getResponseMessage(0));

        assertFalse(response.isReasoningMessage(1));
        assertEquals("The answer is blue.", response.getResponseMessage(1));
        assertNull(response.getResponseReasoningContent(1));
    }

    @Test
    public void testStreamWithRedactedReasoning() {
        ConverseStreamModelResponse response = new ConverseStreamModelResponse();

        response.apply(MessageStartEvent.builder().role("assistant").build());
        response.apply(redactedReasoningDelta(0));
        response.apply(textDelta(1, "Final answer."));
        response.apply(MessageStopEvent.builder().stopReason("end_turn").build());

        assertEquals(2, response.getNumberOfResponseMessages());
        assertTrue(response.isReasoningMessage(0));
        assertTrue(response.isResponseReasoningRedacted(0));
        assertEquals("", response.getResponseReasoningContent(0));
        assertNull(response.getResponseReasoningSignature(0));
    }

    @Test
    public void testStreamWithNoReasoning() {
        ConverseStreamModelResponse response = new ConverseStreamModelResponse();

        response.apply(MessageStartEvent.builder().role("assistant").build());
        response.apply(textDelta(0, "Just a plain answer."));
        response.apply(MessageStopEvent.builder().stopReason("end_turn").build());

        assertEquals(1, response.getNumberOfResponseMessages());
        assertFalse(response.isReasoningMessage(0));
        assertEquals("Just a plain answer.", response.getResponseMessage(0));
    }

    private static ContentBlockDeltaEvent textDelta(int index, String text) {
        return ContentBlockDeltaEvent.builder()
                .contentBlockIndex(index)
                .delta(ContentBlockDelta.builder().text(text).build())
                .build();
    }

    private static ContentBlockDeltaEvent reasoningTextDelta(int index, String text) {
        return ContentBlockDeltaEvent.builder()
                .contentBlockIndex(index)
                .delta(ContentBlockDelta.builder().reasoningContent(ReasoningContentBlockDelta.builder().text(text).build()).build())
                .build();
    }

    private static ContentBlockDeltaEvent reasoningSignatureDelta(int index, String signature) {
        return ContentBlockDeltaEvent.builder()
                .contentBlockIndex(index)
                .delta(ContentBlockDelta.builder().reasoningContent(ReasoningContentBlockDelta.builder().signature(signature).build()).build())
                .build();
    }

    private static ContentBlockDeltaEvent redactedReasoningDelta(int index) {
        return ContentBlockDeltaEvent.builder()
                .contentBlockIndex(index)
                .delta(ContentBlockDelta.builder()
                        .reasoningContent(ReasoningContentBlockDelta.builder()
                                .redactedContent(SdkBytes.fromUtf8String("encrypted-reasoning-blob"))
                                .build())
                        .build())
                .build();
    }
}
