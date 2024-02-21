/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;

import java.util.Map;
import java.util.UUID;

public interface ModelInvocation {
    String VENDOR = "bedrock";
    String BEDROCK = "Bedrock";
    String INGEST_SOURCE = "Java";
    String TRACE_ID = "trace.id";
    String SPAN_ID = "span.id";

    // LLM event types
    String LLM_EMBEDDING = "LlmEmbedding";
    String LLM_CHAT_COMPLETION_SUMMARY = "LlmChatCompletionSummary";
    String LLM_CHAT_COMPLETION_MESSAGE = "LlmChatCompletionMessage";

    // Supported models
    String ANTHROPIC_CLAUDE = "claude";
    String AMAZON_TITAN = "titan";
    String META_LLAMA_2 = "llama";
    String COHERE_COMMAND = "cohere";
    String AI_21_LABS_JURASSIC = "jurassic";

    /**
     * Set name of the span/segment for each LLM embedding and chat completion call
     * Llm/{operation_type}/{vendor_name}/{function_name}
     *
     * @param txn           current transaction
     */
    void setLlmOperationMetricName(Transaction txn, String functionName);

    void recordLlmEmbeddingEvent(long startTime, Map<String, String> linkingMetadata);

    void recordLlmChatCompletionSummaryEvent(int numberOfMessages, long startTime, Map<String, String> linkingMetadata);

    void recordLlmChatCompletionMessageEvent(int sequence, String message, Map<String, String> linkingMetadata);

    void recordLlmEvents(long startTime, Map<String, String> linkingMetadata);


    /**
     * This needs to be incremented for every invocation of the SDK.
     * Supportability/{language}/ML/{vendor_name}/{vendor_version}
     * <p>
     * The metric generated triggers the creation of a tag which gates the AI Response UI. The
     * tag lives for 27 hours so if this metric isn't repeatedly sent the tag will disappear and
     * the UI will be hidden.
     */
    static void incrementInstrumentedSupportabilityMetric() {
        // Bedrock vendor_version isn't available, so set it to instrumentation version instead
        NewRelic.incrementCounter("Supportability/Java/ML/Bedrock/2.20");
    }

    static void setLlmTrueAgentAttribute(Transaction txn) {
        // If in a txn with LLM-related spans
        txn.getAgentAttributes().put("llm", true);
    }

    // Lowercased name of vendor (bedrock or openAI)
    static String getVendor() {
        return VENDOR;
    }

    // Name of the language agent (ex: Python, Node)
    static String getIngestSource() {
        return INGEST_SOURCE;
    }

    // GUID associated with the active trace
    static String getSpanId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get(SPAN_ID);
    }

    // ID of the current trace
    static String getTraceId(Map<String, String> linkingMetadata) {
        return linkingMetadata.get(TRACE_ID);
    }

    // Returns a string representation of a random GUID
    static String getRandomGuid() {
        return UUID.randomUUID().toString();
    }
}
