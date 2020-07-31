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

/**
 * This file is for tests which check that the enabled flag in the xml is honored.
 */
public class ReinstrumentServiceXmlDisabledTest {

    private static final String XML_ENABLED;
    private static final String XML_DISABLED;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" enabled=\"true\" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.InstrumentMeObject");
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
        XML_ENABLED = sb.toString();

        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urn:extension xmlns:urn=\"newrelic-extension\"");
        sb.append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append(" xsi:schemaLocation=\"newrelic-extension extension.xsd \" enabled=\"false\" name=\"test1\">");
        sb.append("<urn:instrumentation>");
        sb.append("<urn:pointcut transactionStartPoint=\"true\">");
        sb.append("<urn:className>com.newrelic.agent.reinstrument.InstrumentMeObject");
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
        XML_DISABLED = sb.toString();
    }

    private void addandVerifyEnabled(String transactionMetric) {
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_ENABLED);

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getName("mrs");
        InstrumentTestUtils.verifyMetricPresent(transactionMetric);

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(1, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    private void removeAndVerifyDisabled(String transactionMetric) {
        ReinstrumentResult result = ServiceFactory.getRemoteInstrumentationService().processXml(XML_DISABLED);

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getName("mrs");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // verify result
        Map<String, Object> actual = result.getStatusMap();
        Assert.assertEquals(0, actual.get(ReinstrumentResult.PCS_SPECIFIED_KEY));
        Assert.assertNull(actual.get(ReinstrumentResult.ERROR_KEY));
        Assert.assertNotNull(actual.get(ReinstrumentResult.RETRANSFORM_INIT_KEY));
    }

    /**
     * This test is timing sensitive - if other ReinstrumentServiceTests run in front of it it will fail. Because of
     * that it has been moved to a separate test.
     */
    @Test
    public void testBasicReinstrumentXmlDisabled() {
        String transactionMetric = "OtherTransaction/Custom/" + InstrumentMeObject.class.getName() + "/getName";

        InstrumentMeObject obj = new InstrumentMeObject();
        obj.getName("mr");
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // add and remove instrumentation 2 times
        for (int i = 0; i < 2; i++) {
            addandVerifyEnabled(transactionMetric);
            removeAndVerifyDisabled(transactionMetric);
        }
    }

}
