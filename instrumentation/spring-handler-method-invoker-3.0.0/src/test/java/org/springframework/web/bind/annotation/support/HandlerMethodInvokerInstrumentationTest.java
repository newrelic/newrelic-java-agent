/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.bind.annotation.support;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class HandlerMethodInvokerInstrumentationTest {

    private final Agent originalAgent = AgentBridge.getAgent();
    private final Agent mockAgent = mock(Agent.class);
    private final Transaction mockTransaction = mock(Transaction.class);
    private final TracedMethod mockTracedMethod = mock(TracedMethod.class);

    @Before
    public void setUp() {
        AgentBridge.agent = mockAgent;
    }

    @After
    public void tearDown() {
        AgentBridge.agent = originalAgent;
    }

    @Test
    public void invokeHandlerMethod_setsMetricName() throws Exception {
        when(mockAgent.getTransaction(false)).thenReturn(mockTransaction);
        when(mockTransaction.getTracedMethod()).thenReturn(mockTracedMethod);

        HandlerMethodInvoker_Instrumentation invoker = new HandlerMethodInvoker_Instrumentation();
        Method method = TestController.class.getMethod("handleRequest");

        invoker.invokeHandlerMethod(method, new TestController(), null, null);

        verify(mockTracedMethod).setMetricName("Spring", "Java",
                "org.springframework.web.bind.annotation.support.HandlerMethodInvokerInstrumentationTest$TestController",
                "handleRequest");
    }

    @Test
    public void invokeHandlerMethod_noTransaction_doesNotCrash() throws Exception {
        when(mockAgent.getTransaction(false)).thenReturn(null);

        HandlerMethodInvoker_Instrumentation invoker = new HandlerMethodInvoker_Instrumentation();
        Method method = TestController.class.getMethod("handleRequest");

        invoker.invokeHandlerMethod(method, new TestController(), null, null);

        verifyNoInteractions(mockTracedMethod);
    }

    public static class TestController {
        public String handleRequest() {
            return "response";
        }
    }
}