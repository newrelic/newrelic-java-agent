/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.apache.cxf.service.invoker;

import java.lang.reflect.Method;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.model.EndpointInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AbstractInvokerInstrumentationTest {

    private final Agent originalAgent = AgentBridge.getAgent();
    private final Agent mockAgent = mock(Agent.class);
    private final Transaction mockTransaction = mock(Transaction.class);
    private final TracedMethod mockTracedMethod = mock(TracedMethod.class);

    @Before
    public void setUp() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getTransaction(false)).thenReturn(mockTransaction);
        when(mockTransaction.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockAgent.getLogger()).thenReturn(mock(Logger.class));
    }

    @After
    public void tearDown() {
        AgentBridge.agent = originalAgent;
    }

    @Test
    public void performInvocation_setsJavaMetricNameFromServiceAndMethod() throws Exception {
        AbstractInvoker_Instrumentation invoker = new AbstractInvoker_Instrumentation();
        Method method = SampleService.class.getMethod("doSomething");
        Exchange exchange = mockExchange(null);

        invoker.performInvocation(exchange, new SampleService(), method, new Object[0]);

        verify(mockTracedMethod).setMetricName("Java", SampleService.class.getName(), "doSomething");
    }

    @Test
    public void performInvocation_withResolvableAddress_namesTransactionFromRequestUri() throws Exception {
        AbstractInvoker_Instrumentation invoker = new AbstractInvoker_Instrumentation();
        Method method = SampleService.class.getMethod("doSomething");
        Exchange exchange = mockExchange("http://localhost:8080/service/sample/");

        invoker.performInvocation(exchange, new SampleService(), method, new Object[0]);

        verify(mockTransaction).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "CXF",
                "/service/sample/doSomething");
    }

    @Test
    public void performInvocation_withoutResolvableAddress_fallsBackToServiceAndMethodName() throws Exception {
        AbstractInvoker_Instrumentation invoker = new AbstractInvoker_Instrumentation();
        Method method = SampleService.class.getMethod("doSomething");
        Exchange exchange = mockExchange(null);

        invoker.performInvocation(exchange, new SampleService(), method, new Object[0]);

        verify(mockTransaction).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "CXF",
                SampleService.class.getName() + "/doSomething");
    }

    @Test
    public void performInvocation_noTransaction_doesNotCrash() throws Exception {
        when(mockAgent.getTransaction(false)).thenReturn(null);

        AbstractInvoker_Instrumentation invoker = new AbstractInvoker_Instrumentation();
        Method method = SampleService.class.getMethod("doSomething");
        Exchange exchange = mockExchange(null);

        invoker.performInvocation(exchange, new SampleService(), method, new Object[0]);

        verifyNoInteractions(mockTracedMethod);
    }

    private static Exchange mockExchange(String address) {
        Exchange exchange = mock(Exchange.class);
        if (address == null) {
            when(exchange.getEndpoint()).thenReturn(null);
            return exchange;
        }
        Endpoint endpoint = mock(Endpoint.class);
        EndpointInfo endpointInfo = mock(EndpointInfo.class);
        when(exchange.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        when(endpointInfo.getAddress()).thenReturn(address);
        return exchange;
    }

    public static class SampleService {
        public String doSomething() {
            return "result";
        }
    }
}
