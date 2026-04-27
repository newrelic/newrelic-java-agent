/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.ai.chat.client;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

import static util.CompletionUtil.expectedCompletionId;
import static util.CompletionUtil.expectedCompletionTokens;
import static util.CompletionUtil.expectedFinishReason;
import static util.CompletionUtil.expectedGenerationMessage;
import static util.CompletionUtil.expectedMaxTokens;
import static util.CompletionUtil.expectedPromptTokens;
import static util.CompletionUtil.expectedRequestModelId;
import static util.CompletionUtil.expectedResponseModelId;
import static util.CompletionUtil.expectedTemp;
import static util.CompletionUtil.expectedTotalTokens;

public class MockChatModel implements ChatModel {
    @Override
    public ChatResponse call(Prompt prompt) {
        return buildMockChatResponse();
    }

    @Override
    public String call(String message) {
        return ChatModel.super.call(message);
    }

    @Override
    public String call(Message... messages) {
        return ChatModel.super.call(messages);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder().model(expectedRequestModelId).maxTokens(expectedMaxTokens).temperature(expectedTemp).build();
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(buildMockChatResponse());
    }

    @Override
    public Flux<String> stream(String message) {
        return ChatModel.super.stream(message);
    }

    @Override
    public Flux<String> stream(Message... messages) {
        return ChatModel.super.stream(messages);
    }

    private ChatResponse buildMockChatResponse() {
        List<Generation> generations = new ArrayList<>();

        Generation generation = new Generation(new AssistantMessage(expectedGenerationMessage),
                ChatGenerationMetadata.builder().finishReason(expectedFinishReason).build());
        generations.add(generation);

        DefaultUsage defaultUsage = new DefaultUsage(expectedPromptTokens, expectedCompletionTokens, expectedTotalTokens);

        ChatResponseMetadata.Builder chatResponseMetadataBuilder = ChatResponseMetadata.builder();
        chatResponseMetadataBuilder.model(expectedResponseModelId).usage(defaultUsage).id(expectedCompletionId);
        return ChatResponse.builder().metadata(chatResponseMetadataBuilder.build()).generations(generations).build();
    }
}
