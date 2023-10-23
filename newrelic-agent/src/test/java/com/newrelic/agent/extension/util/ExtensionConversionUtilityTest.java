/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension.util;

import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.ClassName;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.dom.ExtensionDomParser;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.ParameterAttributeName;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Type;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.newrelic.agent.AgentHelper.getFile;

/**
 * Tests the ExtensionConversionUtility.
 * 
 * @since Sep 20, 2012
 */
public class ExtensionConversionUtilityTest {
    /** Test case one. */
    private static final String FILE_PATH_1 = "com/newrelic/agent/extension/util/test1.xml";
    /** Test file two. */
    private static final String FILE_PATH_2 = "com/newrelic/agent/extension/util/test2.xml";
    /**
     * This one has a point cut missing a method name and another one missing the class name.
     */
    private static final String FILE_PATH_3 = "com/newrelic/agent/extension/util/test3.xml";

    /** An empty xml. */
    private static final String FILE_PATH_4 = "com/newrelic/agent/extension/util/test4_empty.xml";

    /** Has all of the properties. */
    private static final String FILE_PATH_5 = "com/newrelic/agent/extension/util/test5_allprops.xml";

    /** File that contains multiples of classes and methods. */
    private static final String FILE_PATH_6_MULTIPLES = "com/newrelic/agent/extension/util/test6_mult.xml";

    /** The directory to write extension files to. */
    private static final String WRITE_DIR = "/tmp/extConUtil" + System.nanoTime() + "/";

    /** Test file two. */
    private static final String FILE_PATH_VALID = "com/newrelic/agent/extension/util/validXml.xml";

    private static final String FILE_PATH_ATTS = "com/newrelic/agent/extension/util/test_atts.xml";

    /**
     * Turns the extension to a list.
     * 
     * @param ext The extension.
     * @return List with the extension in it.
     */
    private Collection<Extension> toList(Extension ext) {
        List<Extension> extensions = new ArrayList<>();
        extensions.add(ext);
        return extensions;
    }

