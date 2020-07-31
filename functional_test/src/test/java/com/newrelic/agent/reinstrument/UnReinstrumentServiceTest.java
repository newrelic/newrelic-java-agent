/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.util.Arrays;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.service.ServiceFactory;

public class UnReinstrumentServiceTest {

    private static final String XML_INIT;
    private static final String XML_ONE_METH_REMOVED;
    private static final String XML_ALL_METH_REMOVED;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.UninstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("<urn:type>java.lang.String</urn:type>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getAge</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_INIT = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.UninstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("<urn:type>java.lang.String</urn:type>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_ONE_METH_REMOVED = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.UninstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>notARealMethod</urn:name>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_ALL_METH_REMOVED = sb.toString();
    }

    @Test
    public void testUninstrumentation() {
        String transactionMetric = "OtherTransaction/Custom/" + UninstrumentMeObject.class.getName() + "/getName";
        String methodMetric = "Java/" + UninstrumentMeObject.class.getName() + "/getName";
        String transactionMetricAge = "OtherTransaction/Custom/" + UninstrumentMeObject.class.getName() + "/getAge";
        String methodMetricAge = "Java/" + UninstrumentMeObject.class.getName() + "/getAge";

        UninstrumentMeObject obj = new UninstrumentMeObject();
        obj.getName("mr");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // 1. reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_INIT);

        obj = new UninstrumentMeObject();
        obj.getName("mrs");
        obj.getAge();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metrics
        InstrumentTestUtils.verifySingleMetrics(transactionMetric, methodMetric, transactionMetricAge, methodMetricAge);

        // 2. Now undo part of the instrumentation
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_ONE_METH_REMOVED);

        obj = new UninstrumentMeObject();
        obj.getName("mrs");
        obj.getAge();

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metrics
        InstrumentTestUtils.veryPresentNotPresent(Arrays.asList(transactionMetric, methodMetric), Arrays.asList(
                transactionMetricAge, methodMetricAge));

        // 3. remove all of the instrumentation
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_ALL_METH_REMOVED);

        obj = new UninstrumentMeObject();
        obj.getName("mrs");
        obj.getAge();

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metrics
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric, transactionMetricAge,
                methodMetricAge));
    }

    private static final String XML_INTERFACE;
    private static final String XML_REMOVE_INTERFACE;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:interfaceName>com.newrelic.agent.reinstrument.UninstrumentInterfaceObject");
        sb.append("</urn:interfaceName>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getHeight</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_INTERFACE = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:interfaceName>com.newrelic.agent.reinstrument.NotRealInterface");
        sb.append("</urn:interfaceName>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getHeight</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_REMOVE_INTERFACE = sb.toString();
    }

    @Test
    public void testInterfaceXmlUninstrument() {
        String transactionMetric = "OtherTransaction/Custom/" + UninstrumentMeObject.class.getName() + "/getHeight";
        String methodMetric = "Java/" + UninstrumentMeObject.class.getName() + "/getHeight";

        UninstrumentMeObject obj = new UninstrumentMeObject();
        obj.getHeight();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        InstrumentImplObject secondObj = new InstrumentImplObject();
        secondObj.getHeight();

        // 1. reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_INTERFACE);

        obj = new UninstrumentMeObject();
        obj.getHeight();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metrics
        InstrumentTestUtils.verifySingleMetrics(transactionMetric, methodMetric);

        // 2. Uninstrument
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_REMOVE_INTERFACE);

        obj = new UninstrumentMeObject();
        obj.getHeight();

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));

        // verify metric
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric));

    }

    private static final String XML_SUPER;
    private static final String XML_REMOVE_SUPER;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.UninstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getShoeSize</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.UninstrumentMeChildObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getRingSize</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_SUPER = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"true\">com.newrelic.agent.reinstrument.UninstrumentMeChildObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>notArealMethod</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_REMOVE_SUPER = sb.toString();
    }

    @Test
    public void testReinstrumentSuperXml() {
        String transactionMetric = "OtherTransaction/Custom/" + UninstrumentMeChildObject.class.getName()
                + "/getShoeSize";
        String methodMetric = "Java/" + UninstrumentMeChildObject.class.getName() + "/getShoeSize";
        String transactionMetricRing = "OtherTransaction/Custom/" + UninstrumentMeChildObject.class.getName()
                + "/getRingSize";
        String methodMetricRing = "Java/" + UninstrumentMeChildObject.class.getName() + "/getRingSize";

        UninstrumentMeChildObject obj = new UninstrumentMeChildObject();
        obj.getShoeSize();
        obj.getRingSize();

        // 1. reinstrument for the first time
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_SUPER);

        obj = new UninstrumentMeChildObject();
        obj.getShoeSize();
        obj.getRingSize();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(2, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        InstrumentTestUtils.verifySingleMetrics(transactionMetric, methodMetric, transactionMetricRing,
                methodMetricRing);

        // 2. uninstrument
        result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_REMOVE_SUPER);

        obj = new UninstrumentMeChildObject();
        obj.getShoeSize();
        obj.getRingSize();

        // verify result
        actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric,
                transactionMetricRing, methodMetricRing));
    }

    private static final String XML_BASIC;
    private static final String XML_REMOVE_ALL;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className includeSubclasses=\"false\">com.newrelic.agent.reinstrument.UninstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>getMiddleName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");
        XML_BASIC = sb.toString();

        sb = new StringBuilder();
        sb.append(" ");
        XML_REMOVE_ALL = sb.toString();
    }

    private void addAndVerifyMiddleNameInstrumentation(String transactionMetric, String methodMetric) {
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_BASIC);

        UninstrumentMeChildObject obj = new UninstrumentMeChildObject();
        obj.getMiddleName();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        InstrumentTestUtils.verifySingleMetrics(transactionMetric, methodMetric);
    }

    private void removeAndVerifyMiddleNameInstrumentation(String transactionMetric, String methodMetric) {
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_REMOVE_ALL);

        UninstrumentMeChildObject obj = new UninstrumentMeChildObject();
        obj.getMiddleName();

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transactionMetric, methodMetric));

    }

    @Test
    public void testReinstrumentRemoveAllXml() {
        String transactionMetric = "OtherTransaction/Custom/" + UninstrumentMeChildObject.class.getName()
                + "/getMiddleName";
        String methodMetric = "Java/" + UninstrumentMeChildObject.class.getName() + "/getMiddleName";

        UninstrumentMeChildObject obj = new UninstrumentMeChildObject();
        obj.getMiddleName();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // re-instrument and then un-instrument 10 times
        for (int i = 0; i < 10; i++) {
            addAndVerifyMiddleNameInstrumentation(transactionMetric, methodMetric);
            removeAndVerifyMiddleNameInstrumentation(transactionMetric, methodMetric);
        }
    }
}
