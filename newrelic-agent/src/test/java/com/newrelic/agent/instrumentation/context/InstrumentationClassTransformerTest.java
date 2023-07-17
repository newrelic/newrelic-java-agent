package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.DefaultClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcherBuilder;
import com.newrelic.agent.instrumentation.methodmatchers.ExactMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceClassTransformer;
import com.newrelic.agent.instrumentation.weaver.ClassLoaderClassTransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Trace;
import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.PrivilegedAction;
import java.util.Collections;

import static org.mockito.Mockito.mock;

public class InstrumentationClassTransformerTest {

    @Before
    public void setup() {
        MockServiceManager serviceManager = new MockServiceManager();  // yes, this is necessary
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Test
    public void test_shouldNotTransform() throws IllegalClassFormatException {
        InstrumentationClassTransformer target = getSimpleTarget(true, false, true);
        byte[] result = target.transform(this.getClass().getClassLoader(), this.getClass().getName(),
                InstrumentationClassTransformerTest.class, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_nullClassName() throws IllegalClassFormatException {
        InstrumentationClassTransformer target = getSimpleTarget();
        byte[] result = target.transform(this.getClass().getClassLoader(), null,
                InstrumentationClassTransformerTest.class, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_agentClassName() throws IllegalClassFormatException {
        InstrumentationClassTransformer target = getSimpleTarget();
        byte[] result = target.transform(this.getClass().getClassLoader(), "com/newrelic/agent/",
                InstrumentationClassTransformerTest.class, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_bootstrapClassloaderNotEnabled() throws IllegalClassFormatException {
        InstrumentationClassTransformer target = getSimpleTarget(false, false, true);
        byte[] result = target.transform(null, this.getClass().getName(),
                InstrumentationClassTransformerTest.class, null, null);
        Assert.assertNull(result);
    }

    @Test
    public void test_withAnnotationClass() throws IllegalClassFormatException, IOException {
        byte[] classBytes = getClassBytes(Trace.class);
        InstrumentationClassTransformer target = getSimpleTarget();
        byte[] result = target.transform(Trace.class.getClassLoader(), Trace.class.getClass().getName(),
                InstrumentationClassTransformerTest.class, null, classBytes);
        Assert.assertNull(result);
    }

    @Test
    public void test_withInterfaceWithDefaultMethod() throws IllegalClassFormatException, IOException {
        byte[] classBytes = getClassBytes(MyDefaultMethodInterface.class);
        InstrumentationClassTransformer target = getSimpleTarget(true, true, false);
        byte[] result = target.transform(MyDefaultMethodInterface.class.getClassLoader(), MyDefaultMethodInterface.class.getClass().getName(),
                InstrumentationClassTransformerTest.class, null, classBytes);
        Assert.assertNull(result);
    }

    @Test
    public void test_withJDKProxy() throws IllegalClassFormatException, IOException {
        byte[] classBytes = getClassBytes(MyProxy.class);
        InstrumentationClassTransformer target = getSimpleTarget(true, true, false);
        byte[] result = target.transform(MyProxy.class.getClassLoader(), MyProxy.class.getClass().getName(),
                InstrumentationClassTransformerTest.class, null, classBytes);
        Assert.assertNull(result);
    }

    @Test
    public void test_withSkippedInstr() throws Exception { // org/springframework/aop/SpringProxy
        byte[] classBytes = getClassBytes(MyClassToSkip.class);
        InstrumentationClassTransformer target = getComplexTarget();
        byte[] result = target.transform(MyClassToSkip.class.getClassLoader(), MyClassToSkip.class.getClass().getName(),
                MyClassToSkip.class, null, classBytes);
        Assert.assertNull(result);
    }

    @Test
    public void test_transformTracerMatch() throws Exception {
        byte[] classBytes = getClassBytes(MyClassToTransform.class);
        InstrumentationClassTransformer target = getComplexTarget();
        byte[] result = target.transform(MyClassToTransform.class.getClassLoader(), MyClassToTransform.class.getClass().getName(),
                MyClassToTransform.class, null, classBytes);
        Assert.assertNotNull(result);
    }

    private InstrumentationClassTransformer getSimpleTarget() {
        return getSimpleTarget(true, true, true);
    }

    private InstrumentationClassTransformer getSimpleTarget(boolean bootstrapClassloaderEnabled, boolean shouldTransform, boolean defaulMethodTracingEnabled) {
        InstrumentationContextManager manager = Mockito.mock(InstrumentationContextManager.class);
        Mockito.when(manager.shouldTransform(Mockito.anyString(), Mockito.any())).thenReturn(shouldTransform);

        TraceClassTransformer transformer = Mockito.mock(TraceClassTransformer.class);

        return new InstrumentationClassTransformer(manager, transformer,
                bootstrapClassloaderEnabled, defaulMethodTracingEnabled);
    }

    private InstrumentationClassTransformer getComplexTarget() throws Exception {
        ClassLoaderClassTransformer classLoaderClassTransformer = new ClassLoaderClassTransformer(mock(InstrumentationProxy.class),
                Collections.emptySet(), Collections.emptySet());
        InstrumentationProxy instrProxy = Mockito.mock(InstrumentationProxy.class);
        InstrumentationContextManager manager = InstrumentationContextManager.create(classLoaderClassTransformer, instrProxy, true);

        OptimizedClassMatcherBuilder builder = OptimizedClassMatcherBuilder.newBuilder();
        ClassMatcher classMatcher = new ExactClassMatcher("MyClassToTransformWithMods");
        MethodMatcher methodMatcher = new ExactMethodMatcher("myMethodToTransformWithMods");
        ClassAndMethodMatcher matcher = new DefaultClassAndMethodMatcher(classMatcher, methodMatcher);
        builder.addClassMethodMatcher(matcher);
        ClassMatchVisitorFactory matchVisitor = builder.build();
        manager.addContextClassTransformer(matchVisitor, classLoaderClassTransformer);

        //TraceClassTransformer transformer = Mockito.mock(TraceClassTransformer.class);
        TraceClassTransformer transformer = new TraceClassTransformer();

        return  new InstrumentationClassTransformer(manager, transformer,
                true, true);
    }

    private byte[] getClassBytes(Class clazz) throws IOException {
        return WeaveUtils.getClassBytesFromClassLoaderResource(clazz.getName(), clazz.getClassLoader());
    }

    private interface MyDefaultMethodInterface {
        public default void myDefaultMethod() {}
    }

    final class MyProxy extends Proxy {
        Method myMethodField;
        protected MyProxy(InvocationHandler h) {
            super(h);
        }
    }

    public class MyClassToSkip implements PrivilegedAction<String> {
        @Override
        public String run() {
            return null;
        }
    }

    public class MyClassToTransform {
        @Trace
        public void myMethodToTransform() {}
    }
}
