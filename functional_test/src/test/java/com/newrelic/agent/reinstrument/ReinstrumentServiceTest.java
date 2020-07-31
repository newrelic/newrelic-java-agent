/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionListener;
import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.TransactionStats;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReinstrumentServiceTest {

    @Test
    public void testBasicReinstrumentService() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/getName";
        String methodMetric = "Java/" + InstrumentMeObject.class.getName() + "/getName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getName</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("<method>");
        sb.append("<name>getAge</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getName("mr");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.getName("mrs");

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertEquals("com.newrelic.agent.reinstrument.InstrumentMeObject",
                actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

        // verify metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - this should not do anything new
        result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        // verify result
        actual = result.getStatusMap();

        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        // do not create an error if we have already added the pc
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        // Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    @Test
    public void testClassValidationReinstrumentService() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObjectInvalidClass");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>blabla</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        String errors = (String) actual.get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNotNull(errors);
        String clazz = "com.newrelic.agent.reinstrument.InstrumentMeObjectInvalidClass";
        Assert.assertTrue("The class should be contained in the error message. ERROR: " + clazz, errors.contains(clazz));

        // sdaubin : I don't think it's worth getting overly fancy with the retransformations - if a class is matched by
        // the xml, let's retransform it
        // Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

    }

    /** This test looks at method name matching. */
    @Test
    public void testMethodValidationReinstrumentService1() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/getHairColor";
        String methodMetric = "Java/" + InstrumentMeObject.class.getName() + "/getHairColor";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getHairColor</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("<method>");
        sb.append("<name>getNailColor</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getHairColor();
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric));

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj.getHairColor();
        InstrumentTestUtils.verifySingleMetrics(transactionMetric, methodMetric);

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        String errors = (String) actual.get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNotNull(errors);
        String clazz = "com.newrelic.agent.reinstrument.InstrumentMeObject";
        Assert.assertTrue("The class should be contained in the error message. ERROR: " + clazz, errors.contains(clazz));
        Assert.assertTrue("The method should be contained in the error message. ERROR: getNailColor",
                errors.contains("getNailColor"));
        Assert.assertNotNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

    }

    /** This test looks at method parameter matching. */
    @Test
    public void testMethodValidationReinstrumentService2() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/getAgeInXYears";
        String methodMetric = "Java/" + InstrumentMeObject.class.getName() + "/getAgeInXYears";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getAgeInXYears</name>");
        // the parameters are incorrect and so it should not match
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("<method>");
        sb.append("<name>getNailColor</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getAgeInXYears(4);
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric));

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj.getAgeInXYears(2);
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric));

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        String errors = (String) actual.get(ReinstrumentResult.ERROR_KEY);
        Assert.assertNotNull(errors);
        String clazz = "com.newrelic.agent.reinstrument.InstrumentMeObject";
        Assert.assertTrue("The class should be contained in the error message. ERROR: " + clazz, errors.contains(clazz));
        Assert.assertTrue("The method should be contained in the error message. ERROR: getAgeInXYears",
                errors.contains("getAgeInXYears"));
        Assert.assertNotNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

    }

    @Test
    public void testInterfaceXml() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/getHeight";
        String methodMetric = "Java/" + InstrumentMeObject.class.getName() + "/getHeight";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<interfaceName>com.newrelic.agent.reinstrument.InstrumentInterfaceObject");
        sb.append("</interfaceName>");
        sb.append("<method>");
        sb.append("<name>getHeight</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getHeight();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        InstrumentImplObject secondObj = new InstrumentImplObject();
        secondObj.getHeight();

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.getHeight();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.newrelic.agent.reinstrument.InstrumentMeObject"));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.newrelic.agent.reinstrument.InstrumentImplObject"));

        // verifiy metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // reinstrument again - this should not do anything new
        result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        // This is currently not null because we are reinstrumenting every time
        // Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    @Test
    public void testSuperClassXml() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeChildObject.class.getName()
                + "/getShoeSize";
        String methodMetric = "Java/" + InstrumentMeChildObject.class.getName() + "/getShoeSize";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getShoeSize</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeChildObject obj = new InstrumentMeChildObject();
        obj.getShoeSize();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        InstrumentMeObject secondObj = new InstrumentMeObject();
        secondObj.getShoeSize();

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeChildObject();
        obj.getShoeSize();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.newrelic.agent.reinstrument.InstrumentMeObject"));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.newrelic.agent.reinstrument.InstrumentMeChildObject"));

        // verifiy metrics
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

        // second XML
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getShoeSize</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.InstrumentMeChildObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getRingSize</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument again - this should not do anything new
        result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(2, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertTrue(((String) actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY)).contains("com.newrelic.agent.reinstrument.InstrumentMeChildObject"));
    }

    @Test
    public void testInstrumentReturnType() {
        String transactionMetric1 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/method1";
        String methodMetric1 = "Java/" + InstrumentMeObject.class.getName() + "/method1";
        String transactionMetric2 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/method2";
        String methodMetric2 = "Java/" + InstrumentMeObject.class.getName() + "/method2";
        String transactionMetric3 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/method3";
        String methodMetric3 = "Java/" + InstrumentMeObject.class.getName() + "/method3";
        String methodMetricName = "Java/" + InstrumentMeObject.class.getName() + "/getName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>");
        sb.append(InstrumentMeObject.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<returnType>com.newrelic.agent.reinstrument.InstrumentMeReturnType</returnType>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.method1();
        obj.method2(5);
        obj.method3("hello");
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric1, methodMetric1, transactionMetric2,
                methodMetric2, transactionMetric3, methodMetric3));

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.getName("mrs");
        obj.method1();
        obj.method2(5);
        obj.method3("hello");

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertEquals("com.newrelic.agent.reinstrument.InstrumentMeObject",
                actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));

        // verify metrics
        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(transactionMetric1, methodMetric1, transactionMetric2,
                methodMetric2, transactionMetric3, methodMetric3), Arrays.asList(methodMetricName));
    }

    @Test
    public void testInstrumentAnnotation() throws NoSuchMethodException, SecurityException {
        String transactionMetric1 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/aMethod1";
        String methodMetric1 = "Java/" + InstrumentMeObject.class.getName() + "/aMethod1";
        String transactionMetric2 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/aMethod2";
        String methodMetric2 = "Java/" + InstrumentMeObject.class.getName() + "/aMethod2";
        String transactionMetric3 = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/aMethod3";
        String methodMetric3 = "Java/" + InstrumentMeObject.class.getName() + "/aMethod3";
        String methodMetricName = "Java/" + InstrumentMeObject.class.getName() + "/getName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<methodAnnotation>com.newrelic.agent.reinstrument.InstrumentAnnotation");
        sb.append("</methodAnnotation>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.aMethod1();
        obj.aMethod2(5);
        obj.aMethod3("hello");
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric1, methodMetric1, transactionMetric2,
                methodMetric2, transactionMetric3, methodMetric3));

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        InstrumentedMethod annotation = InstrumentMeObject.class.getMethod("aMethod1").getAnnotation(
                InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.instrumentationNames().length, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.RemoteCustomXml, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("test1", annotation.instrumentationNames()[0]);

        obj = new InstrumentMeObject();
        obj.getName("mrs");
        obj.aMethod1();
        obj.aMethod2(5);
        obj.aMethod3("hello");

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metrics
        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(transactionMetric1, methodMetric1, transactionMetric2,
                methodMetric2, transactionMetric3, methodMetric3), Arrays.asList(methodMetricName));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testXmlExternalEntityReinstrumentService() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName()
                + "/getAnotherMethodWahoo";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE extension [ ");
        sb.append("<!ENTITY readthefile SYSTEM \"file:///tmp/testfile\"> ");
        sb.append("]>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>&readthefile;</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getAnotherMethodWahoo();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        Map<String, Object> actual = result.getStatusMap();

        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertTrue("Failed: Actual error: " + actual.get(ReinstrumentResult.ERROR_KEY),
                ((String) actual.get(ReinstrumentResult.ERROR_KEY)).contains("DOCTYPE is disallowed"));

        // javax.xml.parsers.DocumentBuilderFactory
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testXmlExternalEntityReinstrumentServiceNoExtension() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName()
                + "/getAnotherMethodWahoo";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
        sb.append("<!DOCTYPE readfile [ ");
        sb.append("<!ENTITY send SYSTEM \"file://tmp/testfile\">");
        sb.append("<!ENTITY dtd SYSTEM \"file://tmp/send.dtd\" > %dtd; %send;");
        sb.append("]>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getAnotherMethodWahoo</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getAnotherMethodWahoo();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        Map<String, Object> actual = result.getStatusMap();

        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertTrue("Failed: Actual error: " + actual.get(ReinstrumentResult.ERROR_KEY),
                ((String) actual.get(ReinstrumentResult.ERROR_KEY)).contains("DOCTYPE is disallowed"));

        // javax.xml.parsers.DocumentBuilderFactory
    }

    /*
     * Test attributeName when multiple types of instrumentation are being applied to the same method.
     */
    @Test
    public void testTransactionEventsMultipleInstrumentationXml() {
        SeeTransactions transactions = new SeeTransactions();
        ServiceFactory.getTransactionService().addTransactionListener(transactions);
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/bMethod1234";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>bMethod1234</name>");
        sb.append("<parameters>");
        sb.append("<type attributeName=\"myAtt\">java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.bMethod1234("Hello1");

        // should not be any user attributes
        Map<String, Map<String, Object>> userAtts = transactions.getUserAtts();
        Assert.assertEquals(1, userAtts.size());
        Map<String, Object> atts = userAtts.get(transactionMetric);
        Assert.assertNotNull(atts);
        Assert.assertEquals(0, atts.size());
        transactions.clearMap();

        // add xml instrumentation
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.bMethod1234("Hello2");

        userAtts = transactions.getUserAtts();
        Assert.assertEquals(1, userAtts.size());
        atts = userAtts.get(transactionMetric);
        Assert.assertNotNull(atts);
        Assert.assertEquals(1, atts.size());
        Assert.assertEquals("Hello2", atts.get("myAtt"));
        transactions.clearMap();

        obj = new InstrumentMeObject();
        obj.bMethod1234("Hello3");

        userAtts = transactions.getUserAtts();
        Assert.assertEquals(1, userAtts.size());
        atts = userAtts.get(transactionMetric);
        Assert.assertNotNull(atts);
        Assert.assertEquals(1, atts.size());
        Assert.assertEquals("Hello3", atts.get("myAtt"));

    }

    public class SeeTransactions implements TransactionListener {
        private volatile Map<String, Map<String, Object>> userAttsForEachTrans = new HashMap<>();

        @Override
        public void dispatcherTransactionFinished(TransactionData transactionData, TransactionStats transactionStats) {
            String name = transactionData.getPriorityTransactionName().getName();
            userAttsForEachTrans.put(name, transactionData.getUserAttributes());

        }

        Map<String, Map<String, Object>> getUserAtts() {
            return userAttsForEachTrans;
        }

        void clearMap() {
            userAttsForEachTrans.clear();
        }

    }

}
