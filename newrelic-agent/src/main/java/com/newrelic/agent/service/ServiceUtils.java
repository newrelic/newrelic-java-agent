/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;

public class ServiceUtils {
    private static final int ROTATED_BIT_SHIFT = 31;
    private static final String PATH_HASH_SEPARATOR = ";";
    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static final MessageDigest cloneableMd5Digest = initializeMd5Digest();

    public static int calculatePathHash(String appName, String txName, Integer optionalReferringPathHash) {
        int referringPathHash = optionalReferringPathHash == null ? 0 : optionalReferringPathHash;
        int rotatedReferringPathHash = (referringPathHash << 1) | (referringPathHash >>> ROTATED_BIT_SHIFT);
        return (rotatedReferringPathHash ^ getHash(appName, txName));
    }

    public static int reversePathHash(String appName, String txName, Integer optionalReferringPathHash) {
        int referringPathHash = optionalReferringPathHash == null ? 0 : optionalReferringPathHash;
        int rotatedReferringPathHash = referringPathHash ^ getHash(appName, txName);
        return ((rotatedReferringPathHash >>> 1) | (rotatedReferringPathHash << ROTATED_BIT_SHIFT));
    }

    private static int getHash(String appName, String txName) {
        try {
            if (cloneableMd5Digest == null) {
                // This means it is not cloneable or no md5 algorithm exists
                return 0;
            }

            MessageDigest md = (MessageDigest) cloneableMd5Digest.clone();
            byte[] digest = md.digest((appName + PATH_HASH_SEPARATOR + txName).getBytes(StandardCharsets.UTF_8));

            // Take the right most 4 bytes
            int fromBytes =
                    (digest[12] & 0xFF) << 24 | (digest[13] & 0xFF) << 16 | (digest[14] & 0xFF) << 8 | (digest[15] & 0xFF);
            return fromBytes;
        } catch (CloneNotSupportedException e) {
            // This is already handled during initialization so it shouldn't happen
            return 0;
        }
    }

    public static String intToHexString(int val) {
        // properly handles leading 0's
        return String.format("%08x", val);
    }

    public static int hexStringToInt(String val) {
        // Parsing as long to avoid NumberFormatException for negative int's
        return (int) Long.parseLong(val, 16);
    }

    public static String md5HashValueFor(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        byte[] hashBytes;
        try {
            if (cloneableMd5Digest != null) {
                MessageDigest md = (MessageDigest) cloneableMd5Digest.clone();
                hashBytes = md.digest(str.getBytes(StandardCharsets.UTF_8));
            } else {
                return "";
            }
        } catch (Exception ignored) {
            return "";
        }
        return hexStringFromDigestBytes(hashBytes);
    }

    /**
     * Call before reading from shared variable.
     * 
     * When reading and writing to shared non-volatiles, such as an array, it is necessary to force a memory barrier so
     * the changes are visible to other threads. This method reads the {@link AtomicInteger} value which is implemented
     * as a volatile. This triggers a read barrier. Additional logic is added to ensure the JVM can't optimize out this
     * read.
     * 
     * @param i
     */
    public static void readMemoryBarrier(AtomicInteger i) {
        if (i.get() == -1) {
            // This will happen rarely enough that it shouldn't be an issue.
            i.set(0);
        }
    }

    private static String hexStringFromDigestBytes(byte[] digestBytes) {
        char[] hexChars = new char[digestBytes.length * 2];
        for (int i = 0; i < digestBytes.length; i++) {
            int v = digestBytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Call after writing to shared variable.
     * 
     * When reading and writing to shared non-volatiles, such as an array, it is necessary to force a memory barrier so
     * the changes are visible to other threads. This method increments the {@link AtomicInteger} value which is
     * implemented as a volatile. This triggers a write barrier.
     * 
     * @param i
     */
    public static void writeMemoryBarrier(AtomicInteger i) {
        i.incrementAndGet();
    }

    private static MessageDigest initializeMd5Digest() {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // Test for "cloneability", this will throw an exception if it can't be cloned
            return (MessageDigest) messageDigest.clone();
        } catch (NoSuchAlgorithmException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, e, "No algorithm found for MD5 Digest");
            return null;
        } catch (CloneNotSupportedException e) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Clone not supported for MD5 Digest");
            return null;
        } catch (Throwable e) {
            AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to initialize MD5 Digest");
            return null;
        }
    }
}
