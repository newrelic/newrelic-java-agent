/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.converse.models.converse;

import com.newrelic.api.agent.NewRelic;
import llm.converse.models.ModelResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockStopEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamMetadataEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.util.Optional;
import java.util.logging.Level;

import static llm.converse.models.ModelInvocation.getRandomGuid;

public class ConverseStreamModelResponse implements ModelResponse, ConverseStreamResponseHandler.Visitor {
    private String amznRequestId = "";
    private String responseOrganization = "";

    private String operationType = COMPLETION;

    private boolean isSuccessfulResponse = false;
    private int statusCode = 0;
    private String statusText = "";

    private boolean streamErrorOccurred = false;

    private final String llmChatCompletionSummaryId = getRandomGuid();
    private String stopReason = "";
    private String role = "";
    private final StringBuilder content = new StringBuilder();
    private final StringBuilder reasoning = new StringBuilder();
    private String reasoningSignature = null;
    private boolean reasoningRedacted = false;
    private Integer inputTokens = 0;
    private Integer outputTokens = 0;
    private Integer totalTokens = 0;

    /**
     * Apply the initial HTTP response info delivered via {@code responseReceived}.
     * Mirrors what {@link ConverseModelResponse} extracts from a non-streaming ConverseResponse.
     *
     * @param converseStreamResponse the initial response delivered before any stream events
     */
    public void applyHttpResponse(ConverseStreamResponse converseStreamResponse) {
        if (converseStreamResponse != null) {
            SdkHttpResponse sdkHttpResponse = converseStreamResponse.sdkHttpResponse();
            if (sdkHttpResponse != null) {
                isSuccessfulResponse = sdkHttpResponse.isSuccessful();
                statusCode = sdkHttpResponse.statusCode();
                Optional<String> statusTextOptional = sdkHttpResponse.statusText();
                statusTextOptional.ifPresent(s -> statusText = s);
            }
            if (converseStreamResponse.responseMetadata() != null) {
                amznRequestId = converseStreamResponse.responseMetadata().requestId();
            }
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null ConverseStreamResponse");
        }
    }

    public void markStreamError() {
        streamErrorOccurred = true;
    }

    /**
     * Dispatches a single streamed event to the matching visit method above, based on the
     * event's runtime type rather than {@code output.accept(Visitor)} or {@code sdkEventType()}.
     * The SDK's generated event classes throw UnsupportedOperationException from accept(), and
     * sdkEventType() is not populated on these event instances, so dispatch is done here instead.
     *
     * @param output a single event delivered via the event publisher
     */
    public void apply(ConverseStreamOutput output) {
        if (output instanceof MessageStartEvent) {
            visitMessageStart((MessageStartEvent) output);
        } else if (output instanceof ContentBlockStartEvent) {
            visitContentBlockStart((ContentBlockStartEvent) output);
        } else if (output instanceof ContentBlockDeltaEvent) {
            visitContentBlockDelta((ContentBlockDeltaEvent) output);
        } else if (output instanceof ContentBlockStopEvent) {
            visitContentBlockStop((ContentBlockStopEvent) output);
        } else if (output instanceof MessageStopEvent) {
            visitMessageStop((MessageStopEvent) output);
        } else if (output instanceof ConverseStreamMetadataEvent) {
            visitMetadata((ConverseStreamMetadataEvent) output);
        } else {
            visitDefault(output);
        }
    }

    @Override
    public void visitDefault(ConverseStreamOutput event) {
        // No-op, unrecognized event types are ignored
    }

    @Override
    public void visitMessageStart(MessageStartEvent event) {
        role = event.roleAsString();
    }

    @Override
    public void visitContentBlockStart(ContentBlockStartEvent event) {
        // No-op, only text deltas are accumulated
    }

    @Override
    public void visitContentBlockDelta(ContentBlockDeltaEvent event) {
        ContentBlockDelta delta = event.delta();
        if (delta == null) {
            return;
        }
        if (delta.text() != null) {
            content.append(delta.text());
            return;
        }
        ReasoningContentSupport.ReasoningData reasoningData = ReasoningContentSupport.fromContentBlockDelta(delta);
        if (reasoningData != null) {
            if (reasoningData.getText() != null) {
                reasoning.append(reasoningData.getText());
            }
            if (reasoningData.getSignature() != null) {
                reasoningSignature = reasoningData.getSignature();
            }
            if (reasoningData.isRedacted()) {
                reasoningRedacted = true;
            }
        }
    }

    @Override
    public void visitContentBlockStop(ContentBlockStopEvent event) {
        // No-op, only text deltas are accumulated
    }

    @Override
    public void visitMessageStop(MessageStopEvent event) {
        stopReason = event.stopReasonAsString();
    }

    @Override
    public void visitMetadata(ConverseStreamMetadataEvent event) {
        TokenUsage usage = event.usage();
        if (usage != null) {
            inputTokens = usage.inputTokens();
            outputTokens = usage.outputTokens();
            totalTokens = usage.totalTokens();
        }
    }

    @Override
    public String getResponseMessage(int index) {
        if (isReasoningMessage(index)) {
            return "";
        }
        return content.toString();
    }

    @Override
    public int getNumberOfResponseMessages() {
        return hasReasoning() ? 2 : 1;
    }

    /**
     * Whether any reasoning content was observed on the stream. When true, the response is modeled as two
     * messages: index 0 is the reasoning message, index 1 is the text message.
     */
    private boolean hasReasoning() {
        return reasoning.length() > 0 || reasoningRedacted;
    }

    @Override
    public String getStopReason() {
        return stopReason;
    }

    @Override
    public String getAmznRequestId() {
        return amznRequestId;
    }

    @Override
    public String getOperationType() {
        return operationType;
    }

    @Override
    public String getLlmChatCompletionSummaryId() {
        return llmChatCompletionSummaryId;
    }

    @Override
    public boolean isErrorResponse() {
        return !isSuccessfulResponse || streamErrorOccurred;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getStatusText() {
        return statusText;
    }

    @Override
    public boolean isUser() {
        return role.equalsIgnoreCase("user");
    }

    @Override
    public String getResponseOrganization() {
        return responseOrganization;
    }

    @Override
    public Integer getResponseUsagePromptTokens() {
        return inputTokens;
    }

    @Override
    public Integer getResponseUsageCompletionTokens() {
        return outputTokens;
    }

    @Override
    public Integer getResponseUsageTotalTokens() {
        return totalTokens;
    }

    @Override
    public boolean isReasoningMessage(int index) {
        return hasReasoning() && index == 0;
    }

    @Override
    public String getResponseReasoningContent(int index) {
        return isReasoningMessage(index) ? reasoning.toString() : null;
    }

    @Override
    public String getResponseReasoningSignature(int index) {
        return isReasoningMessage(index) ? reasoningSignature : null;
    }

    @Override
    public boolean isResponseReasoningRedacted(int index) {
        return isReasoningMessage(index) && reasoningRedacted;
    }
}
