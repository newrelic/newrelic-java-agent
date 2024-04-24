/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelWithResponseStreamResponseHandler;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class BedrockRuntimeAsyncClientMock implements BedrockRuntimeAsyncClient {

    // Embedding
    public static final String embeddingModelId = "amazon.titan-embed-text-v1";
    public static final String embeddingResponseBody = "{\"embedding\":[0.328125,0.44335938],\"inputTextTokenCount\":8}";
    public static final String embeddingRequestInput = "What is the color of the sky?";

    // Completion
    public static final String completionModelId = "amazon.titan-text-lite-v1";
    public static final String completionResponseBody = "{\"inputTextTokenCount\":8,\"results\":[{\"tokenCount\":9,\"outputText\":\"\\nThe color of the sky is blue.\",\"completionReason\":\"FINISH\"}]}";
    public static final String completionRequestInput = "What is the color of the sky?";
    public static final String completionResponseContent = "\nThe color of the sky is blue.";
    public static final String finishReason = "FINISH";

    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<InvokeModelResponse> invokeModel(InvokeModelRequest invokeModelRequest) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("AWS_REQUEST_ID", "9d32a71a-e285-4b14-a23d-4f7d67b50ac3");
        AwsResponseMetadata awsResponseMetadata = new BedrockRuntimeResponseMetadataMock(metadata);
        SdkHttpFullResponse sdkHttpFullResponse;
        SdkResponse sdkResponse = null;

        boolean isError = invokeModelRequest.body().asUtf8String().contains("\"errorTest\":true");

        if (invokeModelRequest.modelId().equals(completionModelId)) {
            // This case will mock out a chat completion request/response
            if (isError) {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(400).statusText("BAD_REQUEST").build();
            } else {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(200).statusText("OK").build();
            }

            sdkResponse = InvokeModelResponse.builder()
                    .body(SdkBytes.fromUtf8String(completionResponseBody))
                    .contentType("application/json")
                    .responseMetadata(awsResponseMetadata)
                    .sdkHttpResponse(sdkHttpFullResponse)
                    .build();
        } else if (invokeModelRequest.modelId().equals(embeddingModelId)) {
            // This case will mock out an embedding request/response
            if (isError) {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(400).statusText("BAD_REQUEST").build();
            } else {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(200).statusText("OK").build();
            }

            sdkResponse = InvokeModelResponse.builder()
                    .body(SdkBytes.fromUtf8String(embeddingResponseBody))
                    .contentType("application/json")
                    .responseMetadata(awsResponseMetadata)
                    .sdkHttpResponse(sdkHttpFullResponse)
                    .build();
        }
        return CompletableFuture.completedFuture((InvokeModelResponse) sdkResponse);
    }

    @Override
    public CompletableFuture<Void> invokeModelWithResponseStream(InvokeModelWithResponseStreamRequest invokeModelWithResponseStreamRequest,
            InvokeModelWithResponseStreamResponseHandler asyncResponseHandler) {
        return BedrockRuntimeAsyncClient.super.invokeModelWithResponseStream(invokeModelWithResponseStreamRequest, asyncResponseHandler);
        // Streaming not currently supported
    }

    @Override
    public CompletableFuture<Void> invokeModelWithResponseStream(Consumer<InvokeModelWithResponseStreamRequest.Builder> invokeModelWithResponseStreamRequest,
            InvokeModelWithResponseStreamResponseHandler asyncResponseHandler) {
        return BedrockRuntimeAsyncClient.super.invokeModelWithResponseStream(invokeModelWithResponseStreamRequest, asyncResponseHandler);
        // Streaming not currently supported
    }

    @Override
    public BedrockRuntimeServiceClientConfiguration serviceClientConfiguration() {
        return null;
    }
}
