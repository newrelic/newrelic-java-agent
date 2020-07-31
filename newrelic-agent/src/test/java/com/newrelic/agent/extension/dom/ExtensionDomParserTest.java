/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.dom;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.extension.util.XmlException;
import com.newrelic.agent.instrumentation.classmatchers.ChildClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactReturnTypeMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static com.newrelic.agent.AgentHelper.getFile;

public class ExtensionDomParserTest {
    /**
     * Test case one.
     */
    private static final String FILE_PATH_1 = "com/newrelic/agent/extension/util/test1.xml";

    private static final String METHOD_ANNOTATION_FILE_PATH = "com/newrelic/agent/extension/util/method_annotation.xml";
    private static final String PRIMITIVE_RETURN_TYPE_FILE_PATH = "com/newrelic/agent/extension/util/primitive_return_type.xml";
    private static final String SUPERCLASS_FILE_PATH = "com/newrelic/agent/extension/util/superclass.xml";
    private static final String INTERFACE_FILE_PATH = "com/newrelic/agent/extension/util/interface.xml";

    /**
     * Test case one.
     */
    private static final String FILE_PATH_1_NO_PREFIX = "com/newrelic/agent/extension/util/test1noprefix.xml";

    /**
     * Test case two.
     */
    private static final String FILE_PATH_2 = "com/newrelic/agent/extension/dom/allAtts.xml";

    /**
     * Test case three.
     */
    private static final String FILE_PATH_INST = "com/newrelic/agent/extension/dom/multipleInstrument.xml";

    /**
     * Test case four.
     */
    private static final String FILE_PATH_CLASS = "com/newrelic/agent/extension/dom/MultipleClass.xml";

    /**
     * Test file seven.
     */
    private static final String FILE_PATH_7_INTERFACE_CLASS = "com/newrelic/agent/command/commandLineExtension7.xml";

    /**
     * Test file nine.
     */
    private static final String FILE_PATH_9_INTERFACE_CLASS = "com/newrelic/agent/command/commandLineExtension9.xml";

    private static final String ENTITY_EXPANSION = "com/newrelic/agent/extension/dom/entity_expansion.xml";
    private static final String ENTITY_EXPANSION_2 = "com/newrelic/agent/extension/dom/entity_expansion_2.xml";
    private static final String ENTITY_EXPANSION_3 = "com/newrelic/agent/extension/dom/entity_expansion_3.xml";
    private static final String ENTITY_EXPANSION_4 = "com/newrelic/agent/extension/dom/entity_expansion_4.xml";

