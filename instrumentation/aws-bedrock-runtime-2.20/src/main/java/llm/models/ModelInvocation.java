/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

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

    void recordLlmEmbeddingEvent(long startTime, Map<String, String> linkingMetadata);

    void recordLlmChatCompletionSummaryEvent(int numberOfMessages, long startTime, Map<String, String> linkingMetadata);

    void recordLlmChatCompletionMessageEvent(int sequence, String message, Map<String, String> linkingMetadata);

    void recordLlmEvents(long startTime, Map<String, String> linkingMetadata);

    void reportLlmError();

    /**
     * This needs to be incremented for every invocation of the SDK.
     * Supportability/{language}/ML/{vendor_name}/{vendor_version}
     * <p>
     * The metric generated triggers the creation of a tag which gates the AI Response UI. The
     * tag lives for 27 hours so if this metric isn't repeatedly sent the tag will disappear and
     * the UI will be hidden.
     */
    static void incrementInstrumentedSupportabilityMetric(String vendorVersion) {
        NewRelic.incrementCounter("Supportability/Java/ML/" + VENDOR + "/" + vendorVersion);
    }

    static void setLlmTrueAgentAttribute(Transaction txn) {
        // If in a txn with LLM-related spans
        txn.getAgentAttributes().put("llm", true);
    }

    // GUID associated with the active trace
    static String getSpanId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get("span.id");
    }

    // ID of the current trace
    static String getTraceId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get("trace.id");
    }

    // Returns a string representation of a random GUID
    static String getRandomGuid() {
        return UUID.randomUUID().toString();
    }
}
