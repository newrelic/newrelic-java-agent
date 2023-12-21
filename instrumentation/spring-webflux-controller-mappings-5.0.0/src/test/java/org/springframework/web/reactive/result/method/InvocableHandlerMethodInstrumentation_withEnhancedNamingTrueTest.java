package org.springframework.web.reactive.result.method;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import com.nr.agent.instrumentation.web.reactive.TestControllerClasses;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;

import java.util.logging.Level;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvocableHandlerMethodInstrumentation_withEnhancedNamingTrueTest {
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
        when(mockConfig.getValue("class_transformer.enhanced_spring_transaction_naming", false)).thenReturn(true);
        when(mockAgent.getLogger()).thenReturn(mockLogger);
        when(mockLogger.isLoggable(Level.FINEST)).thenReturn(false);
    }

    @After
    public void after() {
        AgentBridge.agent = originalAgent;
    }

    @Test
    public void handleInternal_findsAnnotationsFromInterfaceAndMethod() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$ControllerClassWithInterface/get");
    }

    @Test
    public void handleInternal_findsAnnotationsWithUrlParamFromInterfaceAndMethod() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerClassWithInterface.class,
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("getParam"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$ControllerClassWithInterface/getParam");
    }

    @Test
    public void handleInternal_withRequestMappings_findsAnnotationsWithoutInterface() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }

    @Test
    public void handleInternal_withRequestMappingsAndUrlParam_findsAnnotationsWithoutInterface() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("get2"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$StandardControllerWithAllRequestMappings/get2");
    }

    @Test
    public void handleInternal_withPostMappings_findsAnnotationsWithoutInterface() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.StandardControllerWithAllRequestMappings.class,
                TestControllerClasses.StandardControllerWithAllRequestMappings.class.getMethod("post"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("POST");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/post (POST)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$StandardControllerWithAllRequestMappings/post");
    }

    @Test
    public void handleInternal_whenNoAnnotationPresent_namesTxnBasedOnControllerClassAndMethod() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.NoAnnotationController.class,
                TestControllerClasses.NoAnnotationController.class.getMethod("get"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/NoAnnotationController/get");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$NoAnnotationController/get");
    }

    @Test
    public void handleInternal_whenExtendingAbstractController_namesTxnBasedOnRouteAndHttpMethod() throws Exception {
        InvocableHandlerMethod_Instrumentation cut = new InvocableHandlerMethod_InstrumentationTestImpl(
                TestControllerClasses.ControllerExtendingAbstractClass.class,
                TestControllerClasses.ControllerExtendingAbstractClass.class.getMethod("extend"));

        ServerHttpRequest mockReq = mock(ServerHttpRequest.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockExchange.getRequest()).thenReturn(mockReq);
        when(mockReq.getMethodValue()).thenReturn("GET");

        //cut.handleInternal(mockReq, mockResp, handlerMethod);
        cut.invoke(mockExchange, mock(BindingContext.class));

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/extend (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.web.reactive.TestControllerClasses$ControllerExtendingAbstractClass/extend");
    }
}
