/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.InstrumentedClass;
import com.newrelic.agent.instrumentation.InstrumentedMethod;
import com.newrelic.agent.instrumentation.PointCut;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.WeavedMethod;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher;
import com.newrelic.agent.instrumentation.tracing.TraceDetails;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.bootstrap.BootstrapLoader;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class FinalClassTransformer implements ContextClassTransformer {

    /**
     * These classes make the IBM class verifier fail when we upgrade the classes to version 7 (51) so we should only
     * upgrade them to Java 6 so we don't invoke the new verifier.
     */
    private static final Set<String> CLASSES_TO_SKIP_VERSION_UPGRADE = ImmutableSet.of(
            "org/eclipse/core/runtime/internal/adaptor/ContextFinder");

    private static final Set<String> ANNOTATIONS_TO_REMOVE = ImmutableSet.of(
            Type.getDescriptor(InstrumentedClass.class), Type.getDescriptor(InstrumentedMethod.class),
            Type.getDescriptor(WeavedMethod.class));

    // used by the functional tests
    private static ClassChecker CLASS_CHECKER = null;

    public static void setClassChecker(ClassChecker classChecker) {
        CLASS_CHECKER = classChecker;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context,
            OptimizedClassMatcher.Match match) throws IllegalClassFormatException {
        try {
            if (!PointCutClassTransformer.isValidClassName(className)) {
                return null;
            }
            return getFinalTransformation(loader, className, classBeingRedefined, classfileBuffer, context);
        } catch (Throwable ex) {
            Agent.LOG.log(Level.FINE, "Unable to transform " + className, ex);
        }
        return null;
    }

    private byte[] getFinalTransformation(ClassLoader loader, String className, Class<?> classBeingRedefined,
            byte[] classfileBuffer, InstrumentationContext context) {

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, context.getClassResolver(loader));

        ClassVisitor cv = writer;

        if (!context.getWeavedMethods().isEmpty()) {
            cv = new MarkWeaverMethodsVisitor(cv, context);
        }

        cv = addModifiedClassAnnotation(cv, context);
        cv = addModifiedMethodAnnotation(cv, context, loader);

        cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                if (version < 49 || version > 100) { // Some weird Apache classes have really large versions.
                    int newVersion = WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION;
                    if (CLASSES_TO_SKIP_VERSION_UPGRADE.contains(name)) {
                        newVersion = WeaveUtils.JAVA_6_CLASS_VERSION;
                    }

                    Agent.LOG.log(Level.FINEST, "Converting {0} from version {1} to {2}", name, version, newVersion);
                    version = newVersion;
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access,
                        name, desc, signature, exceptions);
            }

        };

        cv = skipExistingAnnotations(cv);

        cv = CurrentTransactionRewriter.rewriteCurrentTransactionReferences(cv, reader);

        reader.accept(cv, ClassReader.SKIP_FRAMES);

        byte[] classBytes = writer.toByteArray();

        if (CLASS_CHECKER != null) {
            CLASS_CHECKER.check(classBytes);
        }

        if (Agent.isDebugEnabled()) {
            writeClassFiles(className, context, classBytes);
        }

        addSupportabilityMetrics(reader, className, context);

        Agent.LOG.finer("Final transformation of class " + className);
        return classBytes;
    }

    private void writeClassFiles(String className, InstrumentationContext context, byte[] classBytes) {
        PrintWriter oldFileWriter = null;
        PrintWriter newFileWriter = null;
        try {
            File old = File.createTempFile(className.replace('/', '_'), ".old", BootstrapLoader.getTempDir());
            oldFileWriter = new PrintWriter(old);
            Utils.print(context.bytes, oldFileWriter);
            Agent.LOG.debug("Wrote " + old.getAbsolutePath());

            File newFile = File.createTempFile(className.replace('/', '_'), ".new", BootstrapLoader.getTempDir());
            newFileWriter = new PrintWriter(newFile);
            Utils.print(classBytes, newFileWriter);
            Agent.LOG.debug("Wrote " + newFile.getAbsolutePath());

            File newClassFile = File.createTempFile(className.replace('/', '_'), ".new.class", BootstrapLoader.getTempDir());
            try (FileOutputStream fos = new FileOutputStream(newClassFile)) {
                fos.write(classBytes);
            }
            Agent.LOG.debug("Wrote " + newClassFile.getAbsolutePath());
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, t, "Error writing debug bytecode for {0}", className);
        } finally {
            if (oldFileWriter != null) oldFileWriter.close();
            if (newFileWriter != null) newFileWriter.close();
        }
    }

    private ClassVisitor skipExistingAnnotations(ClassVisitor cv) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (ANNOTATIONS_TO_REMOVE.contains(desc)) {
                    return null;
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, super.visitMethod(access, name, desc, signature,
                        exceptions)) {

                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (ANNOTATIONS_TO_REMOVE.contains(desc)) {
                            return null;
                        }
                        return super.visitAnnotation(desc, visible);
                    }

                };
            }

        };
    }

    // Record that we applied "user" instrumentation to a class. Note: generates metric names based on class
    // names. But should not cause a metric explosion because auto-generated classes are filtered by the
    // ClassFileTransformer and never reach this point. If a metric explosion originates here, the root cause
    // is a failure of our proxy-filtering algorithm.
    private void addSupportabilityMetrics(ClassReader reader, String className, InstrumentationContext context) {
        StatsService statsService = ServiceFactory.getStatsService();
        if (statsService != null) {
            for (Method m : context.getTimedMethods()) {
                TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(m);
                if (traceDetails != null && traceDetails.isCustom()) {
                    String metricOrigin = MessageFormat.format(
                            MetricNames.SUPPORTABILITY_INSTRUMENT, className.replace('/', '.'), m.getName(),
                            m.getDescriptor());
                    statsService.doStatsWork(StatsWorks.getRecordMetricWork(metricOrigin, 1), metricOrigin);
                }
            }
        }
    }

    private ClassVisitor addModifiedClassAnnotation(ClassVisitor cv, final InstrumentationContext context) {
        AnnotationVisitor visitAnnotation = cv.visitAnnotation(Type.getDescriptor(InstrumentedClass.class), true);

        if (context.isUsingLegacyInstrumentation()) {
            visitAnnotation.visit("legacy", Boolean.TRUE);
        }
        if (context.hasModifiedClassStructure()) {
            visitAnnotation.visit("classStructureModified", Boolean.TRUE);
        }
        visitAnnotation.visitEnd();

        return cv;
    }

    private ClassVisitor addModifiedMethodAnnotation(ClassVisitor cv, final InstrumentationContext context,
            final ClassLoader loader) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            /**
             * @see InstrumentedMethod#instrumentationTypes()
             * @see InstrumentedMethod#instrumentationNames()
             * @see InstrumentedMethod#dispatcher()
             */
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                Method method = new Method(name, desc);
                if (context.isModified(method)) {
                    if (loader != null) {
                        TraceDetails traceDetails = context.getTraceInformation().getTraceAnnotations().get(method);
                        boolean dispatcher = false;
                        if (traceDetails != null) {
                            dispatcher = traceDetails.dispatcher();
                        }

                        AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(InstrumentedMethod.class), true);
                        av.visit("dispatcher", dispatcher);
                        List<String> instrumentationNames = new ArrayList<>();
                        List<InstrumentationType> instrumentationTypes = new ArrayList<>();
                        Level logLevel = Level.FINER;

                        if (traceDetails != null) {
                            if (traceDetails.instrumentationSourceNames() != null) {
                                instrumentationNames.addAll(traceDetails.instrumentationSourceNames());
                            }
                            if (traceDetails.instrumentationTypes() != null) {
                                for (InstrumentationType type : traceDetails.instrumentationTypes()) {
                                    instrumentationTypes.add(type);
                                }
                            }
                            if (traceDetails.isCustom()) {
                                logLevel = Level.FINE;
                            }
                        }
                        PointCut pointCut = context.getOldStylePointCut(method);
                        if (pointCut != null) {
                            instrumentationNames.add(pointCut.getClass().getName());
                            instrumentationTypes.add(InstrumentationType.Pointcut);
                        }
                        Collection<String> instrumentationPackages = context.getMergeInstrumentationPackages(method);
                        if (instrumentationPackages != null && !instrumentationPackages.isEmpty()) {
                            for (String current : instrumentationPackages) {
                                instrumentationNames.add(current);
                                instrumentationTypes.add(InstrumentationType.WeaveInstrumentation);
                            }
                        }

                        if (instrumentationNames.size() == 0) {
                            instrumentationNames.add("Unknown");
                            Agent.LOG.finest("Unknown instrumentation source for " + className + '.' + method);
                        }
                        if (instrumentationTypes.size() == 0) {
                            instrumentationTypes.add(InstrumentationType.Unknown);
                            Agent.LOG.finest("Unknown instrumentation type for " + className + '.' + method);
                        }

                        AnnotationVisitor visitArrayName = av.visitArray("instrumentationNames");
                        for (String current : instrumentationNames) {
                            // the key on this is ignored
                            visitArrayName.visit("", current);
                        }
                        visitArrayName.visitEnd();

                        AnnotationVisitor visitArrayType = av.visitArray("instrumentationTypes");
                        for (InstrumentationType type : instrumentationTypes) {
                            // the key on this is ignored
                            visitArrayType.visitEnum("", Type.getDescriptor(InstrumentationType.class),
                                    type.toString());
                        }
                        visitArrayType.visitEnd();

                        av.visitEnd();

                        if (Agent.LOG.isLoggable(logLevel)) {
                            Agent.LOG.log(logLevel, "Instrumented " + Type.getObjectType(className).getClassName() + '.'
                                    + method + ", " + instrumentationTypes + ", " + instrumentationNames);
                        }
                    }
                }
                return mv;
            }
        };
    }

}