    /**
     * Sets up the extension dir.
     */
    private void setup() {
        // set the system property
        System.setProperty("newrelic.config.extensions.dir", WRITE_DIR);

        // create the directory
        File dir = new File(WRITE_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Cleans up the extensions directory.
     */
    private void cleanUp() {
        System.clearProperty("newrelic.config.extensions.dir");

        File dir = new File(WRITE_DIR);
        if (dir.exists()) {
            if (dir.isDirectory()) {
                File[] exts = dir.listFiles();
                for (File ext : exts) {
                    ext.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void testExtensionConversionUtilityAttributes() throws Exception {

        File file = getFile(FILE_PATH_ATTS);
        Extension extension = ExtensionDomParser.readFile(file);
        Assert.assertNotNull(extension);

        // default is to include atts
        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToEnabledPointCuts(
                toList(extension), true, InstrumentationType.RemoteCustomXml);
        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        List<ParameterAttributeName> actualAtts = pcs.get(0).getTraceDetails().getParameterAttributeNames();
        verifyAttNames(actualAtts, Arrays.asList("param1", "param2", "reportMe"));

        pcs = ExtensionConversionUtility.convertToEnabledPointCuts(toList(extension), true,
                InstrumentationType.RemoteCustomXml, true);
        actualAtts = pcs.get(0).getTraceDetails().getParameterAttributeNames();
        verifyAttNames(actualAtts, Arrays.asList("param1", "param2", "reportMe"));

        pcs = ExtensionConversionUtility.convertToEnabledPointCuts(toList(extension), true,
                InstrumentationType.RemoteCustomXml, false);
        actualAtts = pcs.get(0).getTraceDetails().getParameterAttributeNames();
        Assert.assertEquals(0, actualAtts.size());

    }

    private void verifyAttNames(List<ParameterAttributeName> actualAtts, List<String> expectedNames) {
        Assert.assertEquals(expectedNames.size(), actualAtts.size());
        // assumes they are all unique
        for (ParameterAttributeName curr : actualAtts) {
            Assert.assertTrue(expectedNames.contains(curr.getAttributeName()));
        }
    }

    @Test
    public void testExtensionConversionUtilityTest1() throws Exception {

        File file = getFile(FILE_PATH_1);
        Extension extension = ExtensionDomParser.readFile(file);
        Assert.assertNotNull(extension);

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToEnabledPointCuts(
                toList(extension), true, InstrumentationType.LocalCustomXml);
        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(F)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(B)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Assert.assertEquals("test/CustomExampleTest", actual.getClassMatcher().getClassNames().toArray()[0]);

    }

    @Test
    public void testExtensionConversionUtilityTest2() throws Exception {

        File file = getFile(FILE_PATH_2);
        Extension extension = ExtensionDomParser.readFileCatchException(file);
        Assert.assertNull(extension);

    }

    @Test
    public void testExtensionConversionUtilityValid() throws Exception {

        File file = getFile(FILE_PATH_VALID);
        Extension extension = ExtensionDomParser.readFileCatchException(file);
        Assert.assertNotNull(extension);

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToEnabledPointCuts(
                toList(extension), true, InstrumentationType.LocalCustomXml);
        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "([[Ljava/util/List;DI)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "timer",
                "([[Ljava/util/List;DI)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "timer",
                "(Ljava/util/List;)Ljava/lang/String", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "noParamMethod", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Collection<String> classes = actual.getClassMatcher().getClassNames();
        Assert.assertTrue(classes.contains("test/network/TestViewer"));

    }

    /**
     * Tests a config with no method name.
     */
    @Test(expected = SAXParseException.class)
    public void testExtensionConversionUtilityTest3() throws Exception {

        File file = getFile(FILE_PATH_3);
        Extension extension = ExtensionDomParser.readFile(file);
    }

    /**
     * Tests a config with zero point cuts.
     **/
    @Test
    public void testExtensionConversionUtilityTest4() throws Exception {

        File file = getFile(FILE_PATH_4);
        ExtensionDomParser.readFile(file);
        // Saxon thinks this is cool

    }

    /**
     * Tests a config with no method name.
     */
    @Test(expected = SAXParseException.class)
    public void testExtensionConversionUtilityTest5() throws Exception {

        File file = getFile(FILE_PATH_5);
        ExtensionDomParser.readFile(file);
    }

    /**
     * This tests multiple classes and methods.
     */
    @Test
    public void testExtensionConversionUtilityTest6() throws Exception {

        File file = getFile(FILE_PATH_6_MULTIPLES);
        Extension extension = ExtensionDomParser.readFile(file);
        Assert.assertNotNull(extension);

        List<ExtensionClassAndMethodMatcher> pcs = ExtensionConversionUtility.convertToEnabledPointCuts(
                toList(extension), true, InstrumentationType.RemoteCustomXml);
        Assert.assertNotNull(pcs);
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher actual = pcs.get(0);

        // test method matching
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish",
                "(Ljava/lang/String;Ljava/lang/String;)V", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(F)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "run", "(F)V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(actual.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, "finish", "(B)V",
                com.google.common.collect.ImmutableSet.<String> of()));

        // test class matching
        Assert.assertEquals(1, actual.getClassMatcher().getClassNames().size());
        Assert.assertEquals("test/CustomExampleTest", actual.getClassMatcher().getClassNames().toArray()[0]);

    }

    @Test
    public void primitiveReturnTypeArray() {
        // arrays of primitives are NOT okay
        Assert.assertFalse(ExtensionConversionUtility.isReturnTypeOkay(Type.getType("[Z")));
        // neither is an array of arrays of booleans
        Assert.assertFalse(ExtensionConversionUtility.isReturnTypeOkay(Type.getType("[[Z")));
    }

    @Test
    public void arrayOfStringsOkay() {
        // but an array of Strings is okay
        Assert.assertTrue(ExtensionConversionUtility.isReturnTypeOkay(Type.getType("[Ljava/lang/String;")));
    }

    @Test
    public void createClassMatcher() throws XmlException {
        Pointcut pointcut = new Pointcut();
        ClassName name = new ClassName();
        name.setValue(ArrayList.class.getName());
        pointcut.setClassName(name);
        ClassMatcher classMatcher = ExtensionConversionUtility.createClassMatcher(pointcut, "test");
        Assert.assertTrue(classMatcher instanceof ExactClassMatcher);
        Assert.assertTrue(classMatcher.isMatch(ArrayList.class));
    }

    @Test(expected = XmlException.class)
    public void createClassMatcher_empty() throws XmlException {
        Pointcut pointcut = new Pointcut();
        ClassName name = new ClassName();
        name.setValue(ArrayList.class.getName());
        ExtensionConversionUtility.createClassMatcher(pointcut, "test");
    }

    @Test(expected = XmlException.class)
    public void createClassMatcherAnnotation() throws XmlException {
        Pointcut pointcut = new Pointcut();
        pointcut.setMethodAnnotation("com.some.Annotation");
        ExtensionConversionUtility.createClassMatcher(pointcut, "test");
    }

    @Test(expected = XmlException.class)
    public void createClassMatcherReturnType() throws XmlException {
        Pointcut pointcut = new Pointcut();
        Method m = new Method();
        m.setReturnType(Action.class.getName());
        pointcut.getMethod().add(m);
        ExtensionConversionUtility.createClassMatcher(pointcut, "test");
    }

    @Test(expected=XmlException.class)
    public void validateExtensionAttributes_nullExtension_shouldThrow() throws XmlException{
        ExtensionConversionUtility.validateExtensionAttributes(null);
    }

    @Test(expected=XmlException.class)
    public void validateExtensionAttributes_extensionWithNullName_shouldThrow() throws XmlException{
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.getName()).thenReturn(null);
        ExtensionConversionUtility.validateExtensionAttributes(mockExt);
    }

    @Test(expected=XmlException.class)
    public void validateExtensionAttributes_extensionWithEmptyName_shouldThrow() throws XmlException{
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.getName()).thenReturn(null);
        ExtensionConversionUtility.validateExtensionAttributes(mockExt);
    }

    @Test(expected=XmlException.class)
    public void validateExtensionAttributes_extensionWithNegativeVersion_shouldThrow() throws XmlException{
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.getVersion()).thenReturn(-1.0);
        Mockito.when(mockExt.getName()).thenReturn("foo");
        ExtensionConversionUtility.validateExtensionAttributes(mockExt);
    }

    @Test(expected=XmlException.class)
    public void convertToPointCutsForValidation_nullInstrumentation_shouldThrow() throws XmlException{
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.getName()).thenReturn("foo");
        Mockito.when(mockExt.getInstrumentation()).thenReturn(null);
        ExtensionConversionUtility.convertToPointCutsForValidation(mockExt);
    }

