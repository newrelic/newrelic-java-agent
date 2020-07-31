/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Assert;
import org.junit.Test;

public class ObfuscatorTest {

    private static final String KEY = "BLAHHHH";

    @Test
    public void testEncodeAndDecode() {
        String encoded = Obfuscator.obfuscateNameUsingKey("testString", KEY);
        Assert.assertEquals("NikyPBs8OisiJg==", encoded);

        String string = Obfuscator.deobfuscateNameUsingKey(encoded, KEY);
        Assert.assertEquals("testString", string);
    }
}
