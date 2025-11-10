package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.instrumentation.ClassTransformerService;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcherTest;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.custom.ClassRetransformer;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.instrumentation.weaver.preprocessors.TracedWeaveInstrumentationTracker;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.bootstrap.BootstrapAgent;
import com.newrelic.test.marker.RequiresFork;
import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

@Category(RequiresFork.class)
public class ClassWeaverServiceTest {

    private static MockedStatic<AgentLogManager> mockedAgentLogManager;
    private static IAgentLogger logger = Mockito.mock(IAgentLogger.class);

    @BeforeClass
    public static void beforeClass() throws Exception {
        Mockito.when(logger.getChildLogger(Mockito.any(Class.class))).thenReturn(Mockito.mock(IAgentLogger.class));
        mockedAgentLogManager = Mockito.mockStatic(AgentLogManager.class);
        mockedAgentLogManager.when(AgentLogManager::getLogger).thenReturn(logger);
    }

    @AfterClass
    public static void afterClass() {
        mockedAgentLogManager.close();
    }

    @Test
    public void test_registerInstrumentation_noJars() throws Exception {
        createServiceManager(null);
        try (MockedStatic<AgentJarHelper> mockedAgentJarHelper = Mockito.mockStatic(AgentJarHelper.class)) {
            mockedAgentJarHelper.when(() ->
                            AgentJarHelper.findAgentJarFileNames(Mockito.any()))
                    .thenReturn(Arrays.asList());
            Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
            ClassWeaverService target = new ClassWeaverService(instrumentation);
            target.registerInstrumentation();

            Assert.assertEquals(0, target.getWeavePackageManger().getRegisteredPackages().size());
            Mockito.verify(logger).log(Mockito.eq(Level.SEVERE), Mockito.contains("No instrumentation packages were found in the agent"));
        }

    }

    @Test
    public void test_registerInstrumentation_missingJars() throws Exception {
        createServiceManager(null);
        try (MockedStatic<AgentJarHelper> mockedAgentJarHelper = Mockito.mockStatic(AgentJarHelper.class)) {
            mockedAgentJarHelper.when(() ->
                            AgentJarHelper.findAgentJarFileNames(Mockito.any()))
                    .thenReturn(Arrays.asList("noexistant.jar"));
            Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
            ClassWeaverService target = new ClassWeaverService(instrumentation);
            target.registerInstrumentation();

            Assert.assertEquals(0, target.getWeavePackageManger().getRegisteredPackages().size());
            Mockito.verify(logger).error(Mockito.contains("Unable to find instrumentation jar"));
        }
    }

    @Test
    public void test_registerInstrumentation_validInstrumentationJar() throws Exception {
        createServiceManager(null);
        try (MockedStatic<AgentJarHelper> mockedAgentJarHelper = Mockito.mockStatic(AgentJarHelper.class)) {
            mockedAgentJarHelper.when(() ->
                            AgentJarHelper.findAgentJarFileNames(Mockito.any()))
                    .thenReturn(Arrays.asList(
                            "com/newrelic/agent/extension/jms-1.1-1.0.jar"
                    ));
            Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
            ClassWeaverService target = new ClassWeaverService(instrumentation);
            target.registerInstrumentation();

            Assert.assertNotNull(target.getWeavePackageManger().getWeavePackage("com.newrelic.instrumentation.jms-1.1"));
            Assert.assertEquals(1, target.getWeavePackageManger().getRegisteredPackages().size());
        }
    }

    @Test
    public void test_registerInstrumentation_noTitleInstrumentationJar() throws Exception {
        createServiceManager(null);
        try (MockedStatic<AgentJarHelper> mockedAgentJarHelper = Mockito.mockStatic(AgentJarHelper.class)) {
            mockedAgentJarHelper.when(() ->
                            AgentJarHelper.findAgentJarFileNames(Mockito.any()))
                    .thenReturn(Arrays.asList(
                            "com/newrelic/agent/service/module/jarTest.jar"
                    ));
            Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
            ClassWeaverService target = new ClassWeaverService(instrumentation);
            target.registerInstrumentation();

            Assert.assertNull(target.getWeavePackageManger().getWeavePackage("com.newrelic.agent.extension.service.module.jarTest"));
            Assert.assertEquals(0, target.getWeavePackageManger().getRegisteredPackages().size());
            Mockito.verify(logger).log(Mockito.eq(Level.FINER), Mockito.any(Throwable.class), Mockito.contains("unable to load weave package jar"), Mockito.any(Object.class));
        }
    }

