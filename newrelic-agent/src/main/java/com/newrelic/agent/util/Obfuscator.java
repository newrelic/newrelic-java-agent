/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * This class obfuscates and deobfuscates strings using a given key. It is used by the real user monitoring feature to
 * obfuscate transaction names inserted into web pages, and used in response headers to obfuscate the app data returned
 * to the calling agent.
 */
public class Obfuscator {

    private Obfuscator() {
    }

    /**
     * Obfuscates a name using the given key. Note that when the RUM feature calls this api it only sends the first 13
     * characters of the browser monitoring key, while the cross process callers expect the full key to be used.
     */
    public static String obfuscateNameUsingKey(String name, String key) {
        byte[] encodedBytes = name.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(encode(encodedBytes, keyBytes));
    }

    private static byte[] encode(byte[] bytes, byte[] keyBytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (bytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return bytes;
    }

    /**
     * Deobfuscates a name using the given key.
     *
     */
    public static String deobfuscateNameUsingKey(String name, String key) {
        byte[] bytes = Base64.getDecoder().decode(name);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        return new String(encode(bytes, keyBytes), StandardCharsets.UTF_8);
    }
}
