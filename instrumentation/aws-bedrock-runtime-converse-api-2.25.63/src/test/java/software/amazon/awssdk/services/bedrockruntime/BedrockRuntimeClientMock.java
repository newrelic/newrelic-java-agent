/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;

import static software.amazon.awssdk.services.bedrockruntime.MockConverseResponse.sdkResponse;

public class BedrockRuntimeClientMock implements BedrockRuntimeClient {
    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public ConverseResponse converse(ConverseRequest converseRequest) {
        // Hijacking additionalModelRequestFields with a BooleanDocument to signal an error condition
        boolean isError = converseRequest.additionalModelRequestFields().asBoolean();
        return (ConverseResponse) sdkResponse(isError);
    }
}
