/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.deps.org.objectweb.asm.AnnotationVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.ClassReader;
import com.newrelic.agent.deps.org.objectweb.asm.ClassVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.ClassWriter;
import com.newrelic.agent.deps.org.objectweb.asm.MethodVisitor;
import com.newrelic.agent.deps.org.objectweb.asm.Type;
import com.newrelic.agent.deps.org.objectweb.asm.commons.Method;
import com.newrelic.agent.deps.org.objectweb.asm.tree.ClassNode;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.classmatchers.ScalaTraitMatcher;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.Annotation;
import com.newrelic.agent.instrumentation.tracing.NoticeSqlVisitor;
import com.newrelic.agent.instrumentation.tracing.TraceClassVisitor;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.agent.instrumentation.weaver.ClassWeaverService;
import com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPostprocessors;
import com.newrelic.agent.instrumentation.weaver.preprocessors.AgentPreprocessors;
import com.newrelic.agent.instrumentation.weaver.preprocessors.TracedWeaveInstrumentationTracker;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.api.agent.Trace;
import com.newrelic.bootstrap.BootstrapLoader;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ClassWeavedListener;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;
import com.newrelic.weave.weavepackage.NewClassAppender;
import com.newrelic.weave.weavepackage.PackageWeaveResult;
import org.junit.runners.model.InitializationError;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

class InstrumentingClassLoader extends WeavingClassLoader {

    private final ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails;

    public InstrumentingClassLoader(URLClassLoader parent, WeaveIncludes weaveIncludes, AgentConfig agentConfig)
            throws InitializationError {
        super(parent, weaveIncludes, ThrowingErrorTrapHandler.ERROR_HANDLER_NODE,
                AgentPreprocessors.createWithInstrumentationTitle(agentConfig, TEST_WEAVE_PACKAGE_NAME),
                new AgentPostprocessors());

        AgentPreprocessors preprocessor = (AgentPreprocessors) weavePreprocessor;
        tracedWeaveInstrumentationDetails = preprocessor.getTracedWeaveInstrumentationDetails();
        ((AgentPostprocessors) weavePostprocessor).setTracedWeaveInstrumentationDetails(tracedWeaveInstrumentationDetails);
    }

    public static class ThrowingErrorTrapHandler extends ErrorTrapHandler {

        public static final ClassNode ERROR_HANDLER_NODE;

        static {
            ClassNode result;
            try {
                result = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                        ThrowingErrorTrapHandler.class.getName(), ThrowingErrorTrapHandler.class.getClassLoader()));
            } catch (IOException e) {
                result = null;
            }

