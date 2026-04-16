/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.embedding;

import org.springframework.ai.document.Document;

import java.util.List;

import static util.EmbeddingUtil.buildEmbeddingResponse;

public class MockEmbeddingModel extends AbstractEmbeddingModel {
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return buildEmbeddingResponse();
    }

    @Override
    public float[] embed(String text) {
        return super.embed(text);
    }

    @Override
    public float[] embed(Document document) {
        return new float[0];
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return super.embed(texts);
    }

    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        return super.embed(documents, options, batchingStrategy);
    }

    @Override
    public EmbeddingResponse embedForResponse(List<String> texts) {
        return super.embedForResponse(texts);
    }
}
