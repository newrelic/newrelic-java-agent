package com.nr.agent.instrumentation;

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
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractHandlerMethodAdapterInstrumentation_withEnhancedNamingTrueTest {
    Agent originalAgent = AgentBridge.getAgent();
    Agent mockAgent = mock(Agent.class);
    Logger mockLogger = mock(Logger.class);
    Config mockConfig = mock(Config.class);
    TracedMethod mockTracedMethod = mock(TracedMethod.class);


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
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$ControllerClassWithInterface/get");
    }

    @Test
    public void handleInternal_findsAnnotationsWithUrlParamFromInterfaceAndMethod() throws Exception {
        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.ControllerClassWithInterface(),
                TestControllerClasses.ControllerClassWithInterface.class.getMethod("getParam"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$ControllerClassWithInterface/getParam");

    }

    @Test
    public void handleInternal_withRequestMappings_findsAnnotationsWithoutInterface() throws Exception {
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
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/get");
    }

    @Test
    public void handleInternal_withRequestMappingsAndUrlParam_findsAnnotationsWithoutInterface() throws Exception {
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
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/get/{id} (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/get2");
    }

    @Test
    public void handleInternal_withPostMappings_findsAnnotationsWithoutInterface() throws Exception {
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
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController", "/root/post (POST)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$StandardControllerWithAllRequestMappings/post");
    }

    @Test
    public void handleInternal_whenNoAnnotationPresent_namesTxnBasedOnControllerClassAndMethod() throws Exception {
        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.NoAnnotationController(),
                TestControllerClasses.NoAnnotationController.class.getMethod("get"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController",
                "/NoAnnotationController/get");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$NoAnnotationController/get");
    }

    @Test
    public void handleInternal_whenExtendingAbstractController_namesTxnBasedOnRouteAndHttpMethod() throws Exception {
        AbstractHandlerMethodAdapter_Instrumentation cut = new AbstractHandlerMethodAdapter_Instrumentation();
        HandlerMethod handlerMethod = new HandlerMethod(new TestControllerClasses.ControllerExtendingAbstractClass(),
                TestControllerClasses.ControllerExtendingAbstractClass.class.getMethod("extend"));

        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        HttpServletResponse mockResp = mock(HttpServletResponse.class);
        Transaction mockTxn = mock(Transaction.class);

        when(mockAgent.getTransaction(false)).thenReturn(mockTxn);
        when(mockTxn.getTracedMethod()).thenReturn(mockTracedMethod);
        when(mockReq.getMethod()).thenReturn("GET");

        cut.handleInternal(mockReq, mockResp, handlerMethod);

        verify(mockTxn).getTracedMethod();
        verify(mockTxn).setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "SpringController",
                "/root/extend (GET)");
        verify(mockTracedMethod).setMetricName("Java", "com.nr.agent.instrumentation.TestControllerClasses$ControllerExtendingAbstractClass/extend");
    }
}
