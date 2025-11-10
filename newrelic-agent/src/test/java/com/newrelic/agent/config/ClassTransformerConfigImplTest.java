/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.instrumentation.PointCutConfiguration;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.instrumentation.pointcuts.frameworks.spring.SpringPointCut;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.service.module.JarCollectorService;
import com.newrelic.api.agent.Logger;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newrelic.agent.config.ConfigHelper.buildConfigMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassTransformerConfigImplTest {

    @Test
    public void isEnabled() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, false);
        assertTrue(config.isEnabled());

        classTransformerMap.put("enabled", false);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Assert.assertFalse(config.isEnabled());
    }

    @Test
    public void isInstrumentationDefaultEnabled() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        Map<String, Object> instDefault = new HashMap<>();
        Map<String, Object> builtinExt = new HashMap<>();

        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Assert.assertTrue(config.isDefaultInstrumentationEnabled());
        Assert.assertTrue(config.isBuiltinExtensionEnabled());

        instDefault.put("enabled", false);
        classTransformerMap.put(ClassTransformerConfigImpl.DEFAULT_INSTRUMENTATION, instDefault);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);

        Assert.assertFalse(config.isDefaultInstrumentationEnabled());
        Assert.assertFalse(config.isBuiltinExtensionEnabled());

        builtinExt.put("enabled", true);
        classTransformerMap.put(ClassTransformerConfigImpl.BUILTIN_EXTENSIONS, builtinExt);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);

        Assert.assertFalse(config.isDefaultInstrumentationEnabled());
        Assert.assertTrue(config.isBuiltinExtensionEnabled());
    }

    @Test
    public void isCustomTracingEnabled() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, false);
        assertTrue(config.isCustomTracingEnabled());
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, false, false, false);
        Assert.assertFalse(config.isCustomTracingEnabled());
    }

    @Test
    public void shutdownDelayInNanos() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, false);
        Assert.assertEquals(-1L, config.getShutdownDelayInNanos());

        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        classTransformerMap.put("shutdown_delay", 60);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Assert.assertEquals(60000000000L, config.getShutdownDelayInNanos());

        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        classTransformerMap.put("shutdown_delay", 44.10);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Assert.assertEquals(44000000000L, config.getShutdownDelayInNanos());
    }

    @Test
    public void includes() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, false);
        Assert.assertEquals(0, config.getIncludes().size());

        classTransformerMap.put(
                ClassTransformerConfigImpl.INCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper, test/ExcludeTest$ExcludeTestInner, org/jruby/rack/RackEnvironment");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Set<String> includes = config.getIncludes();
        Assert.assertEquals(3, includes.size());
        assertTrue(includes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
        assertTrue(includes.contains("test/ExcludeTest$ExcludeTestInner"));
        assertTrue(includes.contains("org/jruby/rack/RackEnvironment"));

        classTransformerMap.put(ClassTransformerConfigImpl.INCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        includes = config.getIncludes();
        Assert.assertEquals(1, includes.size());
        assertTrue(includes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
    }

    private static class BadClassLoader extends ClassLoader {
    }

    private static class BadClassLoaderOther extends ClassLoader {
    }

    private static class GoodClassLoader extends ClassLoader {
    }

    @Test
    public void classloaderExcludes() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        final String prefix = ClassTransformerConfigImplTest.class.getName();
        classTransformerMap.put(
                ClassTransformerConfigImpl.CLASSLOADER_EXCLUDES,
                prefix + "$BadClassLoader, " + prefix + "$BadClassLoaderOther");

        Map<String, Object> configMap = new HashMap<>();
        configMap.put("class_transformer", classTransformerMap);
        configMap.put(AgentConfigImpl.APP_NAME, "unittest");
        AgentConfig config = AgentConfigImpl.createAgentConfig(configMap);
        ClassTransformerConfig classTransformerConfig = config.getClassTransformerConfig();

        classTransformerConfig = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Set<String> exclusions = classTransformerConfig.getClassloaderExclusions();
        Assert.assertEquals(8, exclusions.size()); // 6 included by default ClassTransformerConfigImpl#initializeClassloaderExcludes

        ConfigService configService = ConfigServiceFactory.createConfigService(config, Collections.<String, Object>emptyMap());
        MockServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);
        JarCollectorService mockJarCollector = serviceManager.getJarCollectorService();

        InstrumentationContextManager icm = new InstrumentationContextManager(Mockito.mock(InstrumentationProxy.class));
        Assert.assertFalse(icm.isClassloaderExcluded(new GoodClassLoader()));
        assertTrue(icm.isClassloaderExcluded(new BadClassLoader()));
        assertTrue(icm.isClassloaderExcluded(new BadClassLoaderOther()));
    }

    @Test
    public void defaultClassloaderExcludes() throws ConfigurationException, ForceDisconnectException {
        /*
         * Verify the newrelic.yml file we ship with the agent includes these classloaders in the exclusions.
         */

        // This should be newrelic-agent/src/main/resources/newrelic.yml
        URL defaultYml = getClass().getResource("/newrelic.yml");

        System.setProperty("newrelic.config.file", defaultYml.getPath());
        ConfigService configService = ConfigServiceFactory.createConfigService(mock(Logger.class), false);
        AgentConfig agentConfig = configService.getAgentConfig("app_name");
        Set<String> classloaderExclusions = agentConfig.getClassTransformerConfig().getClassloaderExclusions();

        // https://newrelic.zendesk.com/agent/tickets/194649
        // https://newrelic.zendesk.com/agent/tickets/196500
        assertTrue(classloaderExclusions.contains("groovy.lang.GroovyClassLoader$InnerLoader"));
        assertTrue(classloaderExclusions.contains("org.codehaus.groovy.runtime.callsite.CallSiteClassLoader"));

        // https://newrelic.zendesk.com/agent/tickets/192949
        assertTrue(classloaderExclusions.contains("com.collaxa.cube.engine.deployment.BPELClassLoader"));

        // https://newrelic.zendesk.com/agent/tickets/190213
        assertTrue(classloaderExclusions.contains("org.springframework.data.convert.ClassGeneratingEntityInstantiator$ObjectInstantiatorClassGenerator"));

        // https://newrelic.zendesk.com/agent/tickets/196151
        assertTrue(classloaderExclusions.contains("org.mvel2.optimizers.impl.asm.ASMAccessorOptimizer$ContextClassLoader"));

        // https://newrelic.zendesk.com/agent/tickets/194073
        assertTrue(classloaderExclusions.contains("gw.internal.gosu.compiler.SingleServingGosuClassLoader"));
    }

    @Test
    public void excludes() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, false);
        Assert.assertEquals(0, config.getExcludes().size());

        classTransformerMap.put(
                ClassTransformerConfigImpl.EXCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper, test/ExcludeTest$ExcludeTestInner, org/jruby/rack/RackEnvironment");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        Set<String> excludes = config.getExcludes();
        Assert.assertEquals(3, excludes.size());
        assertTrue(excludes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
        assertTrue(excludes.contains("test/ExcludeTest$ExcludeTestInner"));
        assertTrue(excludes.contains("org/jruby/rack/RackEnvironment"));

        classTransformerMap.put(ClassTransformerConfigImpl.EXCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);
        excludes = config.getExcludes();
        Assert.assertEquals(1, excludes.size());
        assertTrue(excludes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
    }

    @Test
    public void excludes_withSecurityAgentClasses() throws Exception {
        Map<String, Object> classTransformerMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap,
                true, false, true);
        Set<String> excludes = config.getExcludes();
        Assert.assertEquals(5, excludes.size());
        assertTrue(excludes.contains("java/util/zip/InflaterInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInflaterInputStream"));
        assertTrue(excludes.contains("com/newrelic/api/agent/security/.*"));
        assertTrue(excludes.contains("com/newrelic/agent/security/.*"));

        classTransformerMap.put(
                ClassTransformerConfigImpl.EXCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper, test/ExcludeTest$ExcludeTestInner, org/jruby/rack/RackEnvironment");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, true);
        excludes = config.getExcludes();
        Assert.assertEquals(8, excludes.size());
        assertTrue(excludes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
        assertTrue(excludes.contains("test/ExcludeTest$ExcludeTestInner"));
        assertTrue(excludes.contains("org/jruby/rack/RackEnvironment"));
        assertTrue(excludes.contains("java/util/zip/InflaterInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInflaterInputStream"));
        assertTrue(excludes.contains("com/newrelic/api/agent/security/.*"));
        assertTrue(excludes.contains("com/newrelic/agent/security/.*"));

        classTransformerMap.put(ClassTransformerConfigImpl.EXCLUDES,
                "org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper");
        config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, true);
        excludes = config.getExcludes();
        Assert.assertEquals(6, excludes.size());
        assertTrue(excludes.contains("org/apache/tomcat/dbcp/dbcp/PoolableDataSource$PoolGuardConnectionWrapper"));
        assertTrue(excludes.contains("java/util/zip/InflaterInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInputStream"));
        assertTrue(excludes.contains("java/util/zip/ZipFile$ZipFileInflaterInputStream"));
        assertTrue(excludes.contains("com/newrelic/api/agent/security/.*"));
        assertTrue(excludes.contains("com/newrelic/agent/security/.*"));
    }

    @Test
    public void noTraceAnnotationClassNames() {
        ClassTransformerConfig config = new ClassTransformerConfigImpl(Collections.EMPTY_MAP, true);

        assertTrue(config.getTraceAnnotationMatcher().matches(
                ClassTransformerConfigImpl.internalizeName(com.newrelic.api.agent.Trace.class.getName())));
        assertTrue(config.getTraceAnnotationMatcher().matches(
                ClassTransformerConfigImpl.internalizeName("com.dude.NewRelicTrace")));
        Assert.assertEquals(0, config.getExcludes().size());
        Assert.assertEquals(0, config.getIncludes().size());
        Assert.assertEquals(0, config.getJdbcStatements().size());
    }

    @Test
    public void traceDisabled() {
        ClassTransformerConfig config = new ClassTransformerConfigImpl(Collections.EMPTY_MAP, false);

        Assert.assertFalse(config.getTraceAnnotationMatcher().matches(
                ClassTransformerConfigImpl.internalizeName(com.newrelic.api.agent.Trace.class.getName())));
        Assert.assertFalse(config.getTraceAnnotationMatcher().matches(
                ClassTransformerConfigImpl.internalizeName("com.dude.NewRelicTrace")));
    }

    @Test
    public void singleTraceAnnotationClassName() {
        String className = "com.dude.Test";
        Map<String, Object> map = getConfig(className);
        ClassTransformerConfig config = new ClassTransformerConfigImpl(map, true);
        assertTrue(config.getTraceAnnotationMatcher().matches(ClassTransformerConfigImpl.internalizeName(className)));
    }

    @Test
    public void multipleTraceAnnotationClassNames() {
        String className = "com.dude.Test", className2 = "com.dude.my.TraceAnnotation";
        Map<String, Object> map = getConfig(className + " , " + className2);
        ClassTransformerConfig config = new ClassTransformerConfigImpl(map, true);
        assertTrue(config.getTraceAnnotationMatcher().matches(ClassTransformerConfigImpl.internalizeName(className)));
        assertTrue(config.getTraceAnnotationMatcher().matches(ClassTransformerConfigImpl.internalizeName(className2)));
    }

    @Test
    public void multipleTraceAnnotationClassNamesUsingCollection() {
        String className = "com.dude.Test", className2 = "com.dude.my.TraceAnnotation";
        Map<String, Object> map = ImmutableMap.<String, Object>of("trace_annotation_class_name", ImmutableList.of(className, className2));
        ClassTransformerConfig config = new ClassTransformerConfigImpl(map, true);
        assertTrue(config.getTraceAnnotationMatcher().matches(ClassTransformerConfigImpl.internalizeName(className)));
        assertTrue(config.getTraceAnnotationMatcher().matches(ClassTransformerConfigImpl.internalizeName(className2)));
    }

    @Test
    public void jdbcStatementClassesSingle() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.JDBC_STATEMENTS_PROPERTY, "oracle/jdbc/driver/T4CPreparedStatement");
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        List<String> jdbcStatementClasses = new ArrayList<>();
        for (String jdbcStatementClass : config.getJdbcStatements()) {
            jdbcStatementClasses.add(jdbcStatementClass);
        }
        Assert.assertEquals(1, config.getJdbcStatements().size());
        assertTrue(jdbcStatementClasses.contains("oracle/jdbc/driver/T4CPreparedStatement"));
    }

    @Test
    public void jdbcStatementClassesMulti() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.JDBC_STATEMENTS_PROPERTY,
                "oracle/jdbc/driver/T4CPreparedStatement,oracle/jdbc/driver/OraclePreparedStatement");
        ClassTransformerConfigImpl config = new ClassTransformerConfigImpl(props, true);
        List<String> jdbcStatementClasses = new ArrayList<>();
        for (String jdbcStatementClass : config.getJdbcStatements()) {
            jdbcStatementClasses.add(jdbcStatementClass);
        }
        Assert.assertEquals(2, config.getJdbcStatements().size());
        assertTrue(jdbcStatementClasses.contains("oracle/jdbc/driver/T4CPreparedStatement"));
        assertTrue(jdbcStatementClasses.contains("oracle/jdbc/driver/OraclePreparedStatement"));
    }

    @Test
    public void includesSingle() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.INCLUDES, "com/dude/MyClass");
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        List<String> includes = new ArrayList<>();
        for (String include : config.getIncludes()) {
            includes.add(include);
        }
        Assert.assertEquals(1, config.getIncludes().size());
        assertTrue(includes.contains("com/dude/MyClass"));
    }

    @Test
    public void includesMulti() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.INCLUDES, "com/dude/MyClass,  test/IncludeTest$IncludeTestInner");
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        List<String> includes = new ArrayList<>();
        for (String include : config.getIncludes()) {
            includes.add(include);
        }
        Assert.assertEquals(2, config.getIncludes().size());
        assertTrue(includes.contains("com/dude/MyClass"));
        assertTrue(includes.contains("test/IncludeTest$IncludeTestInner"));
    }

    @Test
    public void excludesSingle() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.EXCLUDES, "com/dude/MyClass");
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        List<String> excludes = new ArrayList<>();
        for (String exclude : config.getExcludes()) {
            excludes.add(exclude);
        }
        Assert.assertEquals(1, config.getExcludes().size());
        assertTrue(excludes.contains("com/dude/MyClass"));
    }

    @Test
    public void excludesMulti() {
        HashMap<String, Object> props = new HashMap<>();
        props.put(ClassTransformerConfigImpl.EXCLUDES, "com/dude/MyClass,  test/ExcludeTest$ExcludeTestInner");
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        List<String> excludes = new ArrayList<>();
        for (String exclude : config.getExcludes()) {
            excludes.add(exclude);
        }
        Assert.assertEquals(2, config.getExcludes().size());
        assertTrue(excludes.contains("com/dude/MyClass"));
        assertTrue(excludes.contains("test/ExcludeTest$ExcludeTestInner"));
    }

    @Test
    public void weavePackageConfigDefault() {
        HashMap<String, Object> props = new HashMap<>();
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name("notenabled").enabled(false).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));

        weaveConfig = WeavePackageConfig.builder().name("enabled").enabled(true).build();
        assertTrue(config.isWeavePackageEnabled(weaveConfig));
    }

    @Test
    public void weavePackageConfigInstrumentationDisable() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        HashMap<String, Object> props = new HashMap<>();
        HashMap<String, Object> instDefault = new HashMap<>();

        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);
        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).enabled(true).build();
        assertTrue(config.isWeavePackageEnabled(weaveConfig));

        instDefault.put("enabled", false);
        props.put(ClassTransformerConfigImpl.DEFAULT_INSTRUMENTATION, instDefault);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(props, true, false, false);

        assertFalse(config.isWeavePackageEnabled(weaveConfig));

        props.put(moduleName, ImmutableMap.of("enabled", true));
        assertTrue(config.isWeavePackageEnabled(weaveConfig));
    }

    @Test
    public void testSpringPointcutDisabled() {
        Map<String, Object> classTransformerMap = new HashMap<>();
        Map<String, Object> defaultInstrumentationSettings = new HashMap<>();

        defaultInstrumentationSettings.put("enabled", false);

        classTransformerMap.put(ClassTransformerConfigImpl.DEFAULT_INSTRUMENTATION, defaultInstrumentationSettings);

        Map<String, Object> configSettings = new HashMap<>();
        configSettings.put(AgentConfigImpl.CLASS_TRANSFORMER, classTransformerMap);
        configSettings.put(AgentConfigImpl.APP_NAME, "Unit Test");
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configSettings);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        SpringPointCut springPointCut = new SpringPointCut(null, agentConfig);
        assertFalse(springPointCut.isEnabled());
    }

    @Test
    public void testSpringPointcutEnabledDefaultDisabled() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("class_transformer:instrumentation_default:enabled", false);
        configuration.put("enable_spring_tracing", true);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        SpringPointCut springPointCut = new SpringPointCut(null, agentConfig);
        assertTrue(springPointCut.isEnabled());
    }

    @Test
    public void testSpringPointcutDisabledDefaultEnabled() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("class_transformer:instrumentation_default:enabled", true);
        configuration.put("enable_spring_tracing", false);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        SpringPointCut springPointCut = new SpringPointCut(null, agentConfig);
        assertFalse(springPointCut.isEnabled());
    }

    @Test
    public void testSpringPointcutEnabled() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("lite_mode", true);
        configuration.put("class_transformer:instrumentation_default:enabled", true);
        configuration.put("enable_spring_tracing", true);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());

        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        SpringPointCut springPointCut = new SpringPointCut(null, agentConfig);
        assertTrue(springPointCut.isEnabled());
    }

    @Test
    public void testBuiltinExtension() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("class_transformer:instrumentation_default:enabled", false);
        configuration.put("class_transformer:builtin_extensions:enabled", true);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());

        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

