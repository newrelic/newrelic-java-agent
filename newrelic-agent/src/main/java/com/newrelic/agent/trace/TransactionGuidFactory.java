/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.trace;

import java.util.concurrent.ThreadLocalRandom;

public class TransactionGuidFactory {
    private static final char[] hexchars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private TransactionGuidFactory() {
    }

    public static String generate16CharGuid() {
        // Tests with JMH showed that this implementation is 11x faster than the previous implementation:
        // return new BigInteger(64, randomHolder.get()).toString(16)
        // and about 1.2x faster than the obvious alternative implementation:
        // return Long.toHexString(Math.abs(randomHolder.get().nextLong()))
        // In addition, this one returns 16 useful digits, while the obvious one returns slightly fewer.
        // Note that the digits are generated in "reverse order", which is perfectly fine here.
        long random = ThreadLocalRandom.current().nextLong();
        char[] result = new char[16];
        for (int i = 0; i < 16; ++i) {
            result[i] = hexchars[(int) (random & 0xF)];
            random >>= 4;
        }
        return new String(result);
    }

}
