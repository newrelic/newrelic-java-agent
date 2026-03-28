/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.models.springai;

import com.newrelic.api.agent.NewRelic;
import llm.models.ModelResponse;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.logging.Level;

import static llm.models.ModelInvocation.getRandomGuid;

/**
 * Stores the required info from the SpringAI EmbeddingResponse without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class SpringAiModelResponse implements ModelResponse {
    // LLM operation type
    private String operationType = "";
    private int statusCode = 0;
    private String statusText = "";
    private String responseOrganization = "";
    private String llmEmbeddingId = "";
    private int totalTokens = 0;
    private String model = "";
    private String requestId = "";

    public SpringAiModelResponse(EmbeddingResponse embeddingResponse) {
        if (embeddingResponse != null) {
            EmbeddingResponseMetadata embeddingResponseMetadata = embeddingResponse.getMetadata();
            if (embeddingResponseMetadata != null) {
                model = embeddingResponseMetadata.getModel();
                Usage usage = embeddingResponseMetadata.getUsage();
                if (usage != null) {
                    totalTokens = usage.getTotalTokens();
                }
            }

            llmEmbeddingId = getRandomGuid();
            requestId = getRandomGuid();
            operationType = EMBEDDING;
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "AIM: Received null SpringAI EmbeddingResponse");
        }
    }

    @Override
    public String getRequestId() {
        // Not available
        return requestId;
    }

    @Override
    public String getOperationType() {
        return operationType;
    }

    @Override
    public String getLlmEmbeddingId() {
        return llmEmbeddingId != null ? llmEmbeddingId : getRandomGuid();
    }

    @Override
    public boolean isErrorResponse() {
        /*
         * The EmbeddingResponse doesn't seem to include any info about errors.
         * When a request in Spring AI fails, the error response is typically
         * encapsulated in a Spring RestClientException or a similar exception type
         * within the Spring framework, such as WebClientRequestException (for streaming calls)
         * or an IOException related to connection issues.
         */
        return false;
    }

    @Override
    public int getStatusCode() {
        // Not available
        return statusCode;
    }

    @Override
    public String getStatusText() {
        // Not available
        return statusText;
    }

    @Override
    public String getModelId() {
        return model;
    }

    @Override
    public String getResponseOrganization() {
        // Not available
        return responseOrganization;
    }

    @Override
    public Integer getResponseUsageTotalTokens() {
        return totalTokens;
    }
}