    @Test(expected=XmlException.class)
    public void convertToPointCutsForValidation_instrumentationWithoutPointcuts_shouldThrow() throws XmlException{
        Extension mockExt = Mockito.mock(Extension.class);
        Extension.Instrumentation mockInst = Mockito.mock(Extension.Instrumentation.class);
        Mockito.when(mockExt.getInstrumentation()).thenReturn(mockInst);
        Mockito.when(mockExt.getName()).thenReturn("foo");
        Mockito.when(mockInst.getPointcut()).thenReturn(null);
        ExtensionConversionUtility.convertToPointCutsForValidation(mockExt);
    }

    @Test
    public void convertToEnabledPointCuts_nullExtensions_returnsEmptyList(){
        List<ExtensionClassAndMethodMatcher> pointCutsOut = ExtensionConversionUtility.convertToEnabledPointCuts(null, false, null, false);
        Assert.assertTrue(pointCutsOut.isEmpty());
    }

    @Test
    public void convertToEnabledPointCuts_oneDisabledExt_returnsEmptyList(){
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.isEnabled()).thenReturn(false);
        List<Extension> extensions = new ArrayList<>();
        extensions.add(mockExt);
        Assert.assertTrue(ExtensionConversionUtility.convertToEnabledPointCuts(extensions, false, null, false).isEmpty());
    }

    @Test
    public void convertToEnabledPointCuts_oneExtWithNoPointCuts_returnsEmptyList(){
        Extension mockExt = Mockito.mock(Extension.class);
        Mockito.when(mockExt.isEnabled()).thenReturn(true);
        Extension.Instrumentation mockInst = Mockito.mock(Extension.Instrumentation.class);
        Mockito.when(mockExt.getInstrumentation()).thenReturn(mockInst);
        List<Extension> extensions = new ArrayList<>();
        extensions.add(mockExt);
        Assert.assertTrue(ExtensionConversionUtility.convertToEnabledPointCuts(extensions, false, null, false).isEmpty());
    }

    @Test(expected=XmlException.class)
    public void createClassMatcher_emptyClassName_shouldThrow() throws XmlException{
        Pointcut mockPointcut = Mockito.mock(Pointcut.class);
        ClassName mockClassName = Mockito.mock(ClassName.class);
        Mockito.when(mockPointcut.getClassName()).thenReturn(mockClassName);
        Mockito.when(mockClassName.getValue()).thenReturn("");
        ExtensionConversionUtility.createClassMatcher(mockPointcut, "bar");
    }
}
