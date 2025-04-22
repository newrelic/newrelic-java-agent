/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.extension.ExtensionService;
import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.InterfaceMatcher;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.NoMethodsMatcher;
import com.newrelic.agent.instrumentation.yaml.InstrumentationConstructor;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.AbstractTracerFactory;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.Tracer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class PointCutConfigTest {

    @Before
    public void startUp() {
        MockServiceManager serviceManager = new MockServiceManager();
        ServiceFactory.setServiceManager(serviceManager);

        MockCoreService agent = new MockCoreService();
        serviceManager.setCoreService(agent);

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(new HashMap()), new HashMap());
        serviceManager.setConfigService(configService);

        ExtensionService service = new ExtensionService(configService, ExtensionsLoadedListener.NOOP);
        serviceManager.setExtensionService(service);

        TracerService tService = new TracerService();
        serviceManager.setTracerService(tService);
    }

    @Test
    public void nameMethodMatcherConcise() throws Exception {
        final String yaml = "- class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: 'getBytes'";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    /*
     * @Test public void orMethodMatcherConcise() throws Exception { String yaml =
     * "- class_matcher: 'java/lang/Foo'\n  method_matcher:\n  - 'getFoo'\n  - 'getBar'"; PointCutConfig config = new
     * PointCutConfig(new ByteArrayInputStream(yaml.getBytes("UTF-8"))); Assert.assertEquals(1,
     * config.getPointCuts().size()); Assert.assertNotNull(config.getPointCuts().get(0)); }
     */

    @Test
    public void orMethodMatcherConcise2() throws Exception {
        String yaml = "- class_matcher: 'java/lang/Foo'\n  method_matcher: [ [ 'getFoo', '()V' ], [ 'getBar', '()I' ] ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        MethodMatcher excludes = config.getPointCuts().get(0).getMethodMatcher();
        Assert.assertTrue(excludes.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getFoo", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(excludes.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getBar", "()I",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(excludes.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getBar", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(excludes.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getFoo", "()I",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void orMethodMatcherConciseWithMultipleSignatures() throws Exception {
        // Block sequence of constructors with multiple method signatures
        // Wish this worked, but it doesn't look like constructor arguments can be a block sequence
        // yaml =
        // "- class_matcher: 'java/lang/Foo'\n  method_matcher:\n  - !exact_method_matcher:\n    - 'getFoo'\n    - '(I)V'\n  - !exact_method_matcher:\n    - 'getBar'\n    - '(I)I'";
        String yaml = "- class_matcher: 'java/lang/Foo'\n  method_matcher:\n  - !exact_method_matcher [ 'getFoo', '(I)V', '(J)V' ]\n  - !exact_method_matcher [ 'getBar', '(I)I', '(Ljava/lang/String;)Ljava/lang/String;' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void exactClassMatcherConcise() throws Exception {
        final String yaml = "- class_matcher: 'java/lang/String'\n  method_matcher: !exact_method_matcher [ 'getBytes', '()V' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void orClassMatcherConcise() throws Exception {
        // Block sequence
        String yaml = "- class_matcher:\n  - 'java/lang/String'\n  - 'com/bytes/Factory'\n  method_matcher: !exact_method_matcher [ 'getBytes', '()V' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        // Flow collection
        yaml = "- class_matcher: ['java/lang/String', 'com/bytes/Factory']\n  method_matcher: !exact_method_matcher [ 'getBytes', '()V' ]";
        config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));

        // Mixed string/constructor in sequence
        yaml = "- class_matcher:\n  - 'java/lang/String'\n  - !child_class_matcher 'java/lang/Object'\n  method_matcher: !exact_method_matcher [ 'getBytes', '()V' ]";
        config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    /*
     * @Test public void noName() throws Exception { final String yaml =
     * "- class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !name_method_matcher 'getBytes'";
     * PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes("UTF-8")));
     * Assert.assertEquals(1, config.getPointCuts().size()); Assert.assertNotNull(config.getPointCuts().get(0)); }
     * 
     * @Test public void nameMethodMatcher() throws Exception { final String yaml =
     * "namen:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !name_method_matcher 'getBytes'"
     * ; PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes("UTF-8")));
     * Assert.assertEquals(1, config.getPointCuts().size()); Assert.assertNotNull(config.getPointCuts().get(0)); }
     */

    @Test
    public void exactMethodMatcherConcise() throws Exception {
        final String yaml = "oder:\n  class_matcher: 'java/lang/String'\n  method_matcher: [ 'concat', '(Ljava/lang/String;)Ljava/lang/String;' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        MethodMatcher methodMatcher = config.getPointCuts().get(0).getMethodMatcher();

        Assert.assertTrue(methodMatcher instanceof ExactMethodMatcher);
    }

    @Test
    public void incrediblyConcisePointcut() throws Exception {
        final String yaml = "oder:\n  java/lang/String.concat(Ljava/lang/String;)Ljava/lang/String;";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        MethodMatcher methodMatcher = config.getPointCuts().get(0).getMethodMatcher();

        ClassMatcher classMatcher = config.getPointCuts().get(0).getClassMatcher();
        Assert.assertTrue(classMatcher instanceof ExactClassMatcher);
        Assert.assertTrue(classMatcher.isMatch(String.class));
        Assert.assertFalse(classMatcher.isMatch(List.class));
        Assert.assertTrue(methodMatcher instanceof ExactMethodMatcher);
    }

    @Test
    public void exactMethodMatcherEvenMoreConcise() throws Exception {
        final String yaml = "oder:\n  class_matcher: java/lang/String\n  method_matcher: concat(Ljava/lang/String;)Ljava/lang/String;";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        MethodMatcher methodMatcher = config.getPointCuts().get(0).getMethodMatcher();

        Assert.assertTrue(config.getPointCuts().get(0).getClassMatcher() instanceof ExactClassMatcher);
        Assert.assertTrue(methodMatcher instanceof ExactMethodMatcher);
    }

    @Test
    public void orMethodMatcher() throws Exception {
        // Test single operand to OR, too
        final String yaml = "oder:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !or_method_matcher [ [ 'concat', '(Ljava/lang/String;)Ljava/lang/String;' ] ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void allMethodsMatcher() throws Exception {
        final String yaml = "alles:\n  class_matcher: !exact_class_matcher 'com/newrelic/agent/instrumentation/DefaultPointCut'\n  method_matcher: !all_methods_matcher";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void childClassMatcher() throws Exception {
        final String yaml = "kiddie:\n  class_matcher: !or_class_matcher [ !child_class_matcher 'com/newrelic/agent/instrumentation/DefaultPointCut', !exact_class_matcher 'com/newrelic/agent/instrumentation/DefaultPointCut' ]\n  method_matcher: !exact_method_matcher [ 'getTracerFactoryClassName', '()Ljava/lang/String;' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        ExtensionClassAndMethodMatcher pointCut = config.getPointCuts().get(0);
        MethodMatcher methodMatcher = pointCut.getMethodMatcher();
        Assert.assertTrue(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getTracerFactoryClassName",
                "()Ljava/lang/String;", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "getTracerFactoryClassName", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void andClassMatcher() throws Exception {
        final String yaml = "andy:\n  class_matcher: !and_class_matcher [ !interface_matcher 'java/util/List', !not_class_matcher [ !exact_class_matcher 'java/util/ArrayList' ] ]\n  method_matcher: !exact_method_matcher [ 'size', '()I' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void notClassMatcher() throws Exception {
        final String yaml = "knotty:\n  class_matcher: !not_class_matcher [ !exact_class_matcher 'java/util/HashMap' ]\n  method_matcher: !exact_method_matcher [ 'size', '()I' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void orClassMatcher() throws Exception {
        final String yaml = "orrin:\n  class_matcher: !or_class_matcher [ !exact_class_matcher 'java/util/HashMap', !exact_class_matcher 'java/util/TreeSet' ]\n  method_matcher: !exact_method_matcher [ 'size', '()I' ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void customFiles() throws Exception {
        File nofiles[] = null;
        @SuppressWarnings("unused")
        PointCutConfig pc = new PointCutConfig(nofiles);

        InputStream nostream = null;
        pc = new PointCutConfig(nostream);

        File emptyFile[] = { File.createTempFile("empty", "") };
        pc = new PointCutConfig(emptyFile);
    }

    @Test
    public void yaml() {
        SafeConstructor constructor = new InstrumentationConstructor(new LoaderOptions());
        Yaml yaml = new Yaml(constructor);

        Object config = yaml.load("--- !interface_matcher 'org/apache/solr/request/SolrRequestHandler'");
        Assert.assertEquals(InterfaceMatcher.class, config.getClass());

        config = yaml.load("--- !exact_method_matcher [ 'handleRequest', '(Lorg/apache/solr/request/SolrQueryRequest;Lorg/apache/solr/request/SolrQueryResponse;)V' ]");
        Assert.assertEquals(ExactMethodMatcher.class, config.getClass());
    }

    @Test
    public void solr() {
        PointCutConfig config = new PointCutConfig(
                new ByteArrayInputStream(
                        "solr:\n  class_matcher: !interface_matcher 'org/apache/solr/request/SolrRequestHandler'\n  method_matcher: !exact_method_matcher [ 'handleRequest', '(Lorg/apache/solr/request/SolrQueryRequest;Lorg/apache/solr/request/SolrQueryResponse;)V' ]".getBytes(
                                StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void defaultMetricPrefix() throws Exception {
        PointCutConfig config = new PointCutConfig(
                new ByteArrayInputStream(
                        "pre:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !exact_method_matcher [ 'concat', '(Ljava/lang/String;)Ljava/lang/String;' ]\n".getBytes(
                                StandardCharsets.UTF_8)));
        List<ExtensionClassAndMethodMatcher> pcs = config.getPointCuts();
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher pc = config.getPointCuts().get(0);
        Assert.assertNotNull(pc);
        // metric name prefix should be "Custom"
    }

    @Test
    public void invalidMethodDescriptor() throws Exception {
        PointCutConfig config = new PointCutConfig(
                new ByteArrayInputStream(
                        "pre:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !exact_method_matcher [ 'concat', '(java/lang/String;)Ljava/lang/String;' ]\n".getBytes(
                                StandardCharsets.UTF_8)));
        List<ExtensionClassAndMethodMatcher> pcs = config.getPointCuts();
        Assert.assertEquals(1, pcs.size());
        ExtensionClassAndMethodMatcher pc = config.getPointCuts().get(0);
        Assert.assertNotNull(pc);
        MethodMatcher methodMatcher = pc.getMethodMatcher();
        Assert.assertTrue(methodMatcher instanceof NoMethodsMatcher);
    }

    @Test
    public void metricPrefix() throws Exception {
        PointCutConfig config = new PointCutConfig(
                new ByteArrayInputStream(
                        "pre:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !exact_method_matcher [ 'concat', '(Ljava/lang/String;)Ljava/lang/String;' ]\n  metric_name_format: !class_method_metric_name_format 'ImportantMetricCategory'".getBytes(
                                StandardCharsets.UTF_8)));
        List<ExtensionClassAndMethodMatcher> pcs = config.getPointCuts();
        Assert.assertEquals(1, pcs.size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void classMethodMetricNameFormat() throws Exception {
        PointCutConfig config = new PointCutConfig(
                new ByteArrayInputStream(
                        "pre:\n  class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: !exact_method_matcher [ 'concat', '(Ljava/lang/String;)Ljava/lang/String;' ]\n  metric_name_format: !class_method_metric_name_format".getBytes(
                                StandardCharsets.UTF_8)));
        List<ExtensionClassAndMethodMatcher> pcs = config.getPointCuts();
        Assert.assertEquals(1, pcs.size());
        Assert.assertNotNull(config.getPointCuts().get(0));
    }

    @Test
    public void staticMethodMatcher() throws Exception {
        final String yaml = "oder:\n  class_matcher: java/lang/String\n  method_matcher: !static_method_matcher [ concat(Ljava/lang/String;)Ljava/lang/String; ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));

        MethodMatcher methodMatcher = config.getPointCuts().get(0).getMethodMatcher();
        Assert.assertTrue(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "concat",
                "(Ljava/lang/String;)Ljava/lang/String;", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "dude",
                "(Ljava/lang/String;)Ljava/lang/String;", com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void nestedStaticMethodMatcher() throws Exception {
        final String yaml = "oder:\n  class_matcher: java/lang/String\n  method_matcher: [ !static_method_matcher [ concat(Ljava/lang/String;)Ljava/lang/String; ], go()V ]";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());
        Assert.assertNotNull(config.getPointCuts().get(0));
        MethodMatcher methodMatcher = config.getPointCuts().get(0).getMethodMatcher();
        Assert.assertTrue(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "concat",
                "(Ljava/lang/String;)Ljava/lang/String;", com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertTrue(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "go", "()V",
                com.google.common.collect.ImmutableSet.<String> of()));
        Assert.assertFalse(methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, "dude",
                "(Ljava/lang/String;)Ljava/lang/String;", com.google.common.collect.ImmutableSet.<String> of()));
    }

    @Test
    public void tracerFactory() throws Exception {
        // test missing tracer factory
        String yaml = "- class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: 'getBytes'\n  tracer_factory: NoWayDude";
        PointCutConfig config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(0, config.getPointCuts().size());

        // test factory that exists
        yaml = "- class_matcher: !exact_class_matcher 'java/lang/String'\n  method_matcher: 'getBytes'\n  tracer_factory: "
                + MyTestTracerFactory.class.getName();
        config = new PointCutConfig(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        Assert.assertEquals(1, config.getPointCuts().size());

        Assert.assertNotNull(config.getPointCuts().get(0));

        // make sure it is registered with the service
        Assert.assertNotNull(ServiceFactory.getTracerService().getTracerFactory(
                config.getPointCuts().get(0).getTraceDetails().tracerFactoryName()));
    }

    public static class MyTestTracerFactory extends AbstractTracerFactory {

        @Override
        public Tracer doGetTracer(Transaction transaction, ClassMethodSignature sig, Object object, Object[] args) {
            return null;
        }

    }
}
