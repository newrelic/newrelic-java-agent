package com.newrelic.agent.util;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnwindableInstrumentationImplTest {
    @Test
    public void wrapInstrumentation() {
        Instrumentation inst = UnwindableInstrumentationImpl.wrapInstrumentation(Mockito.mock(Instrumentation.class));
        assertTrue(inst instanceof UnwindableInstrumentation);
    }

    @Test
    public void getMissingInterfaceMethods() {
        List<UnwindableInstrumentationImpl.MethodDesc> missingInterfaceMethods = UnwindableInstrumentationImpl.getMissingInterfaceMethods();
        Optional<Method> redefineModule = Arrays.stream(Instrumentation.class.getDeclaredMethods())
                .filter(method -> "redefineModule".equals(method.getName()))
                .findFirst();
        // if we find the redefineModule method, we're running in java9+ and missing methods should contain it
        assertEquals(redefineModule.isPresent(), !missingInterfaceMethods.isEmpty());
    }

    @Test
    public void createProxyInstance() {
        Instrumentation inst = Mockito.mock(Instrumentation.class);
        UnwindableInstrumentationImpl unwindableInstrumentation = Mockito.mock(UnwindableInstrumentationImpl.class);
        Instrumentation proxy = UnwindableInstrumentationImpl.createProxyInstance(unwindableInstrumentation, inst,
                Collections.singletonList(new UnwindableInstrumentationImpl.MethodDesc("isRetransformClassesSupported", "()Z")));
        assertTrue(proxy instanceof UnwindableInstrumentation);

        ClassFileTransformer transformer = Mockito.mock(ClassFileTransformer.class);
        proxy.addTransformer(transformer);
        Mockito.verify(inst, Mockito.times(0)).addTransformer(transformer);
        Mockito.verify(unwindableInstrumentation, Mockito.times(1)).addTransformer(transformer);

        proxy.isRetransformClassesSupported();
        Mockito.verify(inst, Mockito.times(1)).isRetransformClassesSupported();
        Mockito.verify(unwindableInstrumentation, Mockito.times(0)).isRetransformClassesSupported();
    }

    @Test
    public void addTransformersAndUnwind() {
        final List<ClassFileTransformer> transformers = new ArrayList<>();
        Instrumentation instrumentation = new DelegatingInstrumentation(Mockito.mock(Instrumentation.class)) {
            @Override
            public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
                transformers.add(transformer);
            }
            @Override
            public void addTransformer(ClassFileTransformer transformer) {
                transformers.add(transformer);
            }

            @Override
            public boolean removeTransformer(ClassFileTransformer transformer) {
                return transformers.remove(transformer);
            }
        };
        UnwindableInstrumentationImpl impl = new UnwindableInstrumentationImpl(instrumentation);
        ClassFileTransformer transformer = Mockito.mock(ClassFileTransformer.class);
        impl.addTransformer(transformer);
        impl.addTransformer(transformer, true);

        assertEquals(0, impl.classFileTransformerMap.size());
        assertEquals(2, transformers.size());

        ClassFileTransformer nrTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return null;
            }
        };
        impl.addTransformer(nrTransformer);
        assertEquals(3, transformers.size());

        assertEquals(1, impl.classFileTransformerMap.size());
        impl.unwind();
        assertEquals(0, impl.classFileTransformerMap.size());
        assertEquals(2, transformers.size());

        impl.addTransformer(nrTransformer);
        assertEquals(0, impl.classFileTransformerMap.size());
        assertEquals(3, transformers.size());
    }

    @Test
    public void transformClass() throws Exception {
        final List<ClassFileTransformer> transformers = new ArrayList<>();
        final List<Class<?>> retransformed = new ArrayList<>();
        Instrumentation instrumentation = new DelegatingInstrumentation(Mockito.mock(Instrumentation.class)) {
            @Override
            public void addTransformer(ClassFileTransformer transformer) {
                transformers.add(transformer);
            }

            @Override
            public void retransformClasses(Class<?>... classes) {
                retransformed.addAll(Arrays.asList(classes));
            }
        };
        UnwindableInstrumentationImpl impl = new UnwindableInstrumentationImpl(instrumentation);

        ClassFileTransformer nrTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                return new byte[] { 0xB, 0xE, 0xE, 0xF };
            }
        };
        impl.addTransformer(nrTransformer);

        assertEquals(1, impl.classFileTransformerMap.size());
        assertEquals(1, transformers.size());

        transformers.get(0).transform(null, UnwindableInstrumentation.class.getName(),
                null, null, new byte[0]);
        transformers.get(0).transform(null, UnwindableInstrumentationImpl.class.getName(),
                UnwindableInstrumentationImpl.class, null, new byte[0]);
        assertEquals(1, impl.modifiedClassInfo.size());
        assertEquals(1, impl.modifiedClasses.size());

        impl.unwind();

        assertEquals(0, impl.modifiedClassInfo.size());
        assertEquals(0, impl.modifiedClasses.size());

        assertEquals(2, retransformed.size());
        assertTrue(retransformed.contains(UnwindableInstrumentation.class));
        assertTrue(retransformed.contains(UnwindableInstrumentationImpl.class));
    }
}