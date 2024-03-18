/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.agent.bridge.Token;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;

import java.util.Map;
import java.util.UUID;

import static llm.vendor.Vendor.BEDROCK;

public interface ModelInvocation {
    /**
     * Set name of the traced method for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     * <p>
     * Used with the sync client
     *
     * @param txn          current transaction
     * @param functionName name of SDK function being invoked
     */
    void setTracedMethodName(Transaction txn, String functionName);

    /**
     * Set name of the async segment for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     * <p>
     * Used with the async client
     *
     * @param segment      active segment for async timing
     * @param functionName name of SDK function being invoked
     */
    void setSegmentName(Segment segment, String functionName);

    /**
     * Record an LlmEmbedding event that captures data specific to the creation of an embedding.
     *
     * @param startTime start time of SDK invoke method
     * @param index     of the input message in an array
     */
    void recordLlmEmbeddingEvent(long startTime, int index);

    /**
     * Record an LlmChatCompletionSummary event that captures high-level data about
     * the creation of a chat completion including request, response, and call information.
     *
     * @param startTime        start time of SDK invoke method
     * @param numberOfMessages total number of LlmChatCompletionMessage events associated with the summary
     */
    void recordLlmChatCompletionSummaryEvent(long startTime, int numberOfMessages);

    /**
     * Record an LlmChatCompletionMessage event that corresponds to each message (sent and received)
     * from a chat completion call including those created by the user, assistant, and the system.
     *
     * @param sequence index starting at 0 associated with each message
     * @param message  String representing the input/output message
     * @param isUser   boolean representing if the current message event is from a user input prompt or an assistant response message
     */
    void recordLlmChatCompletionMessageEvent(int sequence, String message, boolean isUser);

    /**
     * Record all LLM events when using the sync client.
     *
     * @param startTime start time of SDK invoke method
     */
    void recordLlmEvents(long startTime);

    /**
     * Record all LLM events when using the async client.
     * <p>
     * This causes the txn to be active on the thread where the LlmEvents are created so
     * that they properly added to the event reservoir on the txn. This is used when the
     * model response is returned asynchronously via CompleteableFuture.
     *
     * @param startTime start time of SDK invoke method
     * @param token     Token used to link the transaction to the thread that produces the response
     */
    void recordLlmEventsAsync(long startTime, Token token);

    /**
     * Report an LLM error.
     */
    void reportLlmError();

    /**
     * Get a map of linking metadata.
     *
     * @return Map of linking metadata
     */
    Map<String, String> getLinkingMetadata();

    /**
     * Get a map of user custom attributes.
     *
     * @return Map of user custom attributes
     */
    Map<String, Object> getUserAttributes();

    /**
     * Get a ModelRequest wrapper class for the SDK Request object.
     *
     * @return ModelRequest
     */
    ModelRequest getModelRequest();

    /**
     * Get a ModelResponse wrapper class for the SDK Response object.
     *
     * @return ModelResponse
     */
    ModelResponse getModelResponse();

    /**
     * Increment a Supportability metric indicating that the SDK was instrumented.
     * <p>
     * This needs to be incremented for every invocation of the SDK.
     * Supportability/{language}/ML/{vendor_name}/{vendor_version}
     *
     * @param vendorVersion version of vendor
     */
    static void incrementInstrumentedSupportabilityMetric(String vendorVersion) {
        NewRelic.incrementCounter("Supportability/Java/ML/" + BEDROCK + "/" + vendorVersion);
    }

    /**
     * Set the llm:true attribute on the active transaction.
     *
     * @param txn current transaction
     */
    static void setLlmTrueAgentAttribute(Transaction txn) {
        // If in a txn with LLM-related spans
        txn.getAgentAttributes().put("llm", true);
    }

    /**
     * Get the span.id attribute from the map of linking metadata.
     *
     * @param linkingMetadata Map of linking metadata
     * @return String representing the span.id
     */
    static String getSpanId(Map<String, String> linkingMetadata) {
        if (linkingMetadata != null && !linkingMetadata.isEmpty()) {
            return linkingMetadata.get("span.id");
        }
        return "";
    }

    /**
     * Get the trace.id attribute from the map of linking metadata.
     *
     * @param linkingMetadata Map of linking metadata
     * @return String representing the trace.id
     */
    static String getTraceId(Map<String, String> linkingMetadata) {
        if (linkingMetadata != null && !linkingMetadata.isEmpty()) {
            return linkingMetadata.get("trace.id");
        }
        return "";
    }

    /**
     * Generate a string representation of a random GUID
     *
     * @return String representation of a GUID
     */
    static String getRandomGuid() {
        return UUID.randomUUID().toString();
    }
}
