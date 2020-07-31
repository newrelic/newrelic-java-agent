/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.complexenv;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import org.junit.Test;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.util.HashMap;
import java.util.Map;

public class ClassRetransformMapperTest {

    @Test
    public void testMethodMapper() throws Exception {
        String transactionMetric = "OtherTransaction/Custom/"
                + "com.newrelic.agent.instrumentation.complexenv.ClassRetransformMapperTest$TestHttpServlet"
                + "/getServletConfig";
        String methodMetric = "Java/"
                + "com.newrelic.agent.instrumentation.complexenv.ClassRetransformMapperTest$TestHttpServlet"
                + "/getServletConfig";

        Servlet servlet = new TestHttpServlet();
        servlet.getServletConfig();
        InstrumentTestUtils.verifyMetricNotPresent(transactionMetric);

        // reinstrument
        InstrumentTestUtils.createTransformerAndRetransformInterface(Servlet.class.getName(), "getServletConfig",
                "()Ljavax/servlet/ServletConfig;");

        // call method on class that was transformed
        Servlet servlet1 = new TestHttpServlet();
        servlet1.getServletConfig();

        // // verifiy
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transactionMetric, 1);
        expected.put(methodMetric, 1);
        // this will perform a harvest
        InstrumentTestUtils.verifyCountMetric(expected);

    }

    class TestHttpServlet extends HttpServlet {
        private static final long serialVersionUID = 7783343003343993230L;

    }
}
