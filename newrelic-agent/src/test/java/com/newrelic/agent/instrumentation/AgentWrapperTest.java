package com.newrelic.agent.instrumentation;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.EntryInvocationHandler;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.TracerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AgentWrapperTest {

    private static AgentWrapper target;

    private static TracerService tracerService = Mockito.mock(TracerService.class);

    @Test
    public void test_invoke_CLASSLOADER_KEYasProxy() {
        setupServices(true, false);
        Assert.assertEquals(AgentBridge.getAgent().getClass().getClassLoader(),
                target.invoke(AgentWrapper.CLASSLOADER_KEY, null, null));
    }

    @Test
    public void test_invoke_coreServiceNotEnabled() {
        setupServices(false, false);
        Assert.assertEquals(NoOpInvocationHandler.INVOCATION_HANDLER,
                target.invoke(null, null, null));
    }

    @Test
    public void test_invoke_proxyIsClass_ignoreTxNull() {
        setupServices(true, false);
        Assert.assertEquals(NoOpInvocationHandler.INVOCATION_HANDLER,
                target.invoke(AgentWrapperTest.class, null, null));
    }

    @Test
    public void test_invoke_proxyIsClass_ignoreTxTrue() {
        setupServices(true, false);
        Object result = target.invoke(AgentWrapperTest.class, null, new Object[]{null, null, null, null, true});
        Assert.assertTrue(result.getClass().getName().contains("IgnoreTransactionHandler"));
    }

    @Test
    public void test_invoke_proxyIsClass_ignoreTxFalse() {
        setupServices(true, false);
        Object result = target.invoke(AgentWrapperTest.class, null, new Object[]{null, null, null, null, false});
        Assert.assertEquals(NoOpInvocationHandler.INVOCATION_HANDLER, result);
    }

    @Test
    public void test_invoke_proxyIsInteger_withEntryInvocationHandler() {
        setupServices(true, false);
        Assert.assertNull(target.invoke(1, null, new Object[]{"className", "methodName", "methodDesc", null, null}));
    }

    @Test
    public void test_invoke_proxyIsInteger_withTracerFactory() {
        setupServices(true, true);
        Object result = target.invoke(1, null, new Object[]{"className", "methodName", "methodDesc", null, null});
        Assert.assertTrue(result instanceof Tracer);
    }

    private void setupServices(boolean coreServiceEnabled, boolean withTracerFactory) {
        InstrumentationProxy instrProxy = Mockito.mock(InstrumentationProxy.class);

        MockServiceManager serviceManager = new MockServiceManager();

        CoreService coreService = Mockito.mock(CoreService.class);
        Mockito.when(coreService.isEnabled()).thenReturn(coreServiceEnabled);
        Mockito.when(coreService.getInstrumentation()).thenReturn(instrProxy);
        serviceManager.setCoreService(coreService);

        Mockito.when(tracerService.getTracer(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mockito.mock(Tracer.class));
        PointCutInvocationHandler pointCutInvocationHandler = withTracerFactory ?
                Mockito.mock(TracerFactory.class) :
                Mockito.mock(EntryInvocationHandler.class);
        Mockito.when(tracerService.getInvocationHandler(Mockito.anyInt())).thenReturn(pointCutInvocationHandler);
        serviceManager.setTracerService(tracerService);

        ServiceFactory.setServiceManager(serviceManager);

        PointCutClassTransformer classTransformer = new PointCutClassTransformer(instrProxy, false);
        target = AgentWrapper.getAgentWrapper(classTransformer);
    }

}
