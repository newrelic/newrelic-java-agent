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

import static llm.vendor.Vendor.VENDOR;

public interface ModelInvocation {
    /**
     * Set name of the traced method for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     * <p>
     * Used with the sync client
     */
    void setTracedMethodName(Transaction txn, String functionName);

    /**
     * Set name of the async segment for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     * <p>
     * Used with the async client
     */
    void setSegmentName(Segment segment, String functionName);

    void recordLlmEmbeddingEvent(long startTime);

    void recordLlmChatCompletionSummaryEvent(long startTime, int numberOfMessages);

    void recordLlmChatCompletionMessageEvent(int sequence, String message);

    void recordLlmEvents(long startTime);

    // This causes the txn to be active on the thread where the LlmEvents are created so that they properly added to the event reservoir on the txn. This is used when the model response is returned asynchronously.
    void recordLlmEventsAsync(long startTime, Token token);

    void reportLlmError();

    Map<String, String> getLinkingMetadata();

    Map<String, Object> getUserAttributes();

    ModelRequest getModelRequest();

    ModelResponse getModelResponse();

    /**
     * This needs to be incremented for every invocation of the SDK.
     * Supportability/{language}/ML/{vendor_name}/{vendor_version}
     */
    static void incrementInstrumentedSupportabilityMetric(String vendorVersion) {
        NewRelic.incrementCounter("Supportability/Java/ML/" + VENDOR + "/" + vendorVersion);
    }

    static void incrementStreamingDisabledSupportabilityMetric() {
        NewRelic.incrementCounter("Supportability/Java/ML/Streaming/Disabled");
    }

    static void setLlmTrueAgentAttribute(Transaction txn) {
        // If in a txn with LLM-related spans
        txn.getAgentAttributes().put("llm", true);
    }

    static String getSpanId(Map<String, String> linkingMetadata) {
        if (linkingMetadata != null && !linkingMetadata.isEmpty()) {
            return linkingMetadata.get("span.id");
        }
        return "";
    }

    // ID of the current trace
    static String getTraceId(Map<String, String> linkingMetadata) {
        if (linkingMetadata != null && !linkingMetadata.isEmpty()) {
            return linkingMetadata.get("trace.id");
        }
        return "";
    }

    // Returns a string representation of a random GUID
    static String getRandomGuid() {
        return UUID.randomUUID().toString();
    }
}
