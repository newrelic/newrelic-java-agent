/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import com.newrelic.agent.bridge.external.ExternalParametersFactory;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessors.TokenNullCheckClassVisitor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.SlowQueryDatastoreParameters;
import com.newrelic.api.agent.SlowQueryWithInputDatastoreParameters;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.weave.UtilityClass;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessors.InstrumentationPackageRemapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AgentPreprocessorsTest {

    private static final String INSTRUMENTATION_TITLE = "com.javaagent.rules";

    @Test
    public void testHandleElevatePermissions() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        // The easiest way to find this class is to put an exception in AgentPreprocessors when loadClass would happen.
        // It only needs to hit loadClass to prove that it's not going to explode on rewrite.
        // It seems like `instanceof` will trigger this.
        final String classname = "org.eclipse.jetty.server.handler.RequestLogHandler";

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = AgentPreprocessors.handleElevatePermissions(writer);
        source.accept(cv);

        StringWriter verificationResults = new StringWriter();
        CheckClassAdapter.verify(new ClassReader(writer.toByteArray()), false, new PrintWriter(verificationResults));
        assertTrue(verificationResults.toString(), verificationResults.toString().isEmpty());

        Class<?> clazz = addToClassLoader(classname, writer.toByteArray(), classloader);
        Assert.assertNotNull(clazz);
        // WeaveTestUtils.byteArrayToFile(WeaveUtils.convertToClassBytes(result), "/tmp/result.class");
        clazz.newInstance();
    }

    @Test
    public void testGatherTraceDetails() throws IOException {
        {
            final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            final String classname = OriginalClass.class.getName();

            byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
            Assert.assertNotNull(bytes);
            ClassNode source = WeaveUtils.convertToClassNode(bytes);
            ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
            ClassVisitor cv = new CheckClassAdapter(result);

            ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> weaveTraceDetailsTrackers = new ConcurrentHashMap<>();

            Map<String, Object> confProps = new HashMap<>();
            AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);
            AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, weaveTraceDetailsTrackers);
            preprocessors.setInstrumentationTitle("foo");
            Assert.assertEquals(0, weaveTraceDetailsTrackers.size());

            cv = preprocessors.gatherTraceInfo(cv);
            source.accept(cv);

            Assert.assertEquals(1, weaveTraceDetailsTrackers.size());
            // this is not a weave class but we should still gather trace info
            Assert.assertEquals(2, weaveTraceDetailsTrackers.get("foo").size());
        }
        {
            final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            final String classname = WeaveClass.class.getName();

            byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
            Assert.assertNotNull(bytes);
            ClassNode source = WeaveUtils.convertToClassNode(bytes);
            ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
            ClassVisitor cv = new CheckClassAdapter(result);

            ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> weaveTraceDetailsTrackers = new ConcurrentHashMap<>();

            Map<String, Object> confProps = new HashMap<>();
            AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);
            AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, weaveTraceDetailsTrackers);
            preprocessors.setInstrumentationTitle("foo");
            Assert.assertEquals(0, weaveTraceDetailsTrackers.size());

            cv = preprocessors.gatherTraceInfo(cv);
            source.accept(cv);

            for (MethodNode compositeMethod : result.methods) {
                TracedWeaveInstrumentationTracker.removeTraceAnnotations(compositeMethod);
                if (null != compositeMethod.visibleAnnotations) {
                    for (AnnotationNode annotation : compositeMethod.visibleAnnotations) {
                        Assert.assertNotEquals(Type.getDescriptor(Trace.class), annotation.desc);
                    }
                }
            }

            Assert.assertEquals(1, weaveTraceDetailsTrackers.size());
            // this is a weave class so we should gather trace info
            Assert.assertEquals(2, weaveTraceDetailsTrackers.get("foo").size());
            Set<TracedWeaveInstrumentationTracker> weaveDetailsSet = weaveTraceDetailsTrackers.get("foo");

            List<String> tracedWeaveMethods = new ArrayList<>();
            tracedWeaveMethods.add("ohLookATracer");
            tracedWeaveMethods.add("ohLookADispatcher");

            for (TracedWeaveInstrumentationTracker tracedWeaveDetails : weaveDetailsSet) {
                Assert.assertEquals("foo", tracedWeaveDetails.getWeavePackageName());
                Assert.assertEquals(
                        "com/newrelic/agent/instrumentation/weaver/preprocessors/AgentPreprocessorsTest/OriginalClass",
                        tracedWeaveDetails.getClassName());
                Method method = tracedWeaveDetails.getMethod();
                assertTrue(tracedWeaveMethods.remove(method.getName()));
            }
        }
    }

    private static class OriginalClass {
        @Trace
        public void ohLookATracer() {
            System.out.println("hi.");
        }

        @Trace
        public void ohLookADispatcher() {
            System.out.println("HI!");
        }

        public void anotherMethod() {
        }
    }

    @Weave(originalName = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest.OriginalClass")
    private static class WeaveClass {
        @Trace
        public void ohLookATracer() {
            System.out.println("hi.");
        }

        @Trace(dispatcher = true)
        public void ohLookADispatcher() {
            System.out.println("HI!");
        }

        public void anotherMethod() {
        }
    }

    @Test
    public void testNotMarkWeaveClassAsUtility() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$WeaveTestClass";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.markUtilityClasses(cv);
        source.accept(cv);
        Class<?> clazz = addToContextClassloader(result);
        Assert.assertNotNull(clazz);

        assertTrue(clazz.isAnnotationPresent(TestAnnotation.class));
        assertTrue(clazz.isAnnotationPresent(Weave.class));
        assertFalse(clazz.isAnnotationPresent(UtilityClass.class));
    }

    @Test
    public void testNotMarkSkipIfPresentClassAsUtility() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$SkipIfPresentTestClass";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);

        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.markUtilityClasses(cv);
        source.accept(cv);
        Class<?> clazz = addToContextClassloader(result);
        Assert.assertNotNull(clazz);

        assertTrue(clazz.isAnnotationPresent(TestAnnotation.class));
        assertTrue(clazz.isAnnotationPresent(SkipIfPresent.class));
        assertFalse(clazz.isAnnotationPresent(UtilityClass.class));
    }

    @Test
    public void testMarkUtilityClassAsUtility() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$UtilityTestClass";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.markUtilityClasses(cv);
        source.accept(cv);
        Class<?> clazz = addToContextClassloader(result);
        Assert.assertNotNull(clazz);

        assertTrue(clazz.isAnnotationPresent(TestAnnotation.class));
        assertTrue(clazz.isAnnotationPresent(UtilityClass.class));
        assertEquals(clazz.getAnnotation(UtilityClass.class).weavePackageName(), INSTRUMENTATION_TITLE);
    }

    @Test
    public void testPreprocess() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$TestPreprocess";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);

        AgentPreprocessors preprocessors = AgentPreprocessors.createWithInstrumentationTitle(agentConfig, INSTRUMENTATION_TITLE);
        cv = preprocessors.preprocess(cv, new HashSet<>(), null);
        source.accept(cv);
        Assert.assertTrue(cv.getClass().getName().equals("com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessors$InstrumentationPackageNameRewriter"));

        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);
        TestPreprocess testClass = (TestPreprocess) clazz.newInstance();
        assertNotNull(testClass);
    }

    @Test
    public void testRewriteSlowQueryNoHighSecurity() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass1";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass1 testClass = (ExternalParametersFactoryTestClass1) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        assertTrue(slowQueryDatastore instanceof SlowQueryDatastoreParameters);
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        assertTrue(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters);
    }

    @Test
    public void testRewriteSlowQueryWithHighSecurity() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass2";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass2 testClass = (ExternalParametersFactoryTestClass2) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryDatastore instanceof SlowQueryDatastoreParameters));
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters));
    }

    @Test
    public void testRewriteSlowQueryWithHighSecurityAndListToCollect() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass3";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        Map<String, Object> ttConfig = new HashMap<>();
        ttConfig.put("collect_slow_queries_from", INSTRUMENTATION_TITLE + ",some-other-module");
        confProps.put("transaction_tracer", ttConfig);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass3 testClass = (ExternalParametersFactoryTestClass3) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        assertTrue(slowQueryDatastore instanceof SlowQueryDatastoreParameters);
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        assertTrue(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters);
    }

    @Test
    public void testRewriteSlowQueryWithHighSecurityAndNonPresentListToCollect() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass4";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        Map<String, Object> ttConfig = new HashMap<>();
        ttConfig.put("collect_slow_queries_from", "some-other-module");
        confProps.put("transaction_tracer", ttConfig);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass4 testClass = (ExternalParametersFactoryTestClass4) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryDatastore instanceof SlowQueryDatastoreParameters));
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters));
    }

    @Test
    public void testRewriteSlowQueryBuilderNoHighSecurity() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass5";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass5 testClass = (ExternalParametersFactoryTestClass5) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        assertTrue(slowQueryDatastore instanceof SlowQueryDatastoreParameters);
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        assertTrue(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters);
    }

    @Test
    public void testRewriteSlowQueryBuilderWithHighSecurity() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass6";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass6 testClass = (ExternalParametersFactoryTestClass6) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryDatastore instanceof SlowQueryDatastoreParameters));
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters));
    }

    @Test
    public void testRewriteSlowQueryBuilderWithHighSecurityAndListToCollect() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass7";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        Map<String, Object> ttConfig = new HashMap<>();
        ttConfig.put("collect_slow_queries_from", INSTRUMENTATION_TITLE + ",some-other-module");
        confProps.put("transaction_tracer", ttConfig);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass7 testClass = (ExternalParametersFactoryTestClass7) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        assertTrue(slowQueryDatastore instanceof SlowQueryDatastoreParameters);
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        assertTrue(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters);
    }

    @Test
    public void testRewriteSlowQueryBuilderWithHighSecurityAndNonPresentListToCollect() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$ExternalParametersFactoryTestClass8";

        Map<String, Object> confProps = new HashMap<>();
        confProps.put("high_security", true);
        Map<String, Object> ttConfig = new HashMap<>();
        ttConfig.put("collect_slow_queries_from", "some-other-module");
        confProps.put("transaction_tracer", ttConfig);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        cv = preprocessors.rewriteSlowQueryIfRequired(cv);
        source.accept(cv);
        Class<?> clazz = addToClassloader(result, classloader);
        Assert.assertNotNull(clazz);

        ExternalParametersFactoryTestClass8 testClass = (ExternalParametersFactoryTestClass8) clazz.newInstance();
        assertNotNull(testClass);
        ExternalParameters regularDatastore = testClass.createRegularDatastore();
        assertNotNull(regularDatastore);
        assertTrue(regularDatastore instanceof DatastoreParameters);
        ExternalParameters slowQueryDatastore = testClass.createSlowQueryDatastore();
        assertNotNull(slowQueryDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryDatastore instanceof SlowQueryDatastoreParameters));
        ExternalParameters slowQueryWithInputDatastore = testClass.createSlowQueryWithInputDatastore();
        assertNotNull(slowQueryWithInputDatastore);
        // AgentPreprocessors should re-write the call to return a regular DatastoreParameters object
        assertTrue(!(slowQueryWithInputDatastore instanceof SlowQueryWithInputDatastoreParameters));
    }

    @TestAnnotation
    @Weave
    private static class WeaveTestClass {
        @NewField
        public Token testToken;
        @NewField
        public Token otherToken;

        public void doesNotNull() {
            if (null != testToken) {
                testToken.link();
                testToken.expire();
            }
        }

        public void doesNull() {
            if (null != testToken) {
                testToken.link();
                testToken.expire();
                testToken = null;
            }
        }

        public void conditionallyNullsGood() {
            if (null != testToken) {
                testToken.link();
                if (System.currentTimeMillis() % 2 == 0) {
                    testToken.expire();
                    testToken = null;
                }
            }
        }

        public void conditionallyNullsBad() {
            if (null != testToken) {
                testToken.link();
                testToken.expire();
                if (System.currentTimeMillis() % 2 == 0) {
                    testToken = null;
                }
            }
        }

        public void nullsLater() {
            testToken.link();
            testToken.expire();
            "Foo".toCharArray();
            testToken = null;
        }

        public void returnsBeforeNull() {
            testToken.link();
            testToken.expire();
            if (true) return;
            testToken = null;
        }

        public void twoExpires() {
            testToken.link();
            testToken.expire();
            otherToken.link();
            otherToken.expire();

            testToken.link();
            testToken.expire();
            testToken = null;
            otherToken.link();
            otherToken.expire();
            otherToken = null;
        }

        public void nullsOtherToken() {
            testToken.link();
            testToken.expire();
            otherToken = null;
        }

        public void expiresLocal() {
            Token t = genToken();
            t.expire();
        }

        private Token genToken() {
            return null;
        }
    }

    @TestAnnotation
    @SkipIfPresent
    private static class SkipIfPresentTestClass {
    }

    @TestAnnotation
    private static class UtilityTestClass {
    }

    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface TestAnnotation {
    }

    // testPreprocess
    public static class TestPreprocess {
        public void testMethod() {
        }
    }

    // testRewriteSlowQueryNoHighSecurity
    public static class ExternalParametersFactoryTestClass1 {

        public ExternalParameters createRegularDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345);
        }

        public ExternalParameters createSlowQueryDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER);
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER, "inputQueryLabel", "rawInputQuery", TEST_BRIDGE_INPUT_QUERY_CONVERTER);
        }
    }

    // testRewriteSlowQueryWithHighSecurity
    public static class ExternalParametersFactoryTestClass2 {

        public ExternalParameters createRegularDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345);
        }

        public ExternalParameters createSlowQueryDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER);
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER, "inputQueryLabel", "rawInputQuery", TEST_BRIDGE_INPUT_QUERY_CONVERTER);
        }
    }

    // testRewriteSlowQueryWithHighSecurityAndListToCollect
    public static class ExternalParametersFactoryTestClass3 {

        public ExternalParameters createRegularDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345);
        }

        public ExternalParameters createSlowQueryDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER);
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER, "inputQueryLabel", "rawInputQuery", TEST_BRIDGE_INPUT_QUERY_CONVERTER);
        }
    }

    // testRewriteSlowQueryWithHighSecurityAndNonPresentListToCollect
    public static class ExternalParametersFactoryTestClass4 {

        public ExternalParameters createRegularDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345);
        }

        public ExternalParameters createSlowQueryDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER);
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return ExternalParametersFactory.createForDatastore("product", "collection", "operation", "host", 12345,
                    "rawQuery", TEST_BRIDGE_QUERY_CONVERTER, "inputQueryLabel", "rawInputQuery",
                    TEST_BRIDGE_INPUT_QUERY_CONVERTER);
        }
    }

    // testRewriteSlowQueryBuilderNoHighSecurity
    public static class ExternalParametersFactoryTestClass5 {

        public ExternalParameters createRegularDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .build();
        }

        public ExternalParameters createSlowQueryDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .build();
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build();
        }
    }

    // testRewriteSlowQueryBuilderWithHighSecurity
    public static class ExternalParametersFactoryTestClass6 {

        public ExternalParameters createRegularDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .build();
        }

        public ExternalParameters createSlowQueryDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .build();
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build();
        }
    }

    // testRewriteSlowQueryBuilderWithHighSecurityAndListToCollect
    public static class ExternalParametersFactoryTestClass7 {

        public ExternalParameters createRegularDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .build();
        }

        public ExternalParameters createSlowQueryDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .build();
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build();
        }
    }

    // testRewriteSlowQueryBuilderWithHighSecurityAndNonPresentListToCollect
    public static class ExternalParametersFactoryTestClass8 {

        public ExternalParameters createRegularDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .build();
        }

        public ExternalParameters createSlowQueryDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .build();
        }

        public ExternalParameters createSlowQueryWithInputDatastore() {
            return DatastoreParameters
                    .product("product")
                    .collection("collection")
                    .operation("operation")
                    .instance("host", 12345)
                    .noDatabaseName()
                    .slowQuery("rawQuery", TEST_QUERY_CONVERTER)
                    .slowQueryWithInput("inputQueryLabel", "rawInputQuery", TEST_INPUT_QUERY_CONVERTER)
                    .build();
        }
    }

    public static com.newrelic.agent.bridge.datastore.QueryConverter<String> TEST_BRIDGE_QUERY_CONVERTER = new com.newrelic.agent.bridge.datastore.QueryConverter<String>() {
        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery;
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            return rawQuery + "-obfuscated";
        }
    };

    public static com.newrelic.agent.bridge.datastore.QueryConverter<String> TEST_BRIDGE_INPUT_QUERY_CONVERTER = new com.newrelic.agent.bridge.datastore.QueryConverter<String>() {
        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery + "-input";
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            return rawQuery + "-input-obfuscated";
        }
    };

    public static QueryConverter<String> TEST_QUERY_CONVERTER = new QueryConverter<String>() {
        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery;
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            return rawQuery + "-obfuscated";
        }
    };

    public static QueryConverter<String> TEST_INPUT_QUERY_CONVERTER = new QueryConverter<String>() {
        @Override
        public String toRawQueryString(String rawQuery) {
            return rawQuery + "-input";
        }

        @Override
        public String toObfuscatedQueryString(String rawQuery) {
            return rawQuery + "-input-obfuscated";
        }
    };

    @Test
    public void testTokenExpire() throws Exception {
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        final String classname = "com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessorsTest$WeaveTestClass";

        Map<String, Object> confProps = new HashMap<>();
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(confProps);

        byte[] bytes = getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode source = WeaveUtils.convertToClassNode(bytes);
        ClassNode result = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        ClassVisitor cv = result;
        cv = new CheckClassAdapter(result);
        AgentPreprocessors preprocessors = new AgentPreprocessors(agentConfig, null);
        preprocessors.setInstrumentationTitle(INSTRUMENTATION_TITLE);
        TokenNullCheckClassVisitor tcv = preprocessors.nullOutTokenAfterExpire(cv);
        source.accept(tcv);

        Map<String, Integer> rewriteCounts = tcv.getExpireRewriteCounts();
        Assert.assertEquals(1, rewriteCounts.get("doesNotNull").intValue());
        Assert.assertEquals(0, rewriteCounts.get("doesNull").intValue());
        Assert.assertEquals(0, rewriteCounts.get("conditionallyNullsGood").intValue());
        Assert.assertEquals(1, rewriteCounts.get("conditionallyNullsBad").intValue());
        Assert.assertEquals(1, rewriteCounts.get("nullsLater").intValue());
        Assert.assertEquals(1, rewriteCounts.get("returnsBeforeNull").intValue());
        Assert.assertEquals(2, rewriteCounts.get("twoExpires").intValue());
        Assert.assertEquals(1, rewriteCounts.get("nullsOtherToken").intValue());
        Assert.assertEquals(0, rewriteCounts.get("expiresLocal").intValue());
    }

    /**
     * Read a ClassLoader's resource into a byte array.
     *
     * @param classname Internal or Fully qualified name of the class
     * @param classloader the classloader to read the resource from
     * @return the resource bytes (class bytes) or null if no resource was found.
     * @throws IOException
     */
    public static byte[] getClassBytesFromClassLoaderResource(String classname, ClassLoader classloader)
            throws IOException {
        InputStream is = classloader.getResourceAsStream(classname.replace('.', '/') + ".class");
        if (null == is) {
            return null; // no such resource
        }

        return Streams.read(is, true);
    }

    /**
     * Add the class represented by the ASM node definition to the class loader
     *
     * @param node ASM class node definition
     * @return class instance that was loaded into the classloader
     */
    public static Class<?> addToContextClassloader(ClassNode node) {
        return addToClassloader(node, Thread.currentThread().getContextClassLoader());
    }

    public static Class<?> addToClassloader(ClassNode node, ClassLoader classLoader) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);
        byte[] bytes = writer.toByteArray();
        String name = node.name.replace('/', '.');
        return addToClassLoader(name, bytes, classLoader);
    }

    /**
     * From http://asm.ow2.org/doc/faq.html#Q5
     *
     * @param className class name
     * @param classBytes binary class data
     * @return class instance that was loaded into the classloader
     */
    public static synchronized Class<?> addToClassLoader(String className, byte[] classBytes, ClassLoader loader) {
        Class clazz = null;
        try {
            Class cls = Class.forName("java.lang.ClassLoader");
            java.lang.reflect.Method method = cls.getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class);

            method.setAccessible(true);
            try {
                Object[] args = new Object[] { className, classBytes, 0, classBytes.length };
                clazz = (Class) method.invoke(loader, args);
            } finally {
                method.setAccessible(false);
            }
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof LinkageError)) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return clazz;
    }

    @Test
    public void testInstrumentationRemapper() {
        HashSet<String> utilityClassNames = new HashSet<>();

        utilityClassNames.add("com/newrelic/agent/bad/Foo");
        utilityClassNames.add("com/nr/instrumentation/good/Bar");

        AgentPreprocessors.InstrumentationPackageRemapper instrumentationPackageRemapper = new InstrumentationPackageRemapper(
                "myWeavePackageName", utilityClassNames);
        String remappedFoo = instrumentationPackageRemapper.map("com/newrelic/agent/bad/Foo");
        assertEquals("com/nr/instrumentation/bad/Foo", remappedFoo);

        String remappedBar = instrumentationPackageRemapper.map("com/nr/instrumentation/good/Bar");
        assertEquals("com/nr/instrumentation/good/Bar", remappedBar);
    }

}
