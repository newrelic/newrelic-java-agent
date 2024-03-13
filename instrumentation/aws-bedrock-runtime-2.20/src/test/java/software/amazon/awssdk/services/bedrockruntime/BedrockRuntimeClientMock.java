/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.bedrockruntime.model.AccessDeniedException;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InternalServerException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ModelErrorException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelNotReadyException;
import software.amazon.awssdk.services.bedrockruntime.model.ModelTimeoutException;
import software.amazon.awssdk.services.bedrockruntime.model.ResourceNotFoundException;
import software.amazon.awssdk.services.bedrockruntime.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.bedrockruntime.model.ThrottlingException;
import software.amazon.awssdk.services.bedrockruntime.model.ValidationException;

import java.util.HashMap;
import java.util.function.Consumer;

public class BedrockRuntimeClientMock implements BedrockRuntimeClient {
    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public InvokeModelResponse invokeModel(InvokeModelRequest invokeModelRequest)
            throws AccessDeniedException, ResourceNotFoundException, ThrottlingException, ModelTimeoutException, InternalServerException, ValidationException,
            ModelNotReadyException, ServiceQuotaExceededException, ModelErrorException, AwsServiceException, SdkClientException, BedrockRuntimeException {

        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("AWS_REQUEST_ID", "9d32a71a-e285-4b14-a23d-4f7d67b50ac3");
        AwsResponseMetadata awsResponseMetadata = new BedrockRuntimeResponseMetadataMock(metadata);
        SdkHttpFullResponse sdkHttpFullResponse;
        SdkResponse sdkResponse = null;

        boolean isError = invokeModelRequest.body().asUtf8String().contains("\"errorTest\":true");

        if (invokeModelRequest.modelId().equals("anthropic.claude-v2")) {
            // This case will mock out a chat completion request/response
            if (isError) {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(400).statusText("BAD_REQUEST").build();
            } else {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(200).statusText("OK").build();
            }

            sdkResponse = InvokeModelResponse.builder()
                    .body(SdkBytes.fromUtf8String(
                            "{\"completion\":\" The sky appears blue during the day because of how sunlight interacts with the gases in Earth's atmosphere. The main gases in our atmosphere are nitrogen and oxygen. These gases are transparent to visible light wavelengths, but they scatter shorter wavelengths more, specifically blue light. This scattering makes the sky look blue from the ground.\",\"stop_reason\":\"stop_sequence\",\"stop\":\"\\n\\nHuman:\"}"))
                    .contentType("application/json")
                    .responseMetadata(awsResponseMetadata)
                    .sdkHttpResponse(sdkHttpFullResponse)
                    .build();
        } else if (invokeModelRequest.modelId().equals("amazon.titan-embed-text-v1")) {
            // This case will mock out an embedding request/response
            if (isError) {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(400).statusText("BAD_REQUEST").build();
            } else {
                sdkHttpFullResponse = SdkHttpResponse.builder().statusCode(200).statusText("OK").build();
            }

            sdkResponse = InvokeModelResponse.builder()
                    .body(SdkBytes.fromUtf8String("{\"embedding\":[0.328125,0.44335938],\"inputTextTokenCount\":8}"))
                    .contentType("application/json")
                    .responseMetadata(awsResponseMetadata)
                    .sdkHttpResponse(sdkHttpFullResponse)
                    .build();
        }
        return (InvokeModelResponse) sdkResponse;
    }

    @Override
    public InvokeModelResponse invokeModel(Consumer<InvokeModelRequest.Builder> invokeModelRequest)
            throws AccessDeniedException, ResourceNotFoundException, ThrottlingException, ModelTimeoutException, InternalServerException, ValidationException,
            ModelNotReadyException, ServiceQuotaExceededException, ModelErrorException, AwsServiceException, SdkClientException, BedrockRuntimeException {
        return null;
    }

    @Override
    public BedrockRuntimeServiceClientConfiguration serviceClientConfiguration() {
        return null;
    }
}
