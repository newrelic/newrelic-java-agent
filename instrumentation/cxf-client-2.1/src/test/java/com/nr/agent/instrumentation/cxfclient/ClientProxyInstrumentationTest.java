/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.cxfclient;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.api.agent.Trace;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "org.apache.cxf.frontend" })
public class ClientProxyInstrumentationTest {

    @Test
    public void invokeSync_setsJavaMetricNameFromDeclaringClassAndMethod() throws Exception {
        invokeSampleService();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        String transactionName = "OtherTransaction/Custom/" + getClass().getName() + "/invokeSampleService";
        Map<String, TracedMetricData> metricsForTransaction = introspector.getMetricsForTransaction(transactionName);

        String expectedMetric = "Java/" + SampleService.class.getName() + "/doSomething";
        Assert.assertTrue(metricsForTransaction.containsKey(expectedMetric));
    }

    @Trace(dispatcher = true)
    private void invokeSampleService() throws Exception {
        Client mockClient = Mockito.mock(Client.class);
        Mockito.when(mockClient.invoke(Mockito.any(BindingOperationInfo.class), Mockito.any(Object[].class)))
                .thenReturn(new Object[] { "result" });

        ClientProxy clientProxy = new ClientProxy(mockClient);
        Method method = SampleService.class.getMethod("doSomething");
        BindingOperationInfo oi = Mockito.mock(BindingOperationInfo.class);

        clientProxy.invokeSync(method, oi, new Object[0]);
    }

    private interface SampleService {
        void doSomething();
    }
}
