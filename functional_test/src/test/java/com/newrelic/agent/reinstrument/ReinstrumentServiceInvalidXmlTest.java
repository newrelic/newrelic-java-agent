/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.newrelic.agent.service.ServiceFactory;

public class ReinstrumentServiceInvalidXmlTest {

    @Test
    public void testInvalidXml() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/setLastName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        // this line really should have a > on the end of it
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\"");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.InstrumentMeObject");
        sb.append("</urn:className>");
        sb.append("<urn:method>");
        sb.append("<urn:name>setLastName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("<urn:type>java.lang.String</urn:type>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.setLastName("smith");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time - this will fail because invalid xml
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.setLastName("smith");

        // verify result failed
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    @Test
    public void testNoClassNameXml() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/setLastName";

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        // there is no class name here
        sb.append("<urn:method>");
        sb.append("<urn:name>setLastName</urn:name>");
        sb.append("<urn:parameters>");
        sb.append("<urn:type>java.lang.String</urn:type>");
        sb.append("</urn:parameters>");
        sb.append("</urn:method>");
        sb.append("</urn:pointcut>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.setLastName("smith");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument for the first time - this will fail because invalid xml
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.setLastName("smith");

        // verify result failed
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    @Test
    public void testEmptyXml() {

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.setLastName("smith");
        ;

        // reinstrument for the first time - this will fail because invalid xml
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml("");

        obj = new InstrumentMeObject();
        obj.setLastName("smith");

        // verify result failed
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    @Test
    public void testNoPointCutsXml() {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("</urn:instrumentation>");
        sb.append("</urn:extension>");

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.setLastName("smith");
        ;

        // reinstrument for the first time - this will fail because invalid xml
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(sb.toString());

        obj = new InstrumentMeObject();
        obj.setLastName("smith");

        // verify result failed
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

}
