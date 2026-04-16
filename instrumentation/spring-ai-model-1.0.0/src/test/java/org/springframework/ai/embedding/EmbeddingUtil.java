/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.embedding;

import org.springframework.ai.chat.metadata.DefaultUsage;

import java.util.LinkedList;
import java.util.List;

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
}
