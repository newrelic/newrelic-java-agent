/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ClassTransformerConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.core.CoreService;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.samplers.SamplerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class InstrumentationImplTest {
    private static ClassTransformerService classTransformerService = Mockito.mock(ClassTransformerService.class);
    private static SamplerService samplerService = Mockito.mock(SamplerService.class);
    private static CoreService mockCoreService = Mockito.mock(CoreService.class);
    private static InstrumentationProxy instrumentation = Mockito.mock(InstrumentationProxy.class);

    private final Instrumentation api = new InstrumentationImpl(Agent.LOG);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.setClassTransformerService(classTransformerService);
        serviceManager.setSamplerService(samplerService);
        serviceManager.setCoreService(mockCoreService);
        ConfigService configService = mock(ConfigService.class);
        serviceManager.setConfigService(configService);
        AgentConfigImpl agentConfig = mock(AgentConfigImpl.class);
        when(configService.getDefaultAgentConfig()).thenReturn(agentConfig);
        ClassTransformerConfig classTransformerConfig = mock(ClassTransformerConfig.class);
        when(agentConfig.getClassTransformerConfig()).thenReturn(classTransformerConfig);
    }

    @AfterClass
    public static void tearDownAfterClass() {
        ServiceFactory.setServiceManager(null);
    }

    @Before
    public void resetMocks() {
        Mockito.reset(mockCoreService);
        Mockito.reset(instrumentation);
        Mockito.reset(classTransformerService);
        Mockito.reset(samplerService);
        Mockito.when(mockCoreService.getInstrumentation()).thenReturn(instrumentation);
    }

    @After
    public void verifyAll() {
        Mockito.verifyNoMoreInteractions(mockCoreService);
        Mockito.verifyNoMoreInteractions(instrumentation);
        Mockito.verifyNoMoreInteractions(classTransformerService);
    }

    @Test
    public void findClasses() {
        when(instrumentation.getAllLoadedClasses()).thenReturn(
                new Class[] { InstrumentationImplTest.class });
        Set<Class<?>> classes = InstrumentationImpl.findClasses(true, InstrumentationImplTest.class.getName());
        assertEquals(1, classes.size());
        Mockito.verify(mockCoreService, times(1)).getInstrumentation();
        Mockito.verify(instrumentation, times(1)).getAllLoadedClasses();
    }

    @Test
    public void findClassesFirstMatch() {
        when(instrumentation.getAllLoadedClasses()).thenReturn(
                new Class[] { InstrumentationImplTest.class });
        Set<Class<?>> classes = InstrumentationImpl.findClasses(false, InstrumentationImplTest.class.getName());
        assertEquals(1, classes.size());
        Mockito.verify(mockCoreService, times(1)).getInstrumentation();
        Mockito.verify(instrumentation, times(1)).getAllLoadedClasses();
    }

    @Test
    public void testDisableAuto() {
        ClassTransformerConfig config = mock(ClassTransformerConfig.class);
        when(config.getAutoAsyncLinkRateLimit()).thenReturn(0L);
        InstrumentationImpl instrumentation = new InstrumentationImpl(mock(Logger.class), config);
        assertFalse(instrumentation.autoInstrumentCheck.get());
    }

    @Test
    public void getAutoAsyncLinkRateLimitInSeconds() {
        ClassTransformerConfig config = mock(ClassTransformerConfig.class);
        when(config.getAutoAsyncLinkRateLimit()).thenReturn(500L);
        assertEquals(0.5f, InstrumentationImpl.getAutoAsyncLinkRateLimitInSeconds(config), 0.001);
    }

    @Test
    public void testInstrument() {
        api.instrument("com.newrelic.TestClass", "TEST");
        Mockito.verify(classTransformerService).addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class),
                Mockito.eq("TEST"));
    }

    @Test
    public void testInstrumentClass() throws Exception {
        Mockito.when(
                classTransformerService.addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class),
                        Mockito.any(TraceDetails.class))).thenReturn(true);
        api.instrument(TestClass.class.getMethod("regularMethod"), "TEST");
        ArgumentCaptor<TraceDetails> traceDetailsCapture = ArgumentCaptor.forClass(TraceDetails.class);
        Mockito.verify(classTransformerService).addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class),
                traceDetailsCapture.capture());
        assertEquals("TEST", traceDetailsCapture.getValue().metricPrefix());
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(samplerService).addSampler(captor.capture(), Mockito.anyLong(), Mockito.any(TimeUnit.class));
        Runnable runnable = captor.getValue();
        runnable.run();
        Mockito.verify(mockCoreService).getInstrumentation();
        Mockito.verify(instrumentation).retransformClasses(Mockito.eq(TestClass.class));
    }

    @Test
    public void testInstrumentAbstractMethodNoRetransform() throws Exception {
        Mockito.when(
                classTransformerService.addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class), Mockito.eq("TEST"))).thenReturn(
                true);
        api.instrument(TestClass.class.getMethod("abstractMethod"), "TEST");
        // No interaction verified by verifyAll()
    }

    @Test
    public void testInstrumentInterfaceMethodNoRetransform() throws Exception {
        Mockito.when(
                classTransformerService.addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class), Mockito.eq("TEST"))).thenReturn(
                true);
        api.instrument(TestInterface.class.getMethod("interfaceMethod"), "TEST");
        // No interaction verified by verifyAll()
    }

    @Test
    public void testInstrumentClassNoRetransform() throws Exception {
        api.instrument(TestClass.class.getMethod("regularMethod"), "TEST");
        Mockito.verify(classTransformerService).addTraceMatcher(Mockito.any(ClassAndMethodMatcher.class),
                Mockito.any(TraceDetails.class));
    }

    @Test
    public void testDontReInstrumentClass() throws Exception {
        api.instrument(AnnotatedClass.class.getMethod("annotatedMethod"), "TEST");
        // No interaction verified by verifyAll()
    }

    @Test
    public void testRetransformUninstrumentedClass() throws Exception {
        api.retransformUninstrumentedClass(getClass());
        Mockito.verify(mockCoreService).getInstrumentation();
        Mockito.verify(instrumentation).retransformClasses(Mockito.eq(getClass()));
        api.retransformUninstrumentedClass(AnnotatedClass.class);
        // Should not interact with mocks. Verified using verifyAll().
    }

    @InstrumentedClass
    private class AnnotatedClass {

        @InstrumentedMethod(instrumentationTypes = InstrumentationType.TraceAnnotation, instrumentationNames = "theName")
        public void annotatedMethod() {

        }
    }

    private abstract class TestClass {
        @SuppressWarnings("unused")
        public void regularMethod() {

        }

        public abstract void abstractMethod();
    }

    private interface TestInterface {
        void interfaceMethod();
    }
}