    @Test
    public void test_registerInstrumentation_validExternalJar() throws Exception {
        URL regularJmsUrl = BootstrapAgent.class.getResource("/com/newrelic/agent/extension/jms-1.1-1.0.jar");
        URL springJmsUrl = BootstrapAgent.class.getResource("/com/newrelic/agent/extension/spring-jms-2-1.0.jar");
        File regularJmsFile = new File(regularJmsUrl.getPath());
        File springJmsFile = new File(springJmsUrl.getPath());
        createServiceManager(new HashSet<>(Arrays.asList(regularJmsFile)));

        try (MockedStatic<AgentJarHelper> mockedAgentJarHelper = Mockito.mockStatic(AgentJarHelper.class)) {
            mockedAgentJarHelper.when(() ->
                            AgentJarHelper.findAgentJarFileNames(Mockito.any()))
                    .thenReturn(Arrays.asList());
            Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
            ClassWeaverService target = new ClassWeaverService(instrumentation);
            target.registerInstrumentation();

            // should be in the weave package manager now
            Assert.assertNotNull(target.getWeavePackageManger().getWeavePackage("com.newrelic.instrumentation.jms-1.1"));
            Assert.assertEquals(1, target.getWeavePackageManger().getRegisteredPackages().size());
            // AND should be in the external
            // but we have no way to validate that

            // now reload with a different jar file
            target.reloadExternalWeavePackages(Arrays.asList(springJmsFile), Arrays.asList(regularJmsFile));
            Assert.assertNull(target.getWeavePackageManger().getWeavePackage("com.newrelic.instrumentation.jms-1.1"));
            Assert.assertNotNull(target.getWeavePackageManger().getWeavePackage("com.newrelic.instrumentation.spring-jms-2"));
            Assert.assertEquals(1, target.getWeavePackageManger().getRegisteredPackages().size());
        }
    }

