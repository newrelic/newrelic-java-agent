/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import static com.newrelic.agent.Agent.LOG;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.weave.utils.WeaveUtils;

public class TraceClassTransformer implements ContextClassTransformer {

    public TraceClassTransformer() {
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match)
            throws IllegalClassFormatException {
        try {
            if (!PointCutClassTransformer.isValidClassName(className)) {
                return null;
            }
            return doTransform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer, context);
        } catch (Throwable t) {
            LOG.log(Level.FINE, "Unable to transform class " + className, t);
            return null;
        }
    }

    private byte[] doTransform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context)
            throws IllegalClassFormatException {

        if (!context.isTracerMatch()) {
            return null;
        }

        LOG.debug("Instrumenting class " + className);
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = writer;

        // Find and store all methods in the class that have the noticeSql() call
        NoticeSqlVisitor noticeSqlVisitor = new NoticeSqlVisitor(WeaveUtils.ASM_API_LEVEL);
        reader.accept(noticeSqlVisitor, ClassReader.SKIP_FRAMES);

        cv = new TraceClassVisitor(cv, className, context, noticeSqlVisitor.getNoticeSqlMethods());
        reader.accept(cv, ClassReader.EXPAND_FRAMES);

        //Utils.print(writer.toByteArray());

        return writer.toByteArray();
    }
}
