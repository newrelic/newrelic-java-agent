/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static com.newrelic.weave.violation.WeaveViolationType.METHOD_SYNTHETIC_WEAVE_ILLEGAL;

import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.violation.WeaveViolation;

/**
 * SyntheticMatchTest.java
 */
public class SyntheticMatchTest {

    /**
     * Test field-related mismatches.
     */
    @Test
    public void testSyntheticMatch() throws IOException {
        String internalName = Type.getInternalName(Weave.class);
        WeaveViolation[] expected = { new WeaveViolation(METHOD_SYNTHETIC_WEAVE_ILLEGAL, internalName, new Method(
                "access$002", "()Ljava/lang/String;")), };
        WeaveTestUtils.expectViolations(Original.class, Weave.class, false, expected);
    }

    public static class Original {
        private static String privateField = "I am private don't access me!!";

        public void methodThatUsesInnerClassToAccessPrivateField() {
            new Runnable() {
                @Override
                public void run() {
                    privateField = "I accessed the private field!";
                }
            }.run();
        }
    }

    public static class Weave {
        public String access$002() {
            return Weaver.callOriginal();
        }
    }
}
