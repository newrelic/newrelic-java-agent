/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.aws;

public class AwsUtil {
    private final static AwsAccountDecoder DECODER = AwsAccountDecoderImpl.newInstance();

    /**
     * Decode the account id from the given access key.
     * This method becomes a noop and always returns null if the config "aws.account_decoding.enabled" is set to false.
     */
    public static Long decodeAccount(String accessKey) {
        return DECODER.decodeAccount(accessKey);
    }

    private AwsUtil() {
        // prevent instantiation of utility class
    }
}