    @Test
    public void testParseWithBadTransformerFactorySetting() throws Exception {
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "Dude");
            ExtensionDomParser.readFile(getFile(FILE_PATH_1));
        } finally {
            System.clearProperty("javax.xml.transform.TransformerFactory");
        }
    }

    @Test
    public void testDomOnePointCut() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(FILE_PATH_1));
        // ext attributes
        Assert.assertEquals("test1", ext.getName());
        Assert.assertEquals(1.0, ext.getVersion(), .001);
        Assert.assertTrue(ext.isEnabled());

        Instrumentation inst = ext.getInstrumentation();
        Assert.assertEquals("PREFIX", inst.getMetricPrefix());

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertFalse(pc.isIgnoreTransaction());
        Assert.assertFalse(pc.isExcludeFromTransactionTrace());
        Assert.assertFalse(pc.isLeaf());
        Assert.assertNull(pc.getMetricNameFormat());
        Assert.assertEquals("test.CustomExampleTest", pc.getClassName().getValue());
        List<Method> methods = pc.getMethod();
        Assert.assertEquals(2, methods.size());
        Assert.assertEquals("run", methods.get(0).getName());
        Assert.assertEquals("finish", methods.get(1).getName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)",
                MethodParameters.getDescriptor(methods.get(0).getParameters()));
        Assert.assertEquals("(F)", MethodParameters.getDescriptor(methods.get(1).getParameters()));

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(B)V",
                com.google.common.collect.ImmutableSet.<String>of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Assert.assertEquals("test/CustomExampleTest", actual.getClassMatcher().getClassNames().toArray()[0]);
    }

    @Test
    public void testSuperclassPointCut() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(SUPERCLASS_FILE_PATH));

        Instrumentation inst = ext.getInstrumentation();

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(2, thePcs.size());
        Pointcut pc = thePcs.get(0);

        Assert.assertNotNull(pc.getClassName());
        Assert.assertEquals("test.SuperTest", pc.getClassName().getValue());

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(2, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        Assert.assertTrue(actual.getClassMatcher() instanceof ChildClassMatcher);

        ExtensionClassAndMethodMatcher returnMatcher = pcs.get(1);

        Assert.assertTrue(returnMatcher.getClassMatcher() instanceof ChildClassMatcher);
        Assert.assertTrue(returnMatcher.getMethodMatcher() instanceof ExactReturnTypeMethodMatcher);

        Assert.assertTrue(returnMatcher.getMethodMatcher().matches(Opcodes.ACC_PUBLIC, "bogus",
                "()Lcom/framework/Result;", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(returnMatcher.getMethodMatcher().matches(Opcodes.ACC_PUBLIC, "test",
                "()[Lcom/framework/Result;", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(returnMatcher.getMethodMatcher().matches(Opcodes.ACC_PUBLIC, "dude",
                "(Lcom/framework/Result;)V", com.google.common.collect.ImmutableSet.<String>of()));
    }

    @Test
    public void testPrimitiveReturnType() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(PRIMITIVE_RETURN_TYPE_FILE_PATH));

        Instrumentation inst = ext.getInstrumentation();

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);

        Assert.assertNull(pc.getClassName());
        Assert.assertEquals("com.company.SomeInterface", pc.getInterfaceName());
        Assert.assertEquals(1, pc.getMethod().size());
        Assert.assertEquals("boolean", pc.getMethod().iterator().next().getReturnType());

        try {
            ExtensionConversionUtility.convertToPointCutsForValidation(ext);
            Assert.fail();
        } catch (XmlException ex) {
            Assert.assertEquals("The return type 'boolean' is not valid.  Primitive types are not allowed.", ex.getMessage());
        }
    }

    @Test
    public void testMethodAnnotation() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(METHOD_ANNOTATION_FILE_PATH));

        Instrumentation inst = ext.getInstrumentation();

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);

        Assert.assertNull(pc.getClassName());
        Assert.assertNull(pc.getInterfaceName());
        Assert.assertNotNull(pc.getMethodAnnotation());
        Assert.assertEquals("javax.ws.rs.DELETE", pc.getMethodAnnotation());

        ExtensionConversionUtility.convertToPointCutsForValidation(ext);
    }

    @Test
    public void testInterfacePointCut() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(INTERFACE_FILE_PATH));

        Instrumentation inst = ext.getInstrumentation();

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);

        Assert.assertNull(pc.getClassName());
        Assert.assertEquals("javax.servlet.Filter", pc.getInterfaceName());
        // Assert.assertEquals(ClassType.INTERFACE_NAME, pc.getClassType());

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        Assert.assertTrue(actual.getClassMatcher() instanceof InterfaceMatcher);
    }

    @Test
    public void testDomOnePointCutNoPrefix() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(FILE_PATH_1_NO_PREFIX));
        // ext attributes
        Assert.assertEquals("test1", ext.getName());
        Assert.assertEquals(1.0, ext.getVersion(), .001);
        Assert.assertTrue(ext.isEnabled());

        Instrumentation inst = ext.getInstrumentation();
        Assert.assertEquals("PREFIX", inst.getMetricPrefix());

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertFalse(pc.isIgnoreTransaction());
        Assert.assertFalse(pc.isExcludeFromTransactionTrace());
        Assert.assertFalse(pc.isLeaf());
        Assert.assertNull(pc.getMetricNameFormat());
        Assert.assertEquals("test.CustomExampleTest", pc.getClassName().getValue());
        List<Method> methods = pc.getMethod();
        Assert.assertEquals(2, methods.size());
        Assert.assertEquals("run", methods.get(0).getName());
        Assert.assertEquals("finish", methods.get(1).getName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)", MethodParameters.getDescriptor(methods.get(0).getParameters()));
        Assert.assertEquals("(F)", MethodParameters.getDescriptor(methods.get(1).getParameters()));

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(B)V",
                com.google.common.collect.ImmutableSet.<String>of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Assert.assertEquals("test/CustomExampleTest", actual.getClassMatcher().getClassNames().toArray()[0]);
    }

    @Test
    public void testDomAllAtts() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(FILE_PATH_2));
        // ext attributes
        Assert.assertEquals("test2", ext.getName());
        Assert.assertEquals(2.2, ext.getVersion(), .001);
        Assert.assertFalse(ext.isEnabled());

        Instrumentation inst = ext.getInstrumentation();
        Assert.assertEquals("special", inst.getMetricPrefix());

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(2, thePcs.size());
        Pointcut pc = thePcs.get(0);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertFalse(pc.isIgnoreTransaction());
        Assert.assertTrue(pc.isExcludeFromTransactionTrace());
        Assert.assertTrue(pc.isLeaf());
        Assert.assertEquals("/Hello", pc.getMetricNameFormat());
        Assert.assertEquals("test.CustomExampleTest$1", ExtensionConversionUtility.getClassName(pc));
        List<Method> methods = pc.getMethod();
        Assert.assertEquals(2, methods.size());
        Assert.assertEquals("run", methods.get(0).getName());
        Assert.assertEquals("finish", methods.get(1).getName());
        Assert.assertEquals("(Ljava/lang/String;[Ljava/lang/String;)", MethodParameters.getDescriptor(methods.get(0).getParameters()));
        Assert.assertEquals("()", MethodParameters.getDescriptor(methods.get(1).getParameters()));

        Pointcut pc1 = thePcs.get(1);
        Assert.assertFalse(pc1.isTransactionStartPoint());
        Assert.assertTrue(pc1.isIgnoreTransaction());
        Assert.assertFalse(pc1.isExcludeFromTransactionTrace());
        Assert.assertEquals("/Yikes/End", pc1.getMetricNameFormat());
        Assert.assertEquals("com.sample.Validator", pc1.getClassName().getValue());
        methods = pc1.getMethod();
        Assert.assertEquals(2, methods.size());
        Assert.assertEquals("runner", methods.get(0).getName());
        Assert.assertEquals("startup", methods.get(1).getName());
        Assert.assertEquals("(ILjava/util/List;)", MethodParameters.getDescriptor(methods.get(0).getParameters()));
        Assert.assertEquals("()", MethodParameters.getDescriptor(methods.get(1).getParameters()));
    }

    @Test
    public void testDomMultInst() throws Exception {
        try {
            ExtensionDomParser.readFile(getFile(FILE_PATH_INST));
            Assert.fail("A runtime exception should have been thrown.");
        } catch (Exception e) {
            // should go into here
        }

        Assert.assertNull(ExtensionDomParser.readFileCatchException(getFile(FILE_PATH_INST)));
    }

    @Test(expected = SAXParseException.class)
    public void testEntityExpansionAttack() throws Exception {
        try {
            ExtensionDomParser.readFile(getFile(ENTITY_EXPANSION));
            Assert.fail("An exception should have been thrown");
        } catch (Exception e) {
            Assert.assertTrue("Actual message: " + e.getMessage(), e.getMessage().contains("DOCTYPE is disallowed"));
            throw e;
        }
    }

    @Test(expected = SAXParseException.class)
    public void testEntityExpansionAttack2() throws Exception {
        try {
            ExtensionDomParser.readFile(getFile(ENTITY_EXPANSION_2));
            Assert.fail("An exception should have been thrown");
        } catch (Exception e) {
            Assert.assertTrue("Actual message: " + e.getMessage(), e.getMessage().contains("DOCTYPE is disallowed"));
            throw e;
        }
    }

    @Test(expected = SAXParseException.class)
    public void testEntityExpansionAttack3() throws Exception {
        try {
            ExtensionDomParser.readFile(getFile(ENTITY_EXPANSION_3));
            Assert.fail("An exception should have been thrown");
        } catch (Exception e) {
            Assert.assertTrue("Actual message: " + e.getMessage(), e.getMessage().contains("DOCTYPE is disallowed"));
            throw e;
        }
    }

    @Test(expected = SAXParseException.class)
    public void testEntityExpansionAttack4() throws Exception {
        try {
            System.out.println("Running test!!!");
            ExtensionDomParser.readFile(getFile(ENTITY_EXPANSION_4));
            System.out.println("Ending test!!!");
            Assert.fail("An exception should have been thrown");
        } catch (Exception e) {
            System.out.println("Ending test!!! " + e.getClass().getName());
            Assert.assertTrue("Actual message: " + e.getMessage(), e.getMessage().contains("DOCTYPE is disallowed"));
            throw e;
        }
    }

    @Test(expected = SAXParseException.class)
    public void testDomMultClass() throws Exception {
        ExtensionDomParser.readFile(getFile(FILE_PATH_CLASS));
    }

    @Test
    public void testDomOnePointCutWithReadString() throws Exception {
        File file = getFile(FILE_PATH_1);
        Extension ext = ExtensionDomParser.readStringCatchException(readInFile(file));
        Assert.assertNotNull(ext);
        // ext attributes
        Assert.assertEquals("test1", ext.getName());
        Assert.assertEquals(1.0, ext.getVersion(), .001);
        Assert.assertTrue(ext.isEnabled());

        Instrumentation inst = ext.getInstrumentation();
        Assert.assertEquals("PREFIX", inst.getMetricPrefix());

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(1, thePcs.size());
        Pointcut pc = thePcs.get(0);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertFalse(pc.isIgnoreTransaction());
        Assert.assertFalse(pc.isExcludeFromTransactionTrace());
        Assert.assertNull(pc.getMetricNameFormat());
        Assert.assertEquals("test.CustomExampleTest", pc.getClassName().getValue());
        List<Method> methods = pc.getMethod();
        Assert.assertEquals(2, methods.size());
        Assert.assertEquals("run", methods.get(0).getName());
        Assert.assertEquals("finish", methods.get(1).getName());
        Assert.assertEquals("(Ljava/lang/String;Ljava/lang/String;)", MethodParameters.getDescriptor(methods.get(0).getParameters()));
        Assert.assertEquals("(F)", MethodParameters.getDescriptor(methods.get(1).getParameters()));

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String>of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(B)V",
                com.google.common.collect.ImmutableSet.<String>of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Assert.assertEquals("test/CustomExampleTest", actual.getClassMatcher().getClassNames().toArray()[0]);
    }

    @Test
    public void testDomNullString() throws Exception {
        Assert.assertNull(ExtensionDomParser.readStringCatchException(null));
    }

    @Test
    public void testDomEmptyString() throws Exception {
        Assert.assertNull(ExtensionDomParser.readStringCatchException(""));
    }

    @Test
    public void testDomInvalidString() throws Exception {
        Assert.assertNull(ExtensionDomParser.readStringCatchException("<Hello"));
    }

    @Test
    public void testClassInterface() throws Exception {
        Extension ext = ExtensionDomParser.readFile(getFile(FILE_PATH_7_INTERFACE_CLASS));
        // ext attributes
        Assert.assertEquals("test1", ext.getName());
        Assert.assertEquals(1.0, ext.getVersion(), .001);
        Assert.assertTrue(ext.isEnabled());

        Instrumentation inst = ext.getInstrumentation();

        List<Pointcut> thePcs = inst.getPointcut();
        Assert.assertEquals(2, thePcs.size());
        Pointcut pc = thePcs.get(0);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertEquals("com.newrelic.agent.extension.beans.Extension", pc.getClassName().getValue());
        Assert.assertNull(pc.getInterfaceName());
        List<Method> methods = pc.getMethod();
        Assert.assertEquals(1, methods.size());

        pc = thePcs.get(1);
        Assert.assertTrue(pc.isTransactionStartPoint());
        Assert.assertEquals("com.newrelic.agent.Agent", pc.getInterfaceName());
        Assert.assertNull(pc.getClassName());
        methods = pc.getMethod();
        Assert.assertEquals(2, methods.size());

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToPointCutsForValidation(ext);

        Assert.assertNotNull(pcs);
        Assert.assertEquals(2, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);
        Assert.assertEquals(ExactClassMatcher.class, actual.getClassMatcher().getClass());
        actual = pcs.get(1);
        Assert.assertEquals(InterfaceMatcher.class, actual.getClassMatcher().getClass());
    }

    @Test(expected = SAXParseException.class)
    public void testClassInterfaceInvalidMultiple() throws Exception {
        try {
            ExtensionDomParser.readFile(getFile(FILE_PATH_9_INTERFACE_CLASS));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("interfaceName"));
            throw e;
        }
    }

    private String readInFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            int size = fis.available();
            byte[] output = new byte[size];
            fis.read(output);
            return new String(output);
        }
    }

}
