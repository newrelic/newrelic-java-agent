/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package org.springframework.web.reactive.result.method;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import java.util.logging.Level;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvocableHandlerMethodInstrumentationEnhancedNamingConfigTest_webflux500 {
    Agent originalAgent = AgentBridge.getAgent();
    Agent mockAgent = mock(Agent.class);
    Logger mockLogger = mock(Logger.class);
    Config mockConfig = mock(Config.class);
    TracedMethod mockTracedMethod = mock(TracedMethod.class);
    ServerWebExchange mockExchange = mock(ServerWebExchange.class);


    @Before
    public void before() {
        AgentBridge.agent = mockAgent;
        when(mockAgent.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(false);
        when(mockAgent.getLogger()).thenReturn(mockLogger);
        when(mockLogger.isLoggable(Level.FINEST)).thenReturn(false);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
    }

    //
    // class_transformer.enhanced_spring_transaction_naming = false
    //
    @Test
    public void handleInternal_findsAnnotationsFromInterfaceAndMethod_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/ControllerClassWithInterface/get");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerClassWithInterface/get");
    }

    @Test
    public void handleInternal_findsAnnotationsWithUrlParamFromInterfaceAndMethod_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("getParam"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/ControllerClassWithInterface/getParam");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerClassWithInterface/getParam");
    }

    @Test
    public void handleInternal_withRequestMappings_findsAnnotationsWithoutInterface_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }

    @Test
    public void handleInternal_withRequestMappingsAndUrlParam_findsAnnotationsWithoutInterface_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get2"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/get2");
    }

    @Test
    public void handleInternal_withPostMappings_findsAnnotationsWithoutInterface_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("post"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.POST);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/post (POST)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/post");
    }

    @Test
    public void handleInternal_whenNoAnnotationPresent_namesTxnBasedOnControllerClassAndMethod_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.NoAnnotationController.class,
                TestControllerClasses.NoAnnotationController.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/NoAnnotationController/get");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$NoAnnotationController/get");
    }

    @Test
    public void handleInternal_whenExtendingAbstractController_namesTxnBasedOnRouteAndHttpMethod_false() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = false;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerExtendingAbstractClass.class,
                TestControllerClasses.ControllerExtendingAbstractClass.class.getMethod("extend"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/ControllerExtendingAbstractClass/extend");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerExtendingAbstractClass/extend");
    }

    //
    // class_transformer.enhanced_spring_transaction_naming = true
    //

    @Test
    public void handleInternal_findsAnnotationsFromInterfaceAndMethod_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerClassWithInterface/get");
    }

    @Test
    public void handleInternal_findsAnnotationsWithUrlParamFromInterfaceAndMethod_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("getParam"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerClassWithInterface/getParam");
    }

    @Test
    public void handleInternal_withRequestMappings_findsAnnotationsWithoutInterface_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }

    @Test
    public void handleInternal_withRequestMappingsAndUrlParam_findsAnnotationsWithoutInterface_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get2"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/get2");
    }

    @Test
    public void handleInternal_withPostMappings_findsAnnotationsWithoutInterface_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("post"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.POST);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/post (POST)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$StandardControllerWithAllRequestMappings/post");
    }

    @Test
    public void handleInternal_whenNoAnnotationPresent_namesTxnBasedOnControllerClassAndMethod_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.NoAnnotationController.class,
                TestControllerClasses.NoAnnotationController.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/NoAnnotationController/get");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$NoAnnotationController/get");
    }

    @Test
    public void handleInternal_whenExtendingAbstractController_namesTxnBasedOnRouteAndHttpMethod_true() throws Exception {
        SpringControllerUtility.ENHANCED_NAMING_ENABLED = true;
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerExtendingAbstractClass.class,
                TestControllerClasses.ControllerExtendingAbstractClass.class.getMethod("extend"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethod()).thenReturn(HttpMethod.GET);

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/extend (GET)");
        verify(mockTracedMethod).setMetricName("Spring", "Java", "org.springframework.web.reactive.result.method.TestControllerClasses$ControllerExtendingAbstractClass/extend");
    }
}
