/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDelta;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlockDeltaEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamMetadataEvent;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStartEvent;
import software.amazon.awssdk.services.bedrockruntime.model.MessageStopEvent;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static llm.converse.models.TestUtil.AWS_REQUEST_ID;
import static llm.converse.models.TestUtil.ERROR_STATUS_CODE;
import static llm.converse.models.TestUtil.ERROR_STATUS_TEXT;
import static llm.converse.models.TestUtil.INPUT_TOKENS;
import static llm.converse.models.TestUtil.OUTPUT_TOKENS;
import static llm.converse.models.TestUtil.RESPONSE_CONTENT_TEXT;
import static llm.converse.models.TestUtil.RESPONSE_ROLE;
import static llm.converse.models.TestUtil.STOP_REASON;
import static llm.converse.models.TestUtil.SUCCESS_STATUS_CODE;
import static llm.converse.models.TestUtil.SUCCESS_STATUS_TEXT;
import static llm.converse.models.TestUtil.TOTAL_TOKENS;

public class MockConverseResponse {
    public static SdkResponse sdkResponse(boolean isError) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("AWS_REQUEST_ID", AWS_REQUEST_ID);
        AwsResponseMetadata awsResponseMetadata = new BedrockRuntimeResponseMetadataMock(metadata);

        SdkHttpFullResponse sdkHttpFullResponse;
        if (isError) {
            sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(ERROR_STATUS_CODE).statusText(ERROR_STATUS_TEXT).build();
        } else {
            sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(SUCCESS_STATUS_CODE).statusText(SUCCESS_STATUS_TEXT).build();
        }
        ContentBlock contentBlock = ContentBlock.builder().text(RESPONSE_CONTENT_TEXT).build();
        Message message = Message.builder().role(RESPONSE_ROLE).content(contentBlock).build();

        ConverseOutput converseOutput = ConverseOutput.builder().message(message).build();
        TokenUsage tokenUsage = TokenUsage.builder().inputTokens(INPUT_TOKENS).outputTokens(OUTPUT_TOKENS).totalTokens(TOTAL_TOKENS).build();

        return ConverseResponse.builder()
                .stopReason(STOP_REASON)
                .output(converseOutput)
                .usage(tokenUsage)
                .responseMetadata(awsResponseMetadata)
                .sdkHttpResponse(sdkHttpFullResponse)
                .build();
    }

    /**
     * Builds the initial response delivered to a ConverseStreamResponseHandler via responseReceived,
     * before any stream events arrive.
     */
    public static ConverseStreamResponse converseStreamResponse() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("AWS_REQUEST_ID", AWS_REQUEST_ID);
        AwsResponseMetadata awsResponseMetadata = new BedrockRuntimeResponseMetadataMock(metadata);

        SdkHttpFullResponse sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(SUCCESS_STATUS_CODE).statusText(SUCCESS_STATUS_TEXT).build();

        return (ConverseStreamResponse) ConverseStreamResponse.builder()
                .responseMetadata(awsResponseMetadata)
                .sdkHttpResponse(sdkHttpFullResponse)
                .build();
    }

    /**
     * Builds the sequence of ConverseStreamOutput events a successful ConverseStream call would
     * deliver via onEventStream: a message start, a single text delta, a message stop with the
     * finish reason, and a metadata event carrying token usage.
     */
    public static List<ConverseStreamOutput> converseStreamEvents() {
        List<ConverseStreamOutput> events = new ArrayList<>();
        events.add(MessageStartEvent.builder().role(RESPONSE_ROLE).build());
        events.add(ContentBlockDeltaEvent.builder()
                .contentBlockIndex(0)
                .delta(ContentBlockDelta.builder().text(RESPONSE_CONTENT_TEXT).build())
                .build());
        events.add(MessageStopEvent.builder().stopReason(STOP_REASON).build());
        events.add(ConverseStreamMetadataEvent.builder()
                .usage(TokenUsage.builder().inputTokens(INPUT_TOKENS).outputTokens(OUTPUT_TOKENS).totalTokens(TOTAL_TOKENS).build())
                .build());
        return events;
    }
}
