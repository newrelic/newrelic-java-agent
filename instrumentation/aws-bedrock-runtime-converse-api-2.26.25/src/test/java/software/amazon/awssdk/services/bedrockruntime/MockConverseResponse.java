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
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.TokenUsage;

import java.util.HashMap;

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
    public static SdkResponse sdkResponse(boolean success) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("AWS_REQUEST_ID", AWS_REQUEST_ID);
        AwsResponseMetadata awsResponseMetadata = new BedrockRuntimeResponseMetadataMock(metadata);

        SdkHttpFullResponse sdkHttpFullResponse;
        if (success) {
            sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(SUCCESS_STATUS_CODE).statusText(SUCCESS_STATUS_TEXT).build();
        } else {
            sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(ERROR_STATUS_CODE).statusText(ERROR_STATUS_TEXT).build();
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
}
