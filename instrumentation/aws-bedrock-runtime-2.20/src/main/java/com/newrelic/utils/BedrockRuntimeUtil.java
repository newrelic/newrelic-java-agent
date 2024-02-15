/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;

import java.util.HashMap;
import java.util.Map;

public class BedrockRuntimeUtil {
    private static final String VENDOR = "bedrock";
    private static final String INGEST_SOURCE = "Java";
    private static final String TRACE_ID = "trace.id";
    private static final String SPAN_ID = "span.id";

    // LLM event types
    private static final String LLM_EMBEDDING = "LlmEmbedding";
    private static final String LLM_CHAT_COMPLETION_SUMMARY = "LlmChatCompletionSummary";
    private static final String LLM_CHAT_COMPLETION_MESSAGE = "LlmChatCompletionMessage";

    /**
     * This needs to be incremented for every invocation of the Bedrock SDK.
     * <p>
     * The metric generated triggers the creation of a tag which gates the AI Response UI. The
     * tag lives for 27 hours so if this metric isn't repeatedly sent the tag will disappear and
     * the UI will be hidden.
     */
    public static void incrementBedrockInstrumentedMetric() {
        // FIXME get library version, not instrumentation version, probably not possible
        NewRelic.incrementCounter("Java/ML/Bedrock/2.20");
    }

    /**
     * Set name of the span/segment for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     *
     * @param txn           current transaction
     * @param operationType operation of type completion or embedding
     */
    public static void setLlmOperationMetricName(Transaction txn, String operationType) {
        txn.getTracedMethod().setMetricName("Llm", operationType, "Bedrock", "invokeModel");
    }

    // TODO add event builders??? Avoid adding null/empty attributes?

    // TODO create a single recordLlmEvent method that can take a type. Always add attributes common to
    //  all types and add others based on conditionals

