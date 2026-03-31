/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.aimonitoring.LlmTokenCountCallbackHolder;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static llm.embeddings.vendor.Vendor.SPRING_AI;

public interface ModelInvocation {
    /**
     * Set name of the traced method for each LLM embedding call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     * <p>
     * Used with the sync client
     *
     * @param txn          current transaction
     * @param functionName name of SDK function being invoked
     */
    void setTracedMethodName(Transaction txn, String functionName);

    /**
     * Record an LlmEmbedding event that captures data specific to the creation of an embedding.
     *
     * @param startTime start time of SDK invoke method
     * @param index     of the input message in an array
     */
    void recordLlmEmbeddingEvent(long startTime, int index);

    /**
     * Record all LLM events when using the sync client.
     *
     * @param startTime start time of SDK invoke method
     */
    void recordLlmEvents(long startTime);

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
        NewRelic.incrementCounter("Supportability/Java/ML/" + SPRING_AI + "/" + vendorVersion);
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

    /**
     * Calculates the tokenCount based on a user provided callback
     *
     * @param model   String representation of the LLM model
     * @param content String representation of the message content or prompt
     * @return int representing the tokenCount
     */
    static int getTokenCount(String model, String content) {
        if (LlmTokenCountCallbackHolder.getLlmTokenCountCallback() == null || Objects.equals(content, "")) {
            return 0;
        }
        return LlmTokenCountCallbackHolder
                .getLlmTokenCountCallback()
                .calculateLlmTokenCount(model, content);
    }
}
