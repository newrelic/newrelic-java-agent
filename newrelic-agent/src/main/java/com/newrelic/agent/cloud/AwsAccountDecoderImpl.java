/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

import java.util.function.Function;

class AwsAccountDecoderImpl implements AwsAccountDecoder {
    private final Function<String, String> CACHE = AgentBridge.collectionFactory.createAccessTimeBasedCache(3600, 4, this::doDecodeAccount);

    public String decodeAccount(String accessKey) {
        return CACHE.apply(accessKey);
    }

    private String doDecodeAccount(String awsAccessKey) {
        if (awsAccessKey.length() < 16) {
            return null;
        }
        try {
            String accessKeyWithoutPrefix = awsAccessKey.substring(4).toLowerCase();
            long encodedAccount = base32Decode(accessKeyWithoutPrefix);
            // magic number
            final long mask = 140737488355200L;
            // magic incantation to find out the account
            long accountId = (encodedAccount & mask) >> 7;
            return Long.toString(accountId);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Character range is A-Z, 2-7. 'A' being 0 and '7', 31.
     * Characters outside of this range will be considered 0.
     * @param src the string to be decoded. Must be at least 10 characters.
     * @return a long containing first 6 bytes of the base 32 decoded data.
     * @throws ArrayIndexOutOfBoundsException if src has less than 10 characters
     */
    private long base32Decode(String src) {
        long base = 0;
        char[] chars = src.toCharArray();
        // each char is 5 bits, we need 48 bits
        for (int i = 0; i < 10; i++) {
            char c = chars[i];
            base <<= 5;
            if (c >= 'a' && c <= 'z') {
                base += c - 'a';
            } else if (c >= '2' && c <= '7') {
                base += c - '2' + 26;
            }
        }
        // 50 bits were read, dropping the lowest 2
        return base >> 2;
    }

    /**
     * Use {@link #newInstance()} to instantiate an AwsAccountDecoder.
     */
    private AwsAccountDecoderImpl() {
    }

    /**
     * This factory method will check the agent configuration and return an appropriate implementation of
     * AwsAccountDecoder.
     */
    static AwsAccountDecoder newInstance() {
        if (NewRelic.getAgent().getConfig().getValue("cloud.aws.account_decoding.enabled", true)) {
            NewRelic.getAgent().getMetricAggregator().incrementCounter("Supportability/Aws/AccountDecode/enabled");
            return new AwsAccountDecoderImpl();
        } else {
            // decoding is disabled, create a noop decoder
            NewRelic.getAgent().getMetricAggregator().incrementCounter("Supportability/Aws/AccountDecode/disabled");
            return (accessKey) -> null;
        }
    }
}