            ERROR_HANDLER_NODE = result;
        }

        /**
         * An error trap that rethrows the exception. Used only for testing as it is functionally equivalent to no error
         * trap.
         *
         * @throws Throwable
         */
        public static void onWeaverThrow(Throwable weaverError) throws Throwable {
            throw weaverError;
        }

    }

    @Override
    protected byte[] transform(String className) throws Exception {
        byte[] classBytes = WeaveUtils.getClassBytesFromClassLoaderResource(className, this);
        if (classBytes == null) {
            return null;
        }

        final InstrumentationContext context = new InstrumentationContext(classBytes, null, null);
        new ClassReader(classBytes).accept(new ScalaTraitMatcher().newClassMatchVisitor(null, null, null,null,  context)
                                             , ClassReader.SKIP_FRAMES);
        // weave
        byte[] weaved = weave(className, classBytes, context.getSkipMethods(), new ClassWeavedListener() {
            @Override
            public void classWeaved(PackageWeaveResult weaveResult, ClassLoader classloader, ClassCache cache) {
                if (weaveResult.weavedClass()) {
                    final String packageName = weaveResult.getValidationResult().getWeavePackage().getName();
                    for (String originalName : weaveResult.getWeavedMethods().keySet()) {
                        for (Method method : weaveResult.getWeavedMethods().get(originalName)) {
                            context.addWeavedMethod(method, packageName);
                        }
                        ClassWeaverService.addTraceInformation(
                                InstrumentingClassLoader.this.tracedWeaveInstrumentationDetails, packageName, context,
                                weaveResult.getComposite(), originalName);
                    }

                    try {
                        Map<String, byte[]> annotationProxyClasses = weaveResult.getAnnotationProxyClasses();
                        if (!annotationProxyClasses.isEmpty()) {
                            // Special case for annotation weaving in order to support dynamic annotation proxies. We
                            // need to add the dynamic proxy classes that we created to the current classloader here
                            NewClassAppender.appendClasses(classloader, annotationProxyClasses);
                        }
                    } catch (Exception e) {
                        Agent.LOG.log(Level.FINE, e, "Unable to add annotation proxy classes");
                    }
                }
            }
        });

        // trace
        if (weaved != null) {
            classBytes = weaved;
        }

        ClassReader reader = new ClassReader(classBytes);
        if (weaved == null) {
            // process trace annotations for non-weaved code
            reader.accept(new SimpleTraceMatchVisitor(null, context), ClassReader.EXPAND_FRAMES);
        }

        if (!context.isTracerMatch()) {
            if (weaved != null) {
                printClass(className, classBytes);
                return classBytes;
            }
            return null;
        }
        NoticeSqlVisitor noticeSqlVisitor = new NoticeSqlVisitor(WeaveUtils.ASM_API_LEVEL);
        reader.accept(noticeSqlVisitor, ClassReader.SKIP_FRAMES); // find the noticeSql calls

        String internalClassName = className.replace('.', '/');
        ClassWriter writer = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, context.getClassResolver(this));
        ClassVisitor cv = writer;
        cv = new TraceClassVisitor(cv, internalClassName, context, noticeSqlVisitor.getNoticeSqlMethods());
        cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                if (version < 49 || version > 100) { // Some weird Apache classes have really large versions.
                    version = WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION;
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        reader.accept(cv, ClassReader.EXPAND_FRAMES);

        byte[] result = writer.toByteArray();

        //printRaw(writer);
        //printClass(className, result);

        return result;
    }

    private void printRaw(ClassWriter writer) {
        ClassNode classNode = WeaveUtils.convertToClassNode(writer.toByteArray());
        classNode.accept(new com.newrelic.agent.deps.org.objectweb.asm.util.TraceClassVisitor(new PrintWriter(System.out)));
    }

    private void printClass(String className, byte[] classBytes) {
        try {
            File newClassFile = File.createTempFile(className.replace('/', '_'), ".new.class", BootstrapLoader.getTempDir());
            try (FileOutputStream fos = new FileOutputStream(newClassFile)) {
                fos.write(classBytes);
            }
            System.out.println("Wrote " + newClassFile.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Unable to write " + className);
        }
    }

    private static class SimpleTraceMatchVisitor extends ClassVisitor {
        private static final String TRACE_DESC = Type.getDescriptor(Trace.class);
        private final InstrumentationContext context;
        private String source;

        public SimpleTraceMatchVisitor(ClassVisitor cv, InstrumentationContext context) {
            super(WeaveUtils.ASM_API_LEVEL, cv);
            this.context = context;
        }

        @Override
        public void visitSource(String source, String debug) {
            super.visitSource(source, debug);
            this.source = source;
        }

        public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature,
                String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
            return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    AnnotationVisitor av = super.visitAnnotation(desc, visible);
                    if (TRACE_DESC.equals(desc)) {
                        av = new Annotation(av, TRACE_DESC, TraceDetailsBuilder.newBuilder().setInstrumentationType(
                                InstrumentationType.TraceAnnotation).setInstrumentationSourceName(source)) {

                            @Override
                            public void visitEnd() {
                                context.putTraceAnnotation(new Method(methodName, methodDesc), getTraceDetails(true));
                                super.visitEnd();
                            }

                        };
                    }
                    return av;
                }
            };
        }
    }
}