//        SpringPointCut springPointCut = new SpringPointCut(agentConfig);
        assertTrue(agentConfig.getClassTransformerConfig().isBuiltinExtensionEnabled());
    }

    @Test
    public void testPointcutDisabled() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("class_transformer:instrumentation_default:enabled", true);
        configuration.put("class_transformer:my_pointcut_group:enabled", false);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);
        ClassTransformerConfig classTransformerConfig = agentConfig.getClassTransformerConfig();

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        PointCutConfiguration pointCutConfiguration = new PointCutConfiguration("name", "my_pointcut_group", true, classTransformerConfig);
        assertFalse(pointCutConfiguration.isEnabled());
    }

    @Test
    public void testPointcutEnabled() {
        Map<String, Object> classTransformerMap = new HashMap<>();

        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(classTransformerMap, true, false, false);

        Map<String, Object> configSettings = new HashMap<>();
        configSettings.put(AgentConfigImpl.CLASS_TRANSFORMER, classTransformerMap);
        configSettings.put(AgentConfigImpl.APP_NAME, "Unit Test");

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configSettings);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        PointCutConfiguration pointCutConfiguration = new PointCutConfiguration("name", null, true, config);
        assertTrue(pointCutConfiguration.isEnabled());
    }

    @Test
    public void testPointcutGroupDisabled() {
        Map<String, Object> configuration = new HashMap<>();
        configuration.put("class_transformer:my_pointcut_group:enabled", true);
        configuration.put("class_transformer:instrumentation_default:enabled", false);
        configuration.put("app_name", "Unit Test");
        configuration = buildConfigMap(configuration);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(configuration);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        PointCutConfiguration pointCutConfiguration = new PointCutConfiguration("PointcutClassName",
                "my_pointcut_group", true, agentConfig.getClassTransformerConfig());
        assertTrue(pointCutConfiguration.isEnabled());
    }

    @Test
    public void testPoincutExplicitEnable() {
        Map<String, Object> config = new HashMap<>();
        config.put("class_transformer:PointcutClassName:enabled", true);
        config.put("class_transformer:instrumentation_default:enabled", true);
        config.put("app_name", "Unit Test");
        config = buildConfigMap(config);

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        PointCutConfiguration pointCutConfiguration = new PointCutConfiguration("PointcutClassName",
                null, true, agentConfig.getClassTransformerConfig());
        assertTrue(pointCutConfiguration.isEnabled());
    }

    @Test
    public void weavePackageConfigInstrumentationDisableSystemPropertyOverride() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        Map<String, Object> configMap = new HashMap<>();
        ClassTransformerConfig config = ClassTransformerConfigImpl.createClassTransformerConfig(configMap, true, false, false);
        Assert.assertTrue(config.isDefaultInstrumentationEnabled());

        configMap.put("instrumentation_default:enabled", false);
        configMap = buildConfigMap(configMap);
        config = ClassTransformerConfigImpl.createClassTransformerConfig(configMap, true, false, false);
        Assert.assertFalse(config.isDefaultInstrumentationEnabled());

        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).build();
        assertFalse(config.isWeavePackageEnabled(weaveConfig));

        System.setProperty("newrelic.config.class_transformer." + moduleName + ".enabled", "true");
        weaveConfig = WeavePackageConfig.builder().name(moduleName).build();
        assertTrue(config.isWeavePackageEnabled(weaveConfig));

        System.clearProperty("newrelic.config.class_transformer." + moduleName + ".enabled");
    }

    @Test
    public void weavePackageConfigNameOverride() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        HashMap<String, Object> props = new HashMap<>();
        props.put(moduleName, ImmutableMap.of("enabled", false));
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).enabled(true).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));
    }

    @Test
    public void weavePackageConfigNameSystemPropertyOverride() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        HashMap<String, Object> props = new HashMap<>();
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        System.setProperty("newrelic.config.class_transformer." + moduleName + ".enabled", "false");
        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).enabled(true).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));

        System.clearProperty("newrelic.config.class_transformer." + moduleName + ".enabled");
    }

    @Test
    public void weavePackageConfigAliasOverride() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        final String aliasName = "my_alias";
        HashMap<String, Object> props = new HashMap<>();
        props.put(aliasName, ImmutableMap.of("enabled", false));
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).alias(aliasName).enabled(true).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));
    }

    @Test
    public void weavePackageConfigAliasSystemPropertyOverride() {
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        final String aliasName = "my_alias";
        HashMap<String, Object> props = new HashMap<>();
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        System.setProperty("newrelic.config.class_transformer." + aliasName + ".enabled", "false");
        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).alias(aliasName).enabled(true).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));

        System.clearProperty("newrelic.config.class_transformer." + aliasName + ".enabled");
    }

    @Test
    public void weavePackageConfigNameAndAliasOverride() {
        // if both alias and name are present, both must be set to enabled for the module to be loaded
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        final String aliasName = "my_alias";
        Map<String, Object> configuration = new HashMap<>();
        configuration.put(moduleName + ":enabled", true);
        configuration.put(aliasName + ":enabled", false);
        configuration = buildConfigMap(configuration);

        ClassTransformerConfig ctConfig = new ClassTransformerConfigImpl(configuration, true);

        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).alias(aliasName).enabled(true).build();
        Assert.assertFalse(ctConfig.isWeavePackageEnabled(weaveConfig));
    }

    @Test
    public void weavePackageConfigNameAndAliasSystemPropertyOverride() {
        // if both alias and name are present, both must be set to enabled for the module to be loaded
        final String moduleName = "com.newrelic.instrumentation.mymodule-1.0";
        final String aliasName = "my_alias";
        HashMap<String, Object> props = new HashMap<>();
        ClassTransformerConfig config = new ClassTransformerConfigImpl(props, true);

        System.setProperty("newrelic.config.class_transformer." + moduleName + ".enabled", "true");
        System.setProperty("newrelic.config.class_transformer." + aliasName + ".enabled", "false");
        WeavePackageConfig weaveConfig = WeavePackageConfig.builder().name(moduleName).alias(aliasName).enabled(true).build();
        Assert.assertFalse(config.isWeavePackageEnabled(weaveConfig));

        System.clearProperty("newrelic.config.class_transformer." + moduleName + ".enabled");
        System.clearProperty("newrelic.config.class_transformer." + aliasName + ".enabled");
    }

    @Test
    public void traceClassName() {
        Assert.assertEquals("Lcom/newrelic/api/agent/Trace;", ClassTransformerConfigImpl.NEW_RELIC_TRACE_TYPE_DESC);
    }

    private Map<String, Object> getConfig(final String traceAnnotationNames) {
        HashMap<String, Object> props = new HashMap<>();
        props.put("trace_annotation_class_name", traceAnnotationNames);
        return props;
    }

    @Test
    public void testLitemode() {
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(ImmutableMap.<String, Object>of("lite_mode", true, "app_name", "Unit Test"));

        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        ServiceManager serviceManager = new MockServiceManager(configService);
        ServiceFactory.setServiceManager(serviceManager);

        ClassTransformerConfig classTransformerConfig = serviceManager.getConfigService().getDefaultAgentConfig().getClassTransformerConfig();
        assertFalse(classTransformerConfig.isDefaultInstrumentationEnabled());
        assertFalse(classTransformerConfig.isBuiltinExtensionEnabled());
    }
}
