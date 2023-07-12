package com.newrelic.agent.profile.method;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.core.CoreService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

public class MethodInfoFactoryTest {

    private static CoreService coreService;

    @BeforeClass
    public static void setup() throws Exception {
        coreService = MockCoreService.getMockAgentAndBootstrapTheServiceManager();
        InstrumentationProxy instrumentation = Mockito.mock(InstrumentationProxy.class);
        Mockito.when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[]{ MethodInfoFactoryTest.class });
        ((MockCoreService)coreService).setInstrumentation(instrumentation);
    }

    @Test
    public void getMethodInfo_classNotLoaded() {
        MethodInfoFactory factory = new MethodInfoFactory();
        Assert.assertNull(factory.getMethodInfo("NoExist", "method", 1));
    }

    @Test
    public void getMethodInfo_loadedRetrievedTwice() {
        MethodInfoFactory factory = new MethodInfoFactory();
        // get the first time to get it cached
        Assert.assertNotNull(factory.getMethodInfo(MethodInfoFactoryTest.class.getName(), "setup", 1));
        // get the second time to get from cache
        Assert.assertNotNull(factory.getMethodInfo(MethodInfoFactoryTest.class.getName(), "setup", 1));
    }

}
