/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package util;

import com.newrelic.agent.introspec.Event;
import llm.embeddings.vendor.Vendor;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static llm.embeddings.events.LlmEvent.LLM_EMBEDDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmbeddingUtil {
    // request
    public static String embeddingInputString = "This is the embedding string to be vectorized";
    public static int dimensions = 10;

    // response
    public static String embeddingModelId = "text-embedding-3-small";
    public static int promptTokens = 1;
    public static int completionTokens = 2;
    public static int totalTokens = 3;

    public static EmbeddingRequest buildEmbeddingRequest() {
        List<String> embeddingInputs = new LinkedList<>();
        embeddingInputs.add(embeddingInputString);
        EmbeddingOptions embeddingOptions = EmbeddingOptionsBuilder.builder().withModel(embeddingModelId).withDimensions(dimensions).build();
        return new EmbeddingRequest(embeddingInputs, embeddingOptions);
    }

    public static EmbeddingResponse buildEmbeddingResponse() {
        DefaultUsage defaultUsage = new DefaultUsage(promptTokens, completionTokens, totalTokens);

        EmbeddingResponseMetadata embeddingResponseMetadata = new EmbeddingResponseMetadata(embeddingModelId, defaultUsage);

        List<Embedding> embeddings = new LinkedList<>();
        float[] embeddingValues = new float[4];
        embeddingValues[0] = -0.003945629f;
        embeddingValues[1] = -0.041435882f;
        embeddingValues[2] = -0.007938714f;
        embeddingValues[3] = 0.008420054f;
        Embedding embedding = new Embedding(embeddingValues, 0);
        embeddings.add(embedding);

        return new EmbeddingResponse(embeddings, embeddingResponseMetadata);
    }

    public static void assertLlmEmbeddingAttributes(Event event, String modelId, String requestInput) {
        assertEquals(LLM_EMBEDDING, event.getType());

        Map<String, Object> attributes = event.getAttributes();
        assertFalse(((String) attributes.get("id")).isEmpty());
        assertFalse(((String) attributes.get("request_id")).isEmpty());
        assertEquals(requestInput, attributes.get("input"));
        assertEquals(modelId, attributes.get("request.model"));
        assertEquals(modelId, attributes.get("response.model"));
        assertEquals(totalTokens, attributes.get("response.usage.total_tokens"));
        assertEquals(Vendor.VENDOR, attributes.get("vendor"));
        assertEquals(Vendor.INGEST_SOURCE, attributes.get("ingest_source"));
        assertTrue(((Float) attributes.get("duration")) >= 0);
        assertEquals("testPrefix", attributes.get("llm.testPrefix"));
        assertEquals("conversation-id-value", attributes.get("llm.conversation_id"));
    }
}
