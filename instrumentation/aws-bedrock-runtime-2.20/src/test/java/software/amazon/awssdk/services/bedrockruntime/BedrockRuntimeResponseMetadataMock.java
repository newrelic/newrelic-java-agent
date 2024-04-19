/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.bedrockruntime;

import software.amazon.awssdk.awscore.AwsResponseMetadata;

import java.util.Map;

public class BedrockRuntimeResponseMetadataMock extends AwsResponseMetadata {
    protected BedrockRuntimeResponseMetadataMock(Map<String, String> metadata) {
        super(metadata);
    }
}
