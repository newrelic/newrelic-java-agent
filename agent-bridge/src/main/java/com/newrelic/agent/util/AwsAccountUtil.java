/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.function.Function;


public class AwsAccountUtil {
    private static AwsAccountUtil INSTANCE = AwsAccountUtil.create();

    private final Function<String, Long> CACHE = AgentBridge.collectionFactory.memoize(this::doDecodeAccount, 32);

    public static AwsAccountUtil get() {
        return INSTANCE;
    }

    public Long decodeAccount(String accessKey) {
        return CACHE.apply(accessKey);
    }

    private Long doDecodeAccount(String awsAccessKeyId) {
        String accessKeyWithoutPrefix = awsAccessKeyId.substring(4).toLowerCase();
        long encodedAccount = base32Decode(accessKeyWithoutPrefix);
        // magic number
        long mask = 140737488355200L;
        // magic incantation to find out the account
        return (encodedAccount & mask) >> 7;
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

    private AwsAccountUtil() {
        // prevent instantiation of utility class
    }

    private static AwsAccountUtil create() {
        return new AwsAccountUtil();
    }
}
