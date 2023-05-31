/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile.v2;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.newrelic.agent.util.StringMap;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public class Murmur3StringMap implements StringMap {
    private final HashFunction hashFunction = Hashing.murmur3_128(394852370);
    private final Charset charSet = StandardCharsets.UTF_8;

    private final Map<Object, String> stringMap = new ConcurrentHashMap<>();

    @Override
    public Map<Object, String> getStringMap() {
        return stringMap;
    }

    @Override
    public Object addString(String string) {
        if (null == string) {
            return null;
        }

        String key;
        if (string.length() < 12) {
            key = string;
        } else {
            HashCode hash = hashFunction.newHasher().putString(string, charSet).hash();
            final byte[] eightBytes = new byte[8];
            hash.writeBytesTo(eightBytes, 0, 8);
            key = new String(Base64.getEncoder().encode(eightBytes), StandardCharsets.US_ASCII);
        }

        stringMap.put(key, string);

        return key;
    }
}
