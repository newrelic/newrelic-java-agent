/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models.springai;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import llm.embeddings.events.LlmEvent;
import llm.embeddings.models.ModelInvocation;
import llm.embeddings.models.ModelRequest;
import llm.embeddings.models.ModelResponse;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static llm.embeddings.models.ModelResponse.EMBEDDING;
import static llm.embeddings.vendor.Vendor.SPRING_AI;

public class SpringAiModelInvocation implements ModelInvocation {
    Map<String, String> linkingMetadata;
    Map<String, Object> userAttributes;
    ModelRequest modelRequest;
    ModelResponse modelResponse;

    /**
     * Construct a SpringAiModelInvocation which encapsulates the request and response to/from an LLM.
     *
     * @param linkingMetadata      agent's context linking data
     * @param userCustomAttributes user custom attributes
     * @param embeddingRequest     a EmbeddingRequest from a client call
     * @param embeddingResponse    a EmbeddingResponse from a client call
     */
    public SpringAiModelInvocation(Map<String, String> linkingMetadata, Map<String, Object> userCustomAttributes,
            EmbeddingRequest embeddingRequest, EmbeddingResponse embeddingResponse) {
        this.linkingMetadata = linkingMetadata;
        this.userAttributes = userCustomAttributes;
        this.modelRequest = new SpringAiModelRequest(embeddingRequest);
        this.modelResponse = new SpringAiModelResponse(embeddingResponse);
    }

    @Override
    public void setTracedMethodName(Transaction txn, String functionName) {
        txn.getTracedMethod().setMetricName("Llm", modelResponse.getOperationType(), SPRING_AI, functionName);
    }

    @Override
    public void recordLlmEmbeddingEvent(long startTime, int index) {
        if (modelResponse.isErrorResponse()) {
            reportLlmError();
        }

        LlmEvent.Builder builder = new LlmEvent.Builder(this);

        LlmEvent llmEmbeddingEvent = builder
                .id(modelResponse.getLlmEmbeddingId())
                .requestId()
                .spanId()
                .traceId()
                .input(index)
                .requestModel()
                .responseModel()
                .responseOrganization()
                .responseUsageTotalTokens()
                .vendor()
                .ingestSource()
                .duration(System.currentTimeMillis() - startTime)
                .error()
                .build();

        llmEmbeddingEvent.recordLlmEmbeddingEvent();
    }

    @Override
    public void recordLlmEvents(long startTime) {
        String operationType = modelResponse.getOperationType();
        if (operationType.equals(EMBEDDING)) {
            recordLlmEmbeddingEvents(startTime);
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Unexpected operation type encountered when trying to record LLM events");
        }
    }

    @Override
    public void reportLlmError() {
        Map<String, Object> errorParams = new HashMap<>();
        // statusCode not available from EmbeddingResponse
        int statusCode = modelResponse.getStatusCode();
        if (statusCode > 0) {
            errorParams.put("http.statusCode", statusCode);
            errorParams.put("error.code", statusCode);
        }
        if (!modelResponse.getLlmEmbeddingId().isEmpty()) {
            errorParams.put("embedding_id", modelResponse.getLlmEmbeddingId());
        }
        // statusText not available from EmbeddingResponse
        NewRelic.noticeError("LlmError: " + modelResponse.getStatusText(), errorParams);
    }

    /**
     * Records one, and potentially more, LlmEmbedding events based on the number of input messages in the request.
     * The number of LlmEmbedding events produced can differ based on vendor.
     */
    private void recordLlmEmbeddingEvents(long startTime) {
        int numberOfRequestMessages = modelRequest.getNumberOfInputTextMessages();
        // Record an LlmEmbedding event for each input message in the request
        for (int i = 0; i < numberOfRequestMessages; i++) {
            recordLlmEmbeddingEvent(startTime, i);
        }
    }

    @Override
    public Map<String, String> getLinkingMetadata() {
        return linkingMetadata;
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return userAttributes;
    }

    @Override
    public ModelRequest getModelRequest() {
        return modelRequest;
    }

    @Override
    public ModelResponse getModelResponse() {
        return modelResponse;
    }
}
