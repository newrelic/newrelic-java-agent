/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseStreamResponseHandler;

import java.util.concurrent.CompletableFuture;

import static software.amazon.awssdk.services.bedrockruntime.MockConverseResponse.sdkResponse;

public class BedrockRuntimeAsyncClientMock implements BedrockRuntimeAsyncClient {
    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public CompletableFuture<ConverseResponse> converse(ConverseRequest converseRequest) {
        return CompletableFuture.completedFuture((ConverseResponse) sdkResponse(true));
    }

    @Override
    public CompletableFuture<Void> converseStream(ConverseStreamRequest converseStreamRequest, ConverseStreamResponseHandler asyncResponseHandler) {
        // TODO Stream support not implemented
        return CompletableFuture.completedFuture(null);
    }
}
