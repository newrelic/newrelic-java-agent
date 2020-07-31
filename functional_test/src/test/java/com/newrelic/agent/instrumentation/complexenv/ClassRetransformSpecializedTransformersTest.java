/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.complexenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.derby.impl.jdbc.EmbedPreparedStatement;
import org.apache.tomcat.util.http.ServerCookies;
import org.junit.Test;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.instrumentation.InstrumentTestUtils;
import com.sun.grizzly.tcp.Request;
import com.sun.grizzly.util.http.Cookies;
import com.sun.grizzly.util.http.Parameters;
import com.sun.grizzly.util.http.ServerCookie;

public class ClassRetransformSpecializedTransformersTest {

    private static final String GRIZZLY_REQUST_CLASS_NAME = "com.sun.grizzly.tcp.Request";
    private static final String GRIZZLY_COOKIE_CLASS_NAME = "com.sun.grizzly.util.http.Cookies";
    private static final String COYOTE_REQUST_CLASS_NAME = "org.apache.coyote.Request";

    @Test
    public void testGrizzlyTransformer() throws Exception {
        String transGetCookies = InstrumentTestUtils.TRANS_PREFIX + GRIZZLY_REQUST_CLASS_NAME + "/getCookies";
        String methodGetCookies = InstrumentTestUtils.METHOD_PREFIX + GRIZZLY_REQUST_CLASS_NAME + "/getCookies";
        String transFirstCookie = InstrumentTestUtils.TRANS_PREFIX + GRIZZLY_COOKIE_CLASS_NAME + "/getCookie";
        String methodFirstCookie = InstrumentTestUtils.METHOD_PREFIX + GRIZZLY_COOKIE_CLASS_NAME + "/getCookie";
        String transGetParams = InstrumentTestUtils.TRANS_PREFIX + GRIZZLY_REQUST_CLASS_NAME + "/getParameters";
        String methodGetParams = InstrumentTestUtils.METHOD_PREFIX + GRIZZLY_REQUST_CLASS_NAME + "/getParameters";

        // the the methods we are going to instrument
        callGrizzyMethods();
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transGetCookies, transFirstCookie, transGetParams,
                methodGetCookies, methodGetParams, methodFirstCookie));

        // perform getCookies re-instrumentation
        InstrumentTestUtils.createTransformerAndRetransformClass(Request.class.getName(), "getCookies",
                "()Lcom/sun/grizzly/util/http/Cookies;");

        callGrizzyMethods();
        InstrumentTestUtils.verifySingleMetrics(transGetCookies, methodGetCookies);

        // perform FirstCookie re-instrumentation
        InstrumentTestUtils.createTransformerAndRetransformClass(Cookies.class.getName(), "getCookie",
                "(I)Lcom/sun/grizzly/util/http/ServerCookie;");

        callGrizzyMethods();
        InstrumentTestUtils.verifySingleMetrics(transGetCookies, methodGetCookies, transFirstCookie, methodFirstCookie);

        // perform getParameters re-instrumentation
        InstrumentTestUtils.createTransformerAndRetransformClass(Request.class.getName(), "getParameters",
                "()Lcom/sun/grizzly/util/http/Parameters;");

        callGrizzyMethods();
        InstrumentTestUtils.verifySingleMetrics(transGetCookies, methodGetCookies, transFirstCookie, methodFirstCookie,
                transGetParams, methodGetParams);
    }

    private void callGrizzyMethods() {
        Request request = new Request();
        Cookies cookies = request.getCookies();
        cookies.addCookie();
        ServerCookie sc = cookies.getCookie(0);
        Parameters params = request.getParameters();
    }

    @Test
    public void testCoyoteTransformer() throws Exception {
        String transGetCookies = InstrumentTestUtils.TRANS_PREFIX + COYOTE_REQUST_CLASS_NAME + "/getCookies";
        String methodGetCookies = InstrumentTestUtils.METHOD_PREFIX + COYOTE_REQUST_CLASS_NAME + "/getCookies";
        String transGetParams = InstrumentTestUtils.TRANS_PREFIX + COYOTE_REQUST_CLASS_NAME + "/getParameters";
        String methodGetParams = InstrumentTestUtils.METHOD_PREFIX + COYOTE_REQUST_CLASS_NAME + "/getParameters";

        // the the methods we are going to instrument
        callCoyoteMethods();
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transGetCookies, transGetParams, methodGetCookies,
                methodGetParams));

        // perform getCookies re-instrumentation
        InstrumentTestUtils.createTransformerAndRetransformClass(org.apache.coyote.Request.class.getName(),
                "getCookies", "()Lorg/apache/tomcat/util/http/ServerCookies;");

        callCoyoteMethods();
        InstrumentTestUtils.verifySingleMetrics(transGetCookies, methodGetCookies);

        // perform getParameters re-instrumentation
        InstrumentTestUtils.createTransformerAndRetransformClass(org.apache.coyote.Request.class.getName(),
                "getParameters", "()Lorg/apache/tomcat/util/http/Parameters;");

        callCoyoteMethods();
        InstrumentTestUtils.verifySingleMetrics(transGetCookies, methodGetCookies, transGetParams, methodGetParams);
    }

    private void callCoyoteMethods() {
        org.apache.coyote.Request request = new org.apache.coyote.Request();
        ServerCookies cookies = request.getCookies();
        request.getParameters();
    }

}
