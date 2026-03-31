/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package llm.embeddings.models.springai;

import com.newrelic.api.agent.NewRelic;
import llm.embeddings.models.ModelRequest;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Stores the required info from the SpringAI EmbeddingRequest without holding
 * a reference to the actual request object to avoid potential memory issues.
 */
public class SpringAiModelRequest implements ModelRequest {
    private String modelId = "";
    private int numberOfInputTextMessages = 0;
    private List<String> instructions = new ArrayList<>();

    public SpringAiModelRequest(EmbeddingRequest embeddingRequest) {
        if (embeddingRequest != null) {
            instructions.addAll(embeddingRequest.getInstructions());
            if (instructions != null && !instructions.isEmpty()) {
                numberOfInputTextMessages = instructions.size();
            }
            EmbeddingOptions embeddingOptions = embeddingRequest.getOptions();
            if (embeddingOptions != null) {
                modelId = embeddingOptions.getModel();
            }
        } else {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "AIM: Received null SpringAI EmbeddingRequest");
        }
    }

    @Override
    public String getInputText(int index) {
        return instructions.get(index);
    }

    @Override
    public int getNumberOfInputTextMessages() {
        return numberOfInputTextMessages;
    }

    @Override
    public String getModelId() {
        return modelId;
    }
}
