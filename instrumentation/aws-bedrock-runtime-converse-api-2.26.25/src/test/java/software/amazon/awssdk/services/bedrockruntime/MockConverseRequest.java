/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.core.document.internal.BooleanDocument;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

import java.util.ArrayList;
import java.util.List;

import static llm.converse.models.TestUtil.REQUEST_CONTENT_TEXT;
import static llm.converse.models.TestUtil.REQUEST_MAX_TOKENS;
import static llm.converse.models.TestUtil.REQUEST_MODEL_ID;
import static llm.converse.models.TestUtil.REQUEST_ROLE;
import static llm.converse.models.TestUtil.REQUEST_TEMPERATURE;

public class MockConverseRequest {
    public static ConverseRequest converseRequest(boolean isError) {
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().maxTokens(REQUEST_MAX_TOKENS).temperature(REQUEST_TEMPERATURE).build();

        List<Message> messages = new ArrayList<>();
        List<ContentBlock> content = new ArrayList<>();
        ContentBlock contentBlock = ContentBlock.builder().text(REQUEST_CONTENT_TEXT).build();
        content.add(contentBlock);
        Message message = Message.builder().role(REQUEST_ROLE).content(content).build();
        messages.add(message);

        // Hijacking additionalModelRequestFields with a BooleanDocument to signal an error condition
        BooleanDocument booleanDocument = new BooleanDocument(isError);
        return ConverseRequest.builder().additionalModelRequestFields(booleanDocument).modelId(REQUEST_MODEL_ID).inferenceConfig(inferenceConfiguration).messages(messages).build();
    }

    // TODO Stream support not implemented
    public static ConverseStreamRequest converseStreamRequest(boolean isError) {
        InferenceConfiguration inferenceConfiguration = InferenceConfiguration.builder().maxTokens(REQUEST_MAX_TOKENS).temperature(REQUEST_TEMPERATURE).build();

        List<Message> messages = new ArrayList<>();
        List<ContentBlock> content = new ArrayList<>();
        ContentBlock contentBlock = ContentBlock.builder().text(REQUEST_CONTENT_TEXT).build();
        content.add(contentBlock);
        Message message = Message.builder().role(REQUEST_ROLE).content(content).build();
        messages.add(message);

        // Hijacking additionalModelRequestFields with a BooleanDocument to signal an error condition
        BooleanDocument booleanDocument = new BooleanDocument(isError);
        return ConverseStreamRequest.builder().additionalModelRequestFields(booleanDocument).modelId(REQUEST_MODEL_ID).inferenceConfig(inferenceConfiguration).messages(messages).build();
    }
}
