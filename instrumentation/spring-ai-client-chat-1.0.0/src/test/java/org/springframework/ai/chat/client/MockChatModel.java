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

public class MockChatModel implements ChatModel {
    public static String requestModelId = "gpt-4o-request";
    public static String responseModelId = "gpt-4o-response";
    public static String completionId = "chatcmpl-DQiVf9ymJ30wEL1PROnY8JKZHA3Ku";
    public static String generationMessage = "Why don't scientists trust atoms? Because they make up everything!";
    public static String finishReason = "stop";
    public static double temp = 0.7;
    public static int maxTokens = 1000;
    public static int promptTokens = 1;
    public static int completionTokens = 2;
    public static int totalTokens = 3;

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
        return ChatOptions.builder().model(requestModelId).maxTokens(maxTokens).temperature(temp).build();
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

        Generation generation = new Generation(new AssistantMessage(generationMessage),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        generations.add(generation);

        DefaultUsage defaultUsage = new DefaultUsage(promptTokens, completionTokens, totalTokens);

        ChatResponseMetadata.Builder chatResponseMetadataBuilder = ChatResponseMetadata.builder();
        chatResponseMetadataBuilder.model(responseModelId).usage(defaultUsage).id(completionId);
        return ChatResponse.builder().metadata(chatResponseMetadataBuilder.build()).generations(generations).build();
    }
}
