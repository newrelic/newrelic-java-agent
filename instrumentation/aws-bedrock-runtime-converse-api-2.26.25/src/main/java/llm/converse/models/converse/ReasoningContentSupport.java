/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models.converse;

import com.newrelic.api.agent.NewRelic;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Reflective accessor for the Bedrock Converse API's reasoningContent support.
 * <p>
 * {@code ContentBlock.reasoningContent()} and {@code ContentBlockDelta.reasoningContent()}, along with the
 * {@code ReasoningContentBlock}, {@code ReasoningTextBlock}, and {@code ReasoningContentBlockDelta} types they return,
 * were all added together in bedrockruntime SDK 2.30.27. This module compiles against and supports SDK 2.26.25 and
 * later, so none of those types can be referenced at compile time. All lookups happen once via reflection and every
 * accessor below no-ops (returns null) when the app's SDK predates 2.30.27.
 */
public class ReasoningContentSupport {
    private static final String REASONING_CONTENT_BLOCK_CLASS = "software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlock";
    private static final String REASONING_TEXT_BLOCK_CLASS = "software.amazon.awssdk.services.bedrockruntime.model.ReasoningTextBlock";
    private static final String REASONING_CONTENT_BLOCK_DELTA_CLASS =
            "software.amazon.awssdk.services.bedrockruntime.model.ReasoningContentBlockDelta";

    private static final Method CONTENT_BLOCK_REASONING_CONTENT;
    private static final Method REASONING_CONTENT_BLOCK_REASONING_TEXT;
    private static final Method REASONING_CONTENT_BLOCK_REDACTED_CONTENT;
    private static final Method REASONING_TEXT_BLOCK_TEXT;
    private static final Method REASONING_TEXT_BLOCK_SIGNATURE;

    private static final Method CONTENT_BLOCK_DELTA_REASONING_CONTENT;
    private static final Method REASONING_CONTENT_BLOCK_DELTA_TEXT;
    private static final Method REASONING_CONTENT_BLOCK_DELTA_SIGNATURE;
    private static final Method REASONING_CONTENT_BLOCK_DELTA_REDACTED_CONTENT;

    static {
        Method contentBlockReasoningContent = null;
        Method reasoningContentBlockReasoningText = null;
        Method reasoningContentBlockRedactedContent = null;
        Method reasoningTextBlockText = null;
        Method reasoningTextBlockSignature = null;
        Method contentBlockDeltaReasoningContent = null;
        Method reasoningContentBlockDeltaText = null;
        Method reasoningContentBlockDeltaSignature = null;
        Method reasoningContentBlockDeltaRedactedContent = null;

        try {
            contentBlockReasoningContent = ContentBlock.class.getMethod("reasoningContent");

            Class<?> reasoningContentBlockClass = Class.forName(REASONING_CONTENT_BLOCK_CLASS);
            reasoningContentBlockReasoningText = reasoningContentBlockClass.getMethod("reasoningText");
            reasoningContentBlockRedactedContent = reasoningContentBlockClass.getMethod("redactedContent");

            Class<?> reasoningTextBlockClass = Class.forName(REASONING_TEXT_BLOCK_CLASS);
            reasoningTextBlockText = reasoningTextBlockClass.getMethod("text");
            reasoningTextBlockSignature = reasoningTextBlockClass.getMethod("signature");

            contentBlockDeltaReasoningContent = ContentBlockDelta.class.getMethod("reasoningContent");

            Class<?> reasoningContentBlockDeltaClass = Class.forName(REASONING_CONTENT_BLOCK_DELTA_CLASS);
            reasoningContentBlockDeltaText = reasoningContentBlockDeltaClass.getMethod("text");
            reasoningContentBlockDeltaSignature = reasoningContentBlockDeltaClass.getMethod("signature");
            reasoningContentBlockDeltaRedactedContent = reasoningContentBlockDeltaClass.getMethod("redactedContent");
        } catch (Exception e) {
            // Bedrock's ContentBlock/ContentBlockDelta reasoningContent() was added in bedrockruntime SDK 2.30.27.
            // On an older SDK these lookups fail and every accessor below no-ops instead of throwing.
        }

        CONTENT_BLOCK_REASONING_CONTENT = contentBlockReasoningContent;
        REASONING_CONTENT_BLOCK_REASONING_TEXT = reasoningContentBlockReasoningText;
        REASONING_CONTENT_BLOCK_REDACTED_CONTENT = reasoningContentBlockRedactedContent;
        REASONING_TEXT_BLOCK_TEXT = reasoningTextBlockText;
        REASONING_TEXT_BLOCK_SIGNATURE = reasoningTextBlockSignature;
        CONTENT_BLOCK_DELTA_REASONING_CONTENT = contentBlockDeltaReasoningContent;
        REASONING_CONTENT_BLOCK_DELTA_TEXT = reasoningContentBlockDeltaText;
        REASONING_CONTENT_BLOCK_DELTA_SIGNATURE = reasoningContentBlockDeltaSignature;
        REASONING_CONTENT_BLOCK_DELTA_REDACTED_CONTENT = reasoningContentBlockDeltaRedactedContent;
    }

