/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.pointcuts.frameworks.cxf;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;

public class CXFInvokerPointCutTest {
    @Test
    public void test() throws Exception {
        Method method = Object.class.getMethod("toString");
        String expectedMetricName = "/service/test/toString";
        Assert.assertEquals(expectedMetricName, CXFInvokerPointCut.getCXFRequestUri("/service/test/", method));

        Assert.assertEquals(expectedMetricName, CXFInvokerPointCut.getCXFRequestUri("http://localhost/service/test/",
                method));
        Assert.assertEquals(expectedMetricName, CXFInvokerPointCut.getCXFRequestUri(
                "http://localhost:8080/service/test/", method));

        Assert.assertEquals(expectedMetricName, CXFInvokerPointCut.getCXFRequestUri("https://localhost/service/test/",
                method));
        Assert.assertEquals(expectedMetricName, CXFInvokerPointCut.getCXFRequestUri(
                "https://localhost:8080/service/test/", method));
    }

    @Test
    public void testProxyRenaming() throws NoSuchMethodException {
        String generateProxyTxnName = CXFInvokerPointCut.buildCXFTransactionName(
                "com.sun.proxy.$Proxy243.EnterpriseSynergyGenerator", "dudeMethod");
        Assert.assertEquals("com.sun.proxy.$Proxy.EnterpriseSynergyGenerator/dudeMethod", generateProxyTxnName);

        String noProxyTxnName = CXFInvokerPointCut.buildCXFTransactionName(
                "com.moon.proxy.EnterpriseArchitecturePattern", "crushMe");
        Assert.assertEquals("com.moon.proxy.EnterpriseArchitecturePattern/crushMe", noProxyTxnName);
    }

}
