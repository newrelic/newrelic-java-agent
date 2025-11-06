/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import com.newrelic.agent.introspec.Event;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import llm.models.ModelResponse;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_MESSAGE;
import static llm.events.LlmEvent.LLM_CHAT_COMPLETION_SUMMARY;
import static llm.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientMock.completionModelId;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientMock.completionRequestInput;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientMock.embeddingModelId;
import static software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientMock.embeddingRequestInput;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.bedrockruntime" }, configName = "llm_disabled_server_side.yml")

public class AIM_Disabled_ServerSide_BedrockRuntimeClient_InstrumentationTest {
    private static final BedrockRuntimeClientMock mockBedrockRuntimeClient = new BedrockRuntimeClientMock();
    private final Introspector introspector = InstrumentationTestRunner.getIntrospector();

    @Before
    public void before() {
        introspector.clear();
    }

    @Test
    public void testInvokeModelCompletion() {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertNoLlmTransaction(ModelResponse.COMPLETION);
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents(ModelResponse.COMPLETION);
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testInvokeModelEmbedding() {
        boolean isError = false;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertNoLlmTransaction(ModelResponse.EMBEDDING);
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents(ModelResponse.EMBEDDING);
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testInvokeModelCompletionError() {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanCompletionRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertNoLlmTransaction(ModelResponse.COMPLETION);
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents(ModelResponse.COMPLETION);
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    @Test
    public void testInvokeModelEmbeddingError() {
        boolean isError = true;
        InvokeModelRequest invokeModelRequest = buildAmazonTitanEmbeddingRequest(isError);
        InvokeModelResponse invokeModelResponse = invokeModelInTransaction(invokeModelRequest);

        assertNotNull(invokeModelResponse);
        assertNoLlmTransaction(ModelResponse.EMBEDDING);
        assertNoLlmSupportabilityMetrics();
        assertNoLlmEvents(ModelResponse.EMBEDDING);
        assertTrue(introspector.getErrorEvents().isEmpty());
    }

    private static InvokeModelRequest buildAmazonTitanCompletionRequest(boolean isError) {
        JSONObject textGenerationConfig = new JSONObject()
                .put("maxTokenCount", 1000)
                .put("stopSequences", Collections.singletonList("User:"))
                .put("temperature", 0.5)
                .put("topP", 0.9);

        String payload = new JSONObject()
                .put("inputText", completionRequestInput)
                .put("textGenerationConfig", textGenerationConfig)
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(completionModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    private static InvokeModelRequest buildAmazonTitanEmbeddingRequest(boolean isError) {
        String payload = new JSONObject()
                .put("inputText", embeddingRequestInput)
                .put("errorTest", isError) // this is not a real model attribute, just adding for testing
                .toString();

        return InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(embeddingModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();
    }

    @Trace(dispatcher = true)
    private InvokeModelResponse invokeModelInTransaction(InvokeModelRequest invokeModelRequest) {
        NewRelic.addCustomParameter("llm.conversation_id", "conversation-id-value"); // Will be added to LLM events
        NewRelic.addCustomParameter("llm.testPrefix", "testPrefix"); // Will be added to LLM events
        NewRelic.addCustomParameter("test", "test"); // Will NOT be added to LLM events
        return mockBedrockRuntimeClient.invokeModel(invokeModelRequest);
    }

    private void assertNoLlmTransaction(String operationType) {
        assertEquals(1, introspector.getFinishedTransactionCount(TimeUnit.SECONDS.toMillis(2)));
        Collection<String> transactionNames = introspector.getTransactionNames();
        String transactionName = transactionNames.iterator().next();
        Map<String, TracedMetricData> metrics = introspector.getMetricsForTransaction(transactionName);
        assertFalse(metrics.containsKey("Llm/" + operationType + "/Bedrock/invokeModel"));
    }

    private void assertNoLlmSupportabilityMetrics() {
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();
        assertFalse(unscopedMetrics.containsKey("Supportability/Java/ML/Bedrock/2.20"));
    }

    private void assertNoLlmEvents(String operationType) {
        if (ModelResponse.COMPLETION.equals(operationType)) {
            // LlmChatCompletionMessage events
            Collection<Event> llmChatCompletionMessageEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_MESSAGE);
            assertEquals(0, llmChatCompletionMessageEvents.size());

            // LlmCompletionSummary events
            Collection<Event> llmCompletionSummaryEvents = introspector.getCustomEvents(LLM_CHAT_COMPLETION_SUMMARY);
            assertEquals(0, llmCompletionSummaryEvents.size());
        } else if (ModelResponse.EMBEDDING.equals(operationType)) {
            // LlmEmbedding events
            Collection<Event> llmEmbeddingEvents = introspector.getCustomEvents(LLM_EMBEDDING);
            assertEquals(0, llmEmbeddingEvents.size());
        }
    }
}