    private ReasoningContentSupport() {
    }

    /**
     * Immutable holder for reasoning content extracted from either a single ContentBlock (non-streaming) or
     * accumulated from a series of ContentBlockDelta events (streaming).
     */
    public static class ReasoningData {
        private final String text;
        private final String signature;
        private final boolean redacted;

        public ReasoningData(String text, String signature, boolean redacted) {
            this.text = text;
            this.signature = signature;
            this.redacted = redacted;
        }

        public String getText() {
            return text;
        }

        public String getSignature() {
            return signature;
        }

        public boolean isRedacted() {
            return redacted;
        }
    }

    /**
     * Determine whether a ContentBlock is a reasoning/thinking block rather than text, tool-use, etc.
     *
     * @param block the ContentBlock to inspect
     * @return true if the block carries reasoning content
     */
    public static boolean isReasoningBlock(ContentBlock block) {
        return fromContentBlock(block) != null;
    }

    /**
     * Extract reasoning text/signature/redacted state from a single ContentBlock.
     *
     * @param block the ContentBlock to inspect
     * @return the extracted ReasoningData, or null if this SDK predates reasoningContent support or the block isn't
     * a reasoning block
     */
    public static ReasoningData fromContentBlock(ContentBlock block) {
        if (CONTENT_BLOCK_REASONING_CONTENT == null || block == null) {
            return null;
        }
        try {
            Object reasoningContentBlock = CONTENT_BLOCK_REASONING_CONTENT.invoke(block);
            if (reasoningContentBlock == null) {
                return null;
            }

            Object redactedContent = REASONING_CONTENT_BLOCK_REDACTED_CONTENT.invoke(reasoningContentBlock);
            Object reasoningTextBlock = REASONING_CONTENT_BLOCK_REASONING_TEXT.invoke(reasoningContentBlock);

            String text = null;
            String signature = null;
            if (reasoningTextBlock != null) {
                text = (String) REASONING_TEXT_BLOCK_TEXT.invoke(reasoningTextBlock);
                signature = (String) REASONING_TEXT_BLOCK_SIGNATURE.invoke(reasoningTextBlock);
            }
            return new ReasoningData(text, signature, redactedContent != null);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Unable to extract Bedrock Converse reasoningContent");
            return null;
        }
    }

    /**
     * Extract reasoning text/signature/redacted state from a single ContentBlockDelta.
     *
     * @param delta the ContentBlockDelta to inspect
     * @return the extracted ReasoningData, or null if this SDK predates reasoningContent support or the delta isn't
     * a reasoning delta
     */
    public static ReasoningData fromContentBlockDelta(ContentBlockDelta delta) {
        if (CONTENT_BLOCK_DELTA_REASONING_CONTENT == null || delta == null) {
            return null;
        }
        try {
            Object reasoningContentBlockDelta = CONTENT_BLOCK_DELTA_REASONING_CONTENT.invoke(delta);
            if (reasoningContentBlockDelta == null) {
                return null;
            }

            String text = (String) REASONING_CONTENT_BLOCK_DELTA_TEXT.invoke(reasoningContentBlockDelta);
            String signature = (String) REASONING_CONTENT_BLOCK_DELTA_SIGNATURE.invoke(reasoningContentBlockDelta);
            Object redactedContent = REASONING_CONTENT_BLOCK_DELTA_REDACTED_CONTENT.invoke(reasoningContentBlockDelta);

            return new ReasoningData(text, signature, redactedContent != null);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "AIM: Unable to extract Bedrock Converse reasoningContent delta");
            return null;
        }
    }
}
