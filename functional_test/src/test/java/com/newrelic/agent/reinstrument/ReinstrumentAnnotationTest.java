/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.HttpJspPage;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.service.ServiceFactory;
import test.newrelic.test.agent.RpcCall;

public class ReinstrumentAnnotationTest {

    @Test
    public void testAnnotation() throws NoSuchMethodException, SecurityException {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObj2.class.getName() + "/getName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"testing 123\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getName</name>");
        sb.append("<parameters>");
        sb.append("<type>java.lang.String</type>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObj2 obj = new InstrumentMeObj2();
        obj.getName("mr");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        InstrumentedMethod annotation = InstrumentMeObj2.class.getDeclaredMethod("getName",
                String.class).getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.instrumentationNames().length, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.RemoteCustomXml, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("testing 123", annotation.instrumentationNames()[0]);
        Assert.assertTrue(annotation.dispatcher());
    }

    @Test
    public void testAnnotationExludeFromTT() throws NoSuchMethodException, SecurityException {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObj2.class.getName() + "/getAge";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"custom xml wahoo\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" excludeFromTransactionTrace=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getAge</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObj2 obj = new InstrumentMeObj2();
        obj.getAge();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        InstrumentedMethod annotation = InstrumentMeObj2.class.getDeclaredMethod("getAge").getAnnotation(
                InstrumentedMethod.class);

        InstrumentMeObj2 obj1 = new InstrumentMeObj2();
        obj1.getAge();
        InstrumentTestUtils.verifyMetricPresent(transactionMetric);

        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.instrumentationNames().length, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.RemoteCustomXml, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("custom xml wahoo", annotation.instrumentationNames()[0]);
        Assert.assertTrue(annotation.dispatcher());
    }

    @Test
    public void testAnnotationIgnore() throws NoSuchMethodException, SecurityException {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObj2.class.getName() + "/getSecondAge";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"custom xml wahoo\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" ignoreTransaction=\"true\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getSecondAge</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObj2 obj = new InstrumentMeObj2();
        obj.getSecondAge();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        InstrumentedMethod annotation = InstrumentMeObj2.class.getDeclaredMethod("getSecondAge").getAnnotation(
                InstrumentedMethod.class);

        InstrumentMeObj2 obj1 = new InstrumentMeObj2();
        obj1.getSecondAge();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // no annotation for ignored methods
        Assert.assertNull(annotation);
    }

    @Test
    public void testAnnotationMetricNameFormat() throws NoSuchMethodException, SecurityException {
        String transactionMetric = "OtherTransaction/Custom/hello";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"foobar\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"false\" metricNameFormat=\"hello\">");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getShoeSize</name>");
        sb.append("<parameters>");
        sb.append("</parameters>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        InstrumentMeObj2 obj = new InstrumentMeObj2();
        obj.getShoeSize();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        InstrumentedMethod annotation = InstrumentMeObj2.class.getDeclaredMethod("getShoeSize").getAnnotation(
                InstrumentedMethod.class);

        InstrumentMeObj2 obj1 = new InstrumentMeObj2();
        obj1.getShoeSize();

        Assert.assertNotNull(annotation);
        Assert.assertEquals(annotation.instrumentationNames().length, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.RemoteCustomXml, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("foobar", annotation.instrumentationNames()[0]);
        Assert.assertFalse(annotation.dispatcher());
    }

    @Test
    public void testAnnotationMixedInstrumentationYamlXml() throws ParserConfigurationException, IOException,
            SAXException, NoSuchMethodException, SecurityException {
        ServiceFactory.getStatsService().getStatsEngineForHarvest(null).clear();

        Method method = SolrCore.class.getDeclaredMethod("getSearcher", boolean.class, boolean.class, Future[].class);
        InstrumentedMethod annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(1, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.CustomYaml, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("Solr", annotation.instrumentationNames()[0]);

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"MySecondSolr\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>org.apache.solr.core.SolrCore");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getSearcher</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        method = SolrCore.class.getDeclaredMethod("getSearcher", boolean.class, boolean.class, Future[].class);
        annotation = method.getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(2, annotation.instrumentationTypes().length);

        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            if (annotation.instrumentationTypes()[i] == InstrumentationType.CustomYaml) {
                Assert.assertEquals("Solr", annotation.instrumentationNames()[i]);
            } else if (annotation.instrumentationTypes()[i] == InstrumentationType.RemoteCustomXml) {
                Assert.assertEquals("MySecondSolr", annotation.instrumentationNames()[i]);
            } else {
                Assert.fail("The instrumentation type should be custom yaml or remote custom xml. Type: "
                        + annotation.instrumentationTypes()[i]);
            }
        }
    }

    @Test
    public void testAnnotationMixedInstrumentationTraceXml() throws ParserConfigurationException, IOException,
            SAXException, NoSuchMethodException, SecurityException {

        InstrumentMeObj2 obj = new InstrumentMeObj2();
        obj.getHairColor();

        InstrumentedMethod annotation = InstrumentMeObj2.class.getDeclaredMethod("getHairColor").getAnnotation(
                InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(1, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.TraceAnnotation, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("InstrumentMeObj2.java", annotation.instrumentationNames()[0]);

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"Hairry\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>com.newrelic.agent.reinstrument.InstrumentMeObj2");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>getHairColor</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        annotation = InstrumentMeObj2.class.getDeclaredMethod("getHairColor").getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(2, annotation.instrumentationTypes().length);

        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            if (annotation.instrumentationTypes()[i] == InstrumentationType.TraceAnnotation) {
                Assert.assertEquals("InstrumentMeObj2.java", annotation.instrumentationNames()[i]);
            } else if (annotation.instrumentationTypes()[i] == InstrumentationType.RemoteCustomXml) {
                Assert.assertEquals("Hairry", annotation.instrumentationNames()[i]);
            } else {
                Assert.fail("The instrumentation type should be custom yaml or remote custom xml. Type: "
                        + annotation.instrumentationTypes()[i]);
            }
        }
    }

    @Test
    public void testWeavePlusXml() throws NoSuchMethodException, SecurityException {
        class JspTest implements HttpJspPage {

            @Override
            public void jspDestroy() {
            }

            @Override
            public void jspInit() {
            }

            @Override
            public void init(ServletConfig config) throws ServletException {
            }

            @Override
            public ServletConfig getServletConfig() {
                return null;
            }

            @Override
            public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
            }

            @Override
            public String getServletInfo() {
                return null;
            }

            @Override
            public void destroy() {
            }

            @Override
            public void _jspService(HttpServletRequest arg0, HttpServletResponse arg1) throws ServletException,
                    IOException {
            }

        }

        JspTest test = new JspTest();

        InstrumentedMethod annotation = JspTest.class.getDeclaredMethod(
                "_jspService",
                HttpServletRequest.class, HttpServletResponse.class).getAnnotation(
                InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(2, annotation.instrumentationTypes().length);
        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            Assert.assertEquals("com.newrelic.instrumentation.jsp-2.4", annotation.instrumentationNames()[i]);
            Assert.assertTrue(annotation.instrumentationTypes()[i] == InstrumentationType.TracedWeaveInstrumentation
                    || annotation.instrumentationTypes()[i] == InstrumentationType.WeaveInstrumentation);
        }

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"Not A Pointcut\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>" + JspTest.class.getName());
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>_jspService</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        annotation = JspTest.class.getDeclaredMethod(
                "_jspService",
                HttpServletRequest.class, HttpServletResponse.class).getAnnotation(
                InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(3, annotation.instrumentationTypes().length);
        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            if (annotation.instrumentationNames()[i].equals("com.newrelic.instrumentation.jsp-2.4")) {
                Assert.assertTrue(annotation.instrumentationTypes()[i] == InstrumentationType.TracedWeaveInstrumentation
                        || annotation.instrumentationTypes()[i] == InstrumentationType.WeaveInstrumentation);
            } else if (annotation.instrumentationNames()[i].equals("Not A Pointcut")) {
                Assert.assertTrue(annotation.instrumentationTypes()[i] == InstrumentationType.RemoteCustomXml);
            } else {
                Assert.fail("The instrumentation name does not match. Name: " + annotation.instrumentationNames()[i]);
            }
        }

    }

    @Test
    public void testXmlPlusPointCut() throws NoSuchMethodException, SecurityException {

        RpcCall rpcCall = new RpcCall();

        InstrumentedMethod annotation = test.newrelic.test.agent.RpcCall.class.getDeclaredMethod(
                "invoke", Object[].class).getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(1, annotation.instrumentationTypes().length);
        Assert.assertEquals(InstrumentationType.Pointcut, annotation.instrumentationTypes()[0]);
        Assert.assertEquals("com.newrelic.agent.instrumentation.pointcuts.XmlRpcPointCut",
                annotation.instrumentationNames()[0]);

        // reinstrument with more instrumentation
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<extension xmlns=\"https://newrelic.com/docs/java/xsd/v1.0\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"Not A Pointcut\">");
        sb.append("<instrumentation>");
        sb.append("<pointcut transactionStartPoint=\"true\" >");
        sb.append("<className>test.newrelic.test.agent.RpcCall");
        sb.append("</className>");
        sb.append("<method>");
        sb.append("<name>invoke</name>");
        sb.append("</method>");
        sb.append("</pointcut>");
        sb.append("</instrumentation>");
        sb.append("</extension>");

        // reinstrument for the first time
        ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        annotation = test.newrelic.test.agent.RpcCall.class.getDeclaredMethod(
                "invoke", Object[].class).getAnnotation(InstrumentedMethod.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals(2, annotation.instrumentationTypes().length);

        for (int i = 0; i < annotation.instrumentationTypes().length; i++) {
            if (annotation.instrumentationTypes()[i] == InstrumentationType.Pointcut) {
                Assert.assertEquals("com.newrelic.agent.instrumentation.pointcuts.XmlRpcPointCut",
                        annotation.instrumentationNames()[i]);
            } else if (annotation.instrumentationTypes()[i] == InstrumentationType.RemoteCustomXml) {
                Assert.assertEquals("Not A Pointcut", annotation.instrumentationNames()[i]);
            } else {
                Assert.fail("The instrumentation type should be custom yaml or remote custom xml. Type: "
                        + annotation.instrumentationTypes()[i]);
            }
        }
    }
}
