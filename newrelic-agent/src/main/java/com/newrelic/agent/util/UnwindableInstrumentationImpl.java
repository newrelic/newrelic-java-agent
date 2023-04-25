package com.newrelic.agent.util;

import org.objectweb.asm.Type;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class UnwindableInstrumentationImpl extends DelegatingInstrumentation implements UnwindableInstrumentation {
    final Map<ClassFileTransformer, ClassFileTransformer> classFileTransformerMap =
            new ConcurrentHashMap<>();
    final Set<Class<?>> modifiedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>());
    final Set<ClassInfo> modifiedClassInfo = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final AtomicBoolean wrap = new AtomicBoolean(true);

    UnwindableInstrumentationImpl(Instrumentation instrumentation) {
        super(instrumentation);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        if (wrap.get() && isNewRelic(transformer)) {
            super.addTransformer(wrap(transformer), canRetransform);
        } else {
            super.addTransformer(transformer, canRetransform);
        }
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        if (wrap.get() && isNewRelic(transformer)) {
            super.addTransformer(wrap(transformer));
        } else {
            super.addTransformer(transformer);
        }
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        if (wrap.get() && isNewRelic(transformer)) {
            ClassFileTransformer wrapper = classFileTransformerMap.get(transformer);
            if (wrapper != null) {
                return super.removeTransformer(wrapper);
            } else {
                return super.removeTransformer(transformer);
            }
        } else {
            return super.removeTransformer(transformer);
        }
    }

    private ClassFileTransformer wrap(ClassFileTransformer transformer) {
        ClassFileTransformer wrapper = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
                final byte[] modifiedBytes = transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                if (wrap.get()) {
                    if (modifiedBytes != null && modifiedBytes.length != classfileBuffer.length) {
                        if (classBeingRedefined == null) {
                            modifiedClassInfo.add(new ClassInfo(loader, className));
                        } else {
                            modifiedClasses.add(classBeingRedefined);
                        }
                    }
                }
                return modifiedBytes;
            }
        };
        classFileTransformerMap.put(transformer, wrapper);
        return wrapper;
    }

    private static boolean isNewRelic(ClassFileTransformer transformer) {
        return transformer.getClass().getName().contains("newrelic");
    }

    @Override
    public void started() {
        wrap.set(false);
        modifiedClasses.clear();
        modifiedClassInfo.clear();
    }

    @Override
    public void unwind() {
        wrap.set(false);
        classFileTransformerMap.values().forEach(super::removeTransformer);
        classFileTransformerMap.clear();

        modifiedClassInfo.forEach(classInfo -> {
            try {
                modifiedClasses.add(classInfo.loadClass());
            } catch (ClassNotFoundException e) {
                // ignore
            }
        });

        modifiedClassInfo.clear();
        if (!modifiedClasses.isEmpty()) {
            try {
                super.retransformClasses(modifiedClasses.toArray(new Class[0]));
            } catch (UnmodifiableClassException e) {
            }
        }

        modifiedClasses.clear();
    }

    /**
     * Wrap instrumentation so that we can unwind our instrumentation if necessary.
     */
    public static Instrumentation wrapInstrumentation(final Instrumentation instrumentation) {
        final List<MethodDesc> missingInterfaceMethods = getMissingInterfaceMethods();

        final UnwindableInstrumentationImpl unwindableInstrumentation = new UnwindableInstrumentationImpl(instrumentation);
        if (missingInterfaceMethods.isEmpty()) {
            return unwindableInstrumentation;
        } else {
            return createProxyInstance(unwindableInstrumentation, instrumentation, missingInterfaceMethods);
        }
    }

    /**
     * Returns an UnwindableInstrumentation proxy instance that invokes the new java 9 Instrumentation methods
     * on the instrumentation instance, otherwise invoking using the unwindableInstrumentation instance.
     */
    static Instrumentation createProxyInstance(final UnwindableInstrumentation unwindableInstrumentation,
                                                       final Instrumentation instrumentation,
                                                       final List<MethodDesc> missingInterfaceMethods) {
        final Set<MethodDesc> missingMethods = new HashSet<>(missingInterfaceMethods);
        final InvocationHandler invocationHandler = (proxy, method, args) -> {
            if (missingMethods.contains(new MethodDesc(method))) {
                return method.invoke(instrumentation, args);
            } else {
                return method.invoke(unwindableInstrumentation, args);
            }
        };

        return (Instrumentation) Proxy.newProxyInstance(UnwindableInstrumentation.class.getClassLoader(),
                new Class[] { UnwindableInstrumentation.class}, invocationHandler);
    }

    /**
     * Return the 2 methods added to Instrumentation in java 9 that are missing from our implementation (if running
     * in 9+).
     */
    static List<MethodDesc> getMissingInterfaceMethods() {
        final List<MethodDesc> interfaceMethods = Arrays.asList(Instrumentation.class.getMethods())
                .stream()
                .map(MethodDesc::new)
                .collect(Collectors.toList());

        Arrays.asList(DelegatingInstrumentation.class.getDeclaredMethods())
                .stream()
                .map(MethodDesc::new)
                .forEach(interfaceMethods::remove);

        return interfaceMethods;
    }

    static class MethodDesc {
        private final String name;
        private final String desc;

        public MethodDesc(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        public MethodDesc(Method method) {
            this(method.getName(), Type.getMethodDescriptor(method));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodDesc that = (MethodDesc) o;
            return name.equals(that.name) && desc.equals(that.desc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, desc);
        }
    }

    private static class ClassInfo {
        private final ClassLoader classLoader;
        private final String className;

        public ClassInfo(ClassLoader classLoader, String className) {
            this.classLoader = classLoader;
            this.className = className;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassInfo classInfo = (ClassInfo) o;
            return Objects.equals(classLoader, classInfo.classLoader) && className.equals(classInfo.className);
        }

        @Override
        public int hashCode() {
            return Objects.hash(classLoader, className);
        }

        public Class<?> loadClass() throws ClassNotFoundException {
            ClassLoader classLoader = this.classLoader == null ? ClassLoader.getSystemClassLoader() : this.classLoader;
            return classLoader.loadClass(className);
        }
    }
}
