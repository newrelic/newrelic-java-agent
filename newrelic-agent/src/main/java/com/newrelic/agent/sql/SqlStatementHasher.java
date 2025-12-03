/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.sql;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SqlStatementHasher {
    public static String hashSqlStatement(String sql, MessageDigest messageDigest) {
        if (sql == null || sql.isEmpty()) {
            return "";
        }

        byte[] hashBytes = messageDigest.digest(sql.getBytes(StandardCharsets.UTF_8));
        return hexStringFromDigestBytes(hashBytes);
    }

    private static String hexStringFromDigestBytes(byte[] digestBytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : digestBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