    @Test
    public void test_newClassMatchVisitor_notRetransforming() throws Exception {
        createServiceManager(new HashSet<>(Arrays.asList()));

        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        ClassWeaverService target = new ClassWeaverService(instrumentation);

        InstrumentationContext iInstrumentationContext = Mockito.mock(InstrumentationContext.class);
        Class<?> clazz = this.getClass();
        ClassReader reader = ClassMatcherTest.getClassReader(clazz);

        ClassVisitor result = target.newClassMatchVisitor(clazz.getClassLoader(), null, reader, null,
                iInstrumentationContext);
        Mockito.verify(iInstrumentationContext).putMatch(target, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_newClassMatchVisitor_isRetransforming() throws Exception {
        createServiceManager(new HashSet<>(Arrays.asList()), true);

        Instrumentation instrumentation = Mockito.mock(Instrumentation.class);
        ClassWeaverService target = new ClassWeaverService(instrumentation);

        InstrumentationContext iInstrumentationContext = Mockito.mock(InstrumentationContext.class);
        Class<?> clazz = this.getClass();
        ClassReader reader = ClassMatcherTest.getClassReader(clazz);

        startedRetransforming = false;
        Runnable retransformer = target.createRetransformRunnable(null);
        Thread thread = new Thread(retransformer);
        thread.start();
        while (!startedRetransforming) {
            Thread.sleep(5);
        }
        ClassVisitor result = target.newClassMatchVisitor(clazz.getClassLoader(), null, reader, null,
                iInstrumentationContext);

        Mockito.verify(iInstrumentationContext, Mockito.times(0)).putMatch(target, null);
        Assert.assertNull(result);

        doneWithTestWork = true;
    }

    @Test
    public void test_addTraceInformation() throws Exception {
        createServiceManager(new HashSet<>(Arrays.asList()), true);

        String weavePackageName = "weavePackageName";
        String className = "originalClassName";
        ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> weaveTraceDetails = new ConcurrentHashMap<>();
        TraceDetails traceDetails = Mockito.mock(TraceDetails.class);
        TracedWeaveInstrumentationTracker traceDetailsTracker = new TracedWeaveInstrumentationTracker(
                weavePackageName, className, null, true, traceDetails);
        weaveTraceDetails.put(weavePackageName, new HashSet<>(Arrays.asList(traceDetailsTracker)));

        InstrumentationContext context = Mockito.mock(InstrumentationContext.class);
        byte[] classBytes = WeaveTestUtils.getClassBytes(SimpleInstrumentation.class.getName());
        ClassNode classNode = WeaveUtils.convertToClassNode(classBytes);

        ClassWeaverService.addTraceInformation(weaveTraceDetails, weavePackageName, context,
                classNode, className);

        Mockito.verify(context).addTrace(Mockito.any(), Mockito.any());
    }

    private void createServiceManager(Set<File> weaveExtensions) throws Exception {
        createServiceManager(weaveExtensions, false);
    }

    private void createServiceManager(Set<File> weaveExtensions, boolean createDelayingClassTransformerService) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, new HashMap<>(), Collections.<String, Object>emptyMap());

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, new HashMap<>());
        serviceManager.setConfigService(configService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        doneWithTestWork = false;
        serviceManager.setClassTransformerService(new DelayingClassTransformerService());

        if (weaveExtensions != null) {
            ExtensionService mockExtensionService = Mockito.mock(ExtensionService.class);
            Mockito.when(mockExtensionService.getWeaveExtensions()).thenReturn(weaveExtensions);
            serviceManager.setExtensionService(mockExtensionService);
        }
    }

    @Weave
    private class SimpleInstrumentation  {
        @WeaveIntoAllMethods
        public void simpleMethod() {

        }
    }

    private boolean startedRetransforming = false;
    private boolean doneWithTestWork = true;

    private class DelayingClassTransformerService implements ClassTransformerService {

        @Override
        public PointCutClassTransformer getClassTransformer() {
            return null;
        }

        @Override
        public ClassRetransformer getLocalRetransformer() {
            return null;
        }

        @Override
        public ClassRetransformer getRemoteRetransformer() {
            return null;
        }

        @Override
        public void checkShutdown() {

        }

        @Override
        public InstrumentationContextManager getContextManager() {
            return null;
        }

        @Override
        public boolean addTraceMatcher(ClassAndMethodMatcher matcher, String metricPrefix) {
            return false;
        }

        @Override
        public boolean addTraceMatcher(ClassAndMethodMatcher matcher, TraceDetails traceDetails) {
            return false;
        }

        @Override
        public void retransformMatchingClasses(Collection<ClassMatchVisitorFactory> classMatchers) {

        }

        @Override
        public void retransformMatchingClassesImmediately(Class<?>[] loadedClasses, Collection<ClassMatchVisitorFactory> classMatchers) {
            try {
                startedRetransforming = true;
                long start = System.currentTimeMillis();
                while (!doneWithTestWork && (System.currentTimeMillis()-start) < 10000) {
                    // max wait 10 seconds
                    Thread.sleep(5);
                }
            } catch (Exception e) {}
        }

        @Override
        public Instrumentation getExtensionInstrumentation() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void start() throws Exception {

        }

        @Override
        public void stop() throws Exception {

        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public IAgentLogger getLogger() {
            return null;
        }

        @Override
        public boolean isStarted() {
            return false;
        }

        @Override
        public boolean isStopped() {
            return false;
        }

        @Override
        public boolean isStartedOrStarting() {
            return false;
        }

        @Override
        public boolean isStoppedOrStopping() {
            return false;
        }
    }
}