    public static void recordLlmEmbeddingEvent(Transaction txn, Map<String, String> linkingMetadata, InvokeModelRequestWrapper invokeModelRequestWrapper,
            InvokeModelResponseWrapper invokeModelResponseWrapper) {

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction

        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("id", invokeModelResponseWrapper.getLlmEmbeddingId());
        eventAttributes.put("request_id", invokeModelResponseWrapper.getAmznRequestId());
        eventAttributes.put("span_id", getSpanId(linkingMetadata));
        eventAttributes.put("transaction_id", getTraceId(linkingMetadata)); // FIXME figure out how to get txn ID from linking metadata
        eventAttributes.put("trace_id", getTraceId(linkingMetadata));
        eventAttributes.put("input", invokeModelRequestWrapper.getInputText());
        eventAttributes.put("request.model", invokeModelRequestWrapper.getModelId());
        eventAttributes.put("response.model", invokeModelRequestWrapper.getModelId()); // For Bedrock it is the same as the request model.
        eventAttributes.put("response.usage.total_tokens", invokeModelResponseWrapper.getTotalTokenCount());
        eventAttributes.put("response.usage.prompt_tokens", invokeModelResponseWrapper.getInputTokenCount());
        eventAttributes.put("vendor", getVendor());
        eventAttributes.put("ingest_source", getIngestSource());
//        eventAttributes.put("duration", "NOT POSSIBLE"); // TODO Total time taken for the chat completion or embedding call to complete
        if (invokeModelResponseWrapper.isErrorResponse()) {
            eventAttributes.put("error", true); // TODO Bool set to True if an error occurred during creation call - omitted if no error occurred
//            NewRelic.noticeError(invokeModelResponseWrapper.getStatusText());
        }
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_EMBEDDING, eventAttributes);
    }

    public static void recordLlmChatCompletionSummaryEvent(Transaction txn, Map<String, String> linkingMetadata,
            InvokeModelRequestWrapper invokeModelRequestWrapper, InvokeModelResponseWrapper invokeModelResponseWrapper) {

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction

        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("id", invokeModelResponseWrapper.getLlmChatCompletionSummaryId());
        eventAttributes.put("request_id", invokeModelResponseWrapper.getAmznRequestId());
        eventAttributes.put("span_id", getSpanId(linkingMetadata));
        eventAttributes.put("transaction_id", getTraceId(linkingMetadata)); // FIXME figure out how to get txn ID from linking metadata
        eventAttributes.put("trace_id", getTraceId(linkingMetadata));
        eventAttributes.put("request.temperature", invokeModelRequestWrapper.getTemperature());
        eventAttributes.put("request.max_tokens", invokeModelRequestWrapper.getMaxTokensToSample());
        eventAttributes.put("request.model", invokeModelRequestWrapper.getModelId());
        eventAttributes.put("response.model", invokeModelRequestWrapper.getModelId()); // For Bedrock it is the same as the request model.
        eventAttributes.put("response.number_of_messages",
                ""); // TODO Number of messages comprising a chat completion including system, user, and assistant messages
        eventAttributes.put("response.usage.total_tokens", invokeModelResponseWrapper.getTotalTokenCount());
        eventAttributes.put("response.usage.prompt_tokens", invokeModelResponseWrapper.getInputTokenCount());
        eventAttributes.put("response.usage.completion_tokens", invokeModelResponseWrapper.getOutputTokenCount());
        eventAttributes.put("response.choices.finish_reason", invokeModelResponseWrapper.getStopReason());
        eventAttributes.put("vendor", getVendor());
        eventAttributes.put("ingest_source", getIngestSource());
//        eventAttributes.put("duration", "NOT POSSIBLE"); // TODO Total time taken for the chat completion or embedding call to complete
        if (invokeModelResponseWrapper.isErrorResponse()) {
            eventAttributes.put("error", true); // TODO Bool set to True if an error occurred during creation call - omitted if no error occurred
//            NewRelic.noticeError(invokeModelResponseWrapper.getStatusText());
        }
//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes?

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_SUMMARY, eventAttributes);
    }

    public static void recordLlmChatCompletionMessageEvent(Transaction txn, Map<String, String> linkingMetadata,
            InvokeModelRequestWrapper invokeModelRequestWrapper, InvokeModelResponseWrapper invokeModelResponseWrapper) {

        // TODO is it possible to do something like this to call getUserAttributes?
        //  see com.newrelic.agent.bridge.Transaction

        Map<String, Object> eventAttributes = new HashMap<>();
        eventAttributes.put("id", invokeModelResponseWrapper.getLlmChatCompletionMessageId());
        eventAttributes.put("request_id", invokeModelResponseWrapper.getAmznRequestId());
        eventAttributes.put("span_id", getSpanId(linkingMetadata));
        eventAttributes.put("transaction_id", getTraceId(linkingMetadata)); // FIXME figure out how to get txn ID from linking metadata
        eventAttributes.put("trace_id", getTraceId(linkingMetadata));
//        eventAttributes.put("llm.conversation_id", "NEW API"); // TODO Optional attribute that can be added to a transaction by a customer via add_custom_attribute API. Should just be added and prefixed along with the other user attributes?
        eventAttributes.put("response.model", invokeModelRequestWrapper.getModelId()); // For Bedrock it is the same as the request model.
        eventAttributes.put("vendor", getVendor());
        eventAttributes.put("ingest_source", getIngestSource());
        eventAttributes.put("content", invokeModelRequestWrapper.getPrompt());
        String role = invokeModelRequestWrapper.getRole();
        if (!role.isEmpty()) {
            eventAttributes.put("role", role);
            if (!role.contains("user")) {
                eventAttributes.put("is_response", true);
            }
        }
        eventAttributes.put("sequence", ""); // TODO Index (beginning at 0) associated with each message including the prompt and responses
        eventAttributes.put("completion_id", invokeModelResponseWrapper.getLlmChatCompletionSummaryId());

//        eventAttributes.put("llm.<user_defined_metadata>", ""); // TODO Optional metadata attributes that can be added to a transaction by a customer via add_custom_attribute API. Done internally when event is created?

        NewRelic.getAgent().getInsights().recordCustomEvent(LLM_CHAT_COMPLETION_MESSAGE, eventAttributes);
    }

    // ========================= AGENT DATA ================================
    // Lowercased name of vendor (bedrock or openAI)
    public static String getVendor() {
        return VENDOR;
    }

    // Name of the language agent (ex: Python, Node)
    public static String getIngestSource() {
        return INGEST_SOURCE;
    }

    // GUID associated with the active trace
    public static String getSpanId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get(SPAN_ID);
    }

    // ID of the current trace
    public static String getTraceId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get(TRACE_ID);
    }
}
