/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.cxfinvoker;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

public class CxfInvokerUtilsTest {

    @Test
    public void getCXFRequestUri_buildsPathFromAddressAndMethodName() throws Exception {
        Method method = Object.class.getMethod("toString");
        String expected = "/service/test/toString";

        Assert.assertEquals(expected, CxfInvokerUtils.getCXFRequestUri("/service/test/", method));
        Assert.assertEquals(expected, CxfInvokerUtils.getCXFRequestUri("http://localhost/service/test/", method));
        Assert.assertEquals(expected, CxfInvokerUtils.getCXFRequestUri("http://localhost:8080/service/test/", method));
        Assert.assertEquals(expected, CxfInvokerUtils.getCXFRequestUri("https://localhost/service/test/", method));
        Assert.assertEquals(expected, CxfInvokerUtils.getCXFRequestUri("https://localhost:8080/service/test/", method));
    }

    @Test
    public void buildCXFTransactionName_stripsProxyNumberToPreventMgi() {
        String generatedProxyTxnName = CxfInvokerUtils.buildCXFTransactionName(
                "com.sun.proxy.$Proxy243.EnterpriseSynergyGenerator", "someMethod");
        Assert.assertEquals("com.sun.proxy.$Proxy.EnterpriseSynergyGenerator/someMethod", generatedProxyTxnName);

        String noProxyTxnName = CxfInvokerUtils.buildCXFTransactionName(
                "com.foo.proxy", "someMethod");
        Assert.assertEquals("com.foo.proxy/someMethod", noProxyTxnName);
    }
}
