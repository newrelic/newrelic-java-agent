/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.method.HandlerMethod;

import java.util.logging.Level;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UseControllerClassForSpringTransactionNamingConfigTest {
    Agent originalAgent = AgentBridge.getAgent();
    Agent mockAgent = mock(Agent.class);
    Logger mockLogger = mock(Logger.class);
    Config mockConfig = mock(Config.class);
    TracedMethod mockTracedMethod = mock(TracedMethod.class);

    @Before
    public void before() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getConfig()).thenReturn(mockConfig);
        when(mockAgent.getLogger()).thenReturn(mockLogger);
        when(mockLogger.isLoggable(Level.FINEST)).thenReturn(false);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
    }

    // class_transformer.use_controller_class_and_method_for_spring_transaction_naming set to true
    // Should always use ControllerClassName/methodName format regardless of enhanced_spring_transaction_naming

    @Test
    public void handleInternal_useControllerClassEnabled_namesWithControllerClassAndMethod() throws Exception {
        when(mockConfig.getValue("class_transformer.use_controller_class_and_method_for_spring_transaction_naming", false)).thenReturn(true);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(false);

        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.StandardControllerWithAllRequestMappings(),
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        // Should use controller class + method name format with slash delimiter, not request mapping format
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/StandardControllerWithAllRequestMappings/get");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }

    @Test
    public void handleInternal_useControllerClassEnabled_overridesEnhancedNaming() throws Exception {
        // Even with enhanced_spring_transaction_naming=true, use_controller_class_for_spring_transaction_naming should take precedence
        when(mockConfig.getValue("class_transformer.use_controller_class_and_method_for_spring_transaction_naming", false)).thenReturn(true);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(true);

        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.ControllerClassWithInterface(),
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("get"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        // Should use controller class + method name format, not the enhanced naming format "/root/get (GET)"
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/ControllerClassWithInterface/get");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "com.nr.agent.instrumentation.TestControllerClasses$ControllerClassWithInterface/get");
    }

    @Test
    public void handleInternal_useControllerClassEnabled_withUrlParams() throws Exception {
        when(mockConfig.getValue("class_transformer.use_controller_class_and_method_for_spring_transaction_naming", false)).thenReturn(true);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(true);

        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.StandardControllerWithAllRequestMappings(),
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get2"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        // Should use controller class + method name format, not "/root/get/{id} (GET)"
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/StandardControllerWithAllRequestMappings/get2");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/get2");
    }

    @Test
    public void handleInternal_useControllerClassEnabled_withPostMapping() throws Exception {
        when(mockConfig.getValue("class_transformer.use_controller_class_and_method_for_spring_transaction_naming", false)).thenReturn(true);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(true);

        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.StandardControllerWithAllRequestMappings(),
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("post"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("POST");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        // Should use controller class + method name format, not "/root/post (POST)"
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/StandardControllerWithAllRequestMappings/post");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/post");
    }

    @Test
    public void handleInternal_useControllerClassDisabled_usesLegacyBehavior() throws Exception {
        // When use_controller_class_for_spring_transaction_naming is false, should use legacy behavior
        when(mockConfig.getValue("class_transformer.use_controller_class_and_method_for_spring_transaction_naming", false)).thenReturn(false);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(true);

        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.StandardControllerWithAllRequestMappings(),
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        // Should use request mapping format since enhanced naming is true
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }
}
