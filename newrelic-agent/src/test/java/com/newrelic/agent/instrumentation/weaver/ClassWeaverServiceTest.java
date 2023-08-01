package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentJarHelper;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.logging.AgentLogManager;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.bootstrap.BootstrapAgent;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

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

    private static void createServiceManager(Set<File> weaveExtensions) throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);
        serviceManager.start();

        AgentConfig agentConfig = AgentHelper.createAgentConfig(true, new HashMap<>(), Collections.<String, Object>emptyMap());

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, new HashMap<>());
        serviceManager.setConfigService(configService);

        TransactionTraceService transactionTraceService = new TransactionTraceService();
        serviceManager.setTransactionTraceService(transactionTraceService);

        if (weaveExtensions != null) {
            ExtensionService mockExtensionService = Mockito.mock(ExtensionService.class);
            Mockito.when(mockExtensionService.getWeaveExtensions()).thenReturn(weaveExtensions);
            serviceManager.setExtensionService(mockExtensionService);
        }
    }

}
