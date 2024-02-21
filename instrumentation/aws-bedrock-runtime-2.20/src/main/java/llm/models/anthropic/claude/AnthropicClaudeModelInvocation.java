package llm.models.anthropic.claude;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import llm.models.ModelInvocation;
import llm.models.ModelRequest;
import llm.models.ModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.COMPLETION;
import static llm.models.anthropic.claude.AnthropicClaudeInvokeModelResponse.EMBEDDING;

public class AnthropicClaudeModelInvocation implements ModelInvocation {
    ModelRequest claudeRequest;
    ModelResponse claudeResponse;

    public AnthropicClaudeModelInvocation(InvokeModelRequest invokeModelRequest, InvokeModelResponse invokeModelResponse) {
        claudeRequest = new AnthropicClaudeInvokeModelRequest(invokeModelRequest);
        claudeResponse = new AnthropicClaudeInvokeModelResponse(invokeModelResponse);
    }

    @Override
    public void setLlmOperationMetricName(Transaction txn, String functionName) {
        txn.getTracedMethod().setMetricName("Llm", claudeResponse.getOperationType(), BEDROCK, functionName);
    }

    // TODO add event builders???
    @Override
    public void recordLlmEmbeddingEvent(long startTime, Map<String, String> linkingMetadata) {
        Map<String, Object> eventAttributes = new HashMap<>();
        // Generic attributes that are constant for all Bedrock Models
        addSpanId(eventAttributes, linkingMetadata);
        addTraceId(eventAttributes, linkingMetadata);
        addVendor(eventAttributes);
        addIngestSource(eventAttributes);

        // Attributes dependent on the request/response
        addId(eventAttributes, claudeResponse.getLlmEmbeddingId());
        addRequestId(eventAttributes, claudeResponse);
        addInput(eventAttributes, claudeRequest);
        addRequestModel(eventAttributes, claudeRequest);
        addResponseModel(eventAttributes, claudeRequest);
        addResponseUsageTotalTokens(eventAttributes, claudeResponse);
        addResponseUsagePromptTokens(eventAttributes, claudeResponse);

        // Error attributes
        if (claudeResponse.isErrorResponse()) {
            addError(eventAttributes);
            claudeResponse.reportLlmError();
        }

        // Duration attribute from manual timing as we don't have a way of getting timing from a tracer/segment within a method that is in the process of being timed
        long endTime = System.currentTimeMillis();
        addDuration(eventAttributes, (endTime - startTime));

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_EMBEDDING, eventAttributes);

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!

    }

    @Override
    public void recordLlmChatCompletionSummaryEvent(int numberOfMessages, long startTime, Map<String, String> linkingMetadata) {
        Map<String, Object> eventAttributes = new HashMap<>();
        // Generic attributes that are constant for all Bedrock Models
        addSpanId(eventAttributes, linkingMetadata);
        addTraceId(eventAttributes, linkingMetadata);
        addVendor(eventAttributes);
        addIngestSource(eventAttributes);

        // Attributes dependent on the request/response
        addId(eventAttributes, claudeResponse.getLlmChatCompletionSummaryId());
        addRequestId(eventAttributes, claudeResponse);
        addRequestTemperature(eventAttributes, claudeRequest);
        addRequestMaxTokens(eventAttributes, claudeRequest);
        addRequestModel(eventAttributes, claudeRequest);
        addResponseModel(eventAttributes, claudeRequest);
        addResponseNumberOfMessages(eventAttributes, numberOfMessages);
        addResponseUsageTotalTokens(eventAttributes, claudeResponse);
        addResponseUsagePromptTokens(eventAttributes, claudeResponse);
        addResponseUsageCompletionTokens(eventAttributes, claudeResponse);
        addResponseChoicesFinishReason(eventAttributes, claudeResponse);

        // Error attributes
        if (claudeResponse.isErrorResponse()) {
            addError(eventAttributes);
            claudeResponse.reportLlmError();
        }

        // Duration attribute from manual timing as we don't have a way of getting timing from a tracer/segment within a method that is in the process of being timed
        long endTime = System.currentTimeMillis();
        addDuration(eventAttributes, (endTime - startTime));

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_SUMMARY, eventAttributes);

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!
    }

    @Override
    public void recordLlmChatCompletionMessageEvent(int sequence, String message, Map<String, String> linkingMetadata) {
        Map<String, Object> eventAttributes = new HashMap<>();
        // Generic attributes that are constant for all Bedrock Models
        addSpanId(eventAttributes, linkingMetadata);
        addTraceId(eventAttributes, linkingMetadata);
        addVendor(eventAttributes);
        addIngestSource(eventAttributes);

        // Multiple completion message events can be created per transaction so generate an id on the fly instead of storing each in the response/request wrapper
        addId(eventAttributes, ModelInvocation.getRandomGuid());

        // Attributes dependent on the request/response
        addContent(eventAttributes, message);
        if (message.contains("Human:")) {
            addRole(eventAttributes, "user");
            addIsResponse(eventAttributes, false);
        } else {
            String role = claudeRequest.getRole();
            if (!role.isEmpty()) {
                addRole(eventAttributes, role);
                if (!role.contains("user")) {
                    addIsResponse(eventAttributes, true);
                }
            }
        }
        addRequestId(eventAttributes, claudeResponse);
        addResponseModel(eventAttributes, claudeRequest);
        addSequence(eventAttributes, sequence);
        addCompletionId(eventAttributes, claudeResponse);

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_MESSAGE, eventAttributes);

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes? YES!
    }

    @Override
    public void recordLlmEvents(long startTime, Map<String, String> linkingMetadata) {
        String operationType = claudeResponse.getOperationType();
        if (operationType.equals(COMPLETION)) {
            recordLlmChatCompletionEvents(startTime, linkingMetadata);
        } else if (operationType.equals(EMBEDDING)) {
            recordLlmEmbeddingEvent(startTime, linkingMetadata);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type encountered when trying to record LLM events");
        }
    }

    /**
     * Records multiple LlmChatCompletionMessage events and a single LlmChatCompletionSummary event.
     * The number of LlmChatCompletionMessage events produced can differ based on vendor.
     */
    private void recordLlmChatCompletionEvents(long startTime, Map<String, String> linkingMetadata) {
        // First LlmChatCompletionMessage represents the user input prompt
        recordLlmChatCompletionMessageEvent(0, claudeRequest.getRequestMessage(), linkingMetadata);
        // Second LlmChatCompletionMessage represents the completion message from the LLM response
        recordLlmChatCompletionMessageEvent(1, claudeResponse.getResponseMessage(), linkingMetadata);
        // A summary of all LlmChatCompletionMessage events
        recordLlmChatCompletionSummaryEvent(2, startTime, linkingMetadata);
    }

    // TODO can all of these helper methods be moved to the ModelInvocation interface???
    private void addSpanId(Map<String, Object> eventAttributes, Map<String, String> linkingMetadata) {
        String spanId = ModelInvocation.getSpanId(linkingMetadata);
        if (spanId != null && !spanId.isEmpty()) {
            eventAttributes.put("span_id", spanId);
        }
    }

    private void addTraceId(Map<String, Object> eventAttributes, Map<String, String> linkingMetadata) {
        String traceId = ModelInvocation.getTraceId(linkingMetadata);
        if (traceId != null && !traceId.isEmpty()) {
            eventAttributes.put("trace_id", traceId);
        }
    }

    private void addVendor(Map<String, Object> eventAttributes) {
        String vendor = ModelInvocation.getVendor();
        if (vendor != null && !vendor.isEmpty()) {
            eventAttributes.put("vendor", vendor);
        }
    }

    private void addIngestSource(Map<String, Object> eventAttributes) {
        String ingestSource = ModelInvocation.getIngestSource();
        if (ingestSource != null && !ingestSource.isEmpty()) {
            eventAttributes.put("ingest_source", ingestSource);
        }
    }

    private void addId(Map<String, Object> eventAttributes, String id) {
        if (id != null && !id.isEmpty()) {
            eventAttributes.put("id", id);
        }
    }

    private void addContent(Map<String, Object> eventAttributes, String message) {
        if (message != null && !message.isEmpty()) {
            eventAttributes.put("content", message);
        }
    }

    private void addRole(Map<String, Object> eventAttributes, String role) {
        if (role != null && !role.isEmpty()) {
            eventAttributes.put("role", role);
        }
    }

    private void addIsResponse(Map<String, Object> eventAttributes, boolean isResponse) {
        eventAttributes.put("is_response", isResponse);
    }

    private void addSequence(Map<String, Object> eventAttributes, int sequence) {
        if (sequence >= 0) {
            eventAttributes.put("sequence", sequence);
        }
    }

    private void addResponseNumberOfMessages(Map<String, Object> eventAttributes, int numberOfMessages) {
        if (numberOfMessages >= 0) {
            eventAttributes.put("response.number_of_messages", numberOfMessages);
        }
    }

    private void addDuration(Map<String, Object> eventAttributes, long duration) {
        if (duration >= 0) {
            eventAttributes.put("duration", duration);
        }
    }

    private void addError(Map<String, Object> eventAttributes) {
        eventAttributes.put("error", true);
    }

    private void addInput(Map<String, Object> eventAttributes, ModelRequest modelRequest) {
        String inputText = modelRequest.getInputText();
        if (inputText != null && !inputText.isEmpty()) {
            eventAttributes.put("input", inputText);
        }
    }

    private void addRequestTemperature(Map<String, Object> eventAttributes, ModelRequest modelRequest) {
        String temperature = modelRequest.getTemperature();
        if (temperature != null && !temperature.isEmpty()) {
            eventAttributes.put("request.temperature", temperature);
        }
    }

    private void addRequestMaxTokens(Map<String, Object> eventAttributes, ModelRequest modelRequest) {
        String maxTokensToSample = modelRequest.getMaxTokensToSample();
        if (maxTokensToSample != null && !maxTokensToSample.isEmpty()) {
            eventAttributes.put("request.max_tokens", maxTokensToSample);
        }
    }

    private void addRequestModel(Map<String, Object> eventAttributes, ModelRequest modelRequest) {
        String modelId = modelRequest.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            eventAttributes.put("request.model", modelId);
        }
    }

    private void addResponseModel(Map<String, Object> eventAttributes, ModelRequest modelRequest) {
        // For Bedrock the response model is the same as the request model.
        String modelId = modelRequest.getModelId();
        if (modelId != null && !modelId.isEmpty()) {
            eventAttributes.put("response.model", modelId);
        }
    }

    private void addRequestId(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        String requestId = modelResponse.getAmznRequestId();
        if (requestId != null && !requestId.isEmpty()) {
            eventAttributes.put("request_id", requestId);
        }
    }

    private void addCompletionId(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        String llmChatCompletionSummaryId = modelResponse.getLlmChatCompletionSummaryId();
        if (llmChatCompletionSummaryId != null && !llmChatCompletionSummaryId.isEmpty()) {
            eventAttributes.put("completion_id", llmChatCompletionSummaryId);
        }
    }

    private void addResponseUsageTotalTokens(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        int totalTokenCount = modelResponse.getTotalTokenCount();
        if (totalTokenCount >= 0) {
            eventAttributes.put("response.usage.total_tokens", totalTokenCount);
        }
    }

    private void addResponseUsagePromptTokens(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        int inputTokenCount = modelResponse.getInputTokenCount();
        if (inputTokenCount >= 0) {
            eventAttributes.put("response.usage.prompt_tokens", inputTokenCount);
        }
    }

    private void addResponseUsageCompletionTokens(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        int outputTokenCount = modelResponse.getOutputTokenCount();
        if (outputTokenCount >= 0) {
            eventAttributes.put("response.usage.completion_tokens", outputTokenCount);
        }
    }

    private void addResponseChoicesFinishReason(Map<String, Object> eventAttributes, ModelResponse modelResponse) {
        String stopReason = modelResponse.getStopReason();
        if (stopReason != null && !stopReason.isEmpty()) {
            eventAttributes.put("response.choices.finish_reason", stopReason);
        }
    }

}
