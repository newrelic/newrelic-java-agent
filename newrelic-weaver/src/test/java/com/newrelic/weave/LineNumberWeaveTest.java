/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.Sets;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * LineNumberWeaveTest.java
 */
public class LineNumberWeaveTest {

    @Test
    public void testRemoveWeaveLineNumbers() throws IOException {
        testLineNumbers(true);
    }

    @Test
    public void testPreserveWeaveLineNumbers() throws IOException {
        testLineNumbers(false);
    }

    private void testLineNumbers(boolean remove) throws IOException {
        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.LineNumberWeaveTest$Original", "com.newrelic.weave.LineNumberWeaveTest$Weave",
                "com.newrelic.weave.LineNumberWeaveTest$Original", true, Collections.<String>emptySet(),
                Collections.<String>emptySet(), WeaveTestUtils.getErrorHandler());

        Set<Integer> originalLineNumbers = findLineNumbers(WeaveTestUtils.readClass(
                "com.newrelic.weave.LineNumberWeaveTest$Original"));
        Set<Integer> weaveLineNumbers = findLineNumbers(WeaveTestUtils.readClass(
                "com.newrelic.weave.LineNumberWeaveTest$Weave"));
        Set<Integer> compositeLineNumbers = findLineNumbers(weave.getComposite());

        assertEquals("Hello, Bill from the weaver!", new Original().greet("Joe"));
        assertTrue(compositeLineNumbers.containsAll(originalLineNumbers));

        if(remove) {
            // make sure the composite only contains original line numbers
            Set<Integer> intersection = Sets.intersection(weaveLineNumbers, originalLineNumbers);
            assertEquals(0, intersection.size());
        } else {
            // some line numbers from the weave class may never make it to composite because they are not weaved
            // empty default constructors is one example
            // just make sure that line numbers that are NOT from the original come from the weave
            Set<Integer> difference = Sets.difference(compositeLineNumbers, originalLineNumbers);
            assertTrue(weaveLineNumbers.containsAll(difference));
        }
    }

    private Set<Integer> findLineNumbers(ClassNode classNode) {
        final Set<Integer> lineNumbers = new HashSet<>();
        classNode.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
                    @Override
                    public void visitLineNumber(int line, Label start) {
                        lineNumbers.add(line);
                    }
                };
            }
        });
        return lineNumbers;
    }

    public static class Original {
        public String greet(String text) {
            return "Hello, " + text;
        }
    }

    public static class Weave {
        public String greet(String text) {
            return Weaver.callOriginal() + " from the weaver!";
        }
    }
}
