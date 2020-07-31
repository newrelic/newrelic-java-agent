/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.beans;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the method matcher utility class.
 * 
 * @since Sep 20, 2012
 */
public class MethodMatcherUtilityTest {

    /**
     * Tests various types of parameters.
     */
    @Test
    public void testParamNamesToParamDescriptor() {

        try {
            // javap output
            // public java.lang.String testMethod(int, boolean, float, byte);
            // Signature: (IZFB)Ljava/lang/String;
            List<String> inputs = new ArrayList<>();
            inputs.add("int");
            inputs.add("boolean");
            inputs.add("float  ");
            inputs.add(" byte");
            Assert.assertEquals("(IZFB)", MethodConverterUtility.paramNamesToParamDescriptor(inputs));

            inputs.clear();
            inputs.add("long");
            inputs.add("java.lang.String");
            inputs.add("com.newrelic.javax.Extension");
            inputs.add(" com.newrelic.javax.Pointcut");
            Assert.assertEquals("(JLjava/lang/String;Lcom/newrelic/javax/Extension;Lcom/newrelic/javax/Pointcut;)",
                    MethodConverterUtility.paramNamesToParamDescriptor(inputs));

            inputs.clear();
            inputs.add("char");
            inputs.add("double");
            Assert.assertEquals("(CD)", MethodConverterUtility.paramNamesToParamDescriptor(inputs));

            // javap output
            // public void testing(short, int[], java.lang.String[],
            // test.something.NewClass[][])
            // (S[I[Ljava/lang/String;[[Ltest/something/NewClass;)V
            inputs.clear();
            inputs.add("short");
            inputs.add("int[]");
            inputs.add("java.lang.String[]");
            inputs.add("test.something.NewClass[][]");
            Assert.assertEquals("(S[I[Ljava/lang/String;[[Ltest/something/NewClass;)",
                    MethodConverterUtility.paramNamesToParamDescriptor(inputs));

            // javap
            // ([[[[DLjava/lang/StringBuilder;[[Ljava/util/List;)V
            inputs.clear();
            inputs.add("double[][][][]");
            inputs.add("java.lang.StringBuilder");
            inputs.add("java.util.List<String>");
            Assert.assertEquals("([[[[DLjava/lang/StringBuilder;Ljava/util/List;)",
                    MethodConverterUtility.paramNamesToParamDescriptor(inputs));

            // ([[[Ljava/util/List;Ljava/util/Map;)V
            inputs.clear();
            inputs.add("java.util.List<String>[][][]");
            inputs.add("java.util.Map<Integer, String>");
            Assert.assertEquals("([[[Ljava/util/List;Ljava/util/Map;)",
                    MethodConverterUtility.paramNamesToParamDescriptor(inputs));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testParamNamesToParamDescriptorNull() {
        try {
            List<String> inputs = new ArrayList<>();
            Assert.assertEquals("()", MethodConverterUtility.paramNamesToParamDescriptor(inputs));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testParamNamesToParamDescriptorException() {
        List<String> inputs = new ArrayList<>();
        try {
            inputs.add("asdfjwpeoiclksjdf[asdf");
            MethodConverterUtility.paramNamesToParamDescriptor(inputs);
            Assert.fail("The test should have thrown an exception.");
        } catch (Exception e) {
            // we should go into here
        }
    }

    @Test
    public void testGetDescriptorBasic() {
        try {
            List<String> inputs = new ArrayList<>();
            inputs.add("int");

            Assert.assertEquals("(I)", MethodConverterUtility.paramNamesToParamDescriptor(inputs));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetDescriptorNone() {
        try {
            List<String> pts = new ArrayList<>();
            Assert.assertEquals("()", MethodConverterUtility.paramNamesToParamDescriptor(pts));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetDescriptorMultiple() {
        try {
            List<String> pts = new ArrayList<>();
            pts.add("java.lang.String");
            pts.add("int");
            pts.add("java.util.Map<java.lang.String,java.lang.String>");

            Assert.assertEquals("(Ljava/lang/String;ILjava/util/Map;)",
                    MethodConverterUtility.paramNamesToParamDescriptor(pts));

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
