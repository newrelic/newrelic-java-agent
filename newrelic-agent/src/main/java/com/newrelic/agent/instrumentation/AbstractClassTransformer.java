/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;

public abstract class AbstractClassTransformer implements StartableClassFileTransformer {
    private final int classreaderFlags;
    private final boolean enabled;

    public AbstractClassTransformer(int classreaderFlags) {
        this(classreaderFlags, true);
    }

    public AbstractClassTransformer(int classreaderFlags, boolean enabled) {
        super();
        this.enabled = enabled;
        this.classreaderFlags = classreaderFlags;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            if (!PointCutClassTransformer.isValidClassName(className)) {
                return null;
            }

            if (!matches(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)) {
                return null;
            }
            if (!isAbleToResolveAgent(loader, className)) {
                final String msg = MessageFormat.format(
                        "Not instrumenting {0}: class loader unable to load agent classes", className);
                Agent.LOG.log(Level.FINER, msg);
                return null;
            }
            byte[] classBytesWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(classfileBuffer,
                    classreaderFlags, loader);
            ClassReader cr = new ClassReader(classBytesWithUID);
            ClassWriter cw = InstrumentationUtils.getClassWriter(cr, loader);
            ClassVisitor classVisitor = getClassVisitor(cr, cw, className, loader);
            if (null == classVisitor) {
                return null;
            }
            cr.accept(classVisitor, classreaderFlags);
            final String msg = MessageFormat.format("Instrumenting {0}", className);
            Agent.LOG.finer(msg);

            return cw.toByteArray();
        } catch (StopProcessingException e) {
            String msg = MessageFormat.format("Instrumentation aborted for {0}: {1}", className, e);
            Agent.LOG.log(Level.FINER, msg, e);
            return null;
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, t, "Instrumentation error for {0}", className);
            return null;
        }
    }

    protected boolean isAbleToResolveAgent(ClassLoader loader, String className) {
        return InstrumentationUtils.isAbleToResolveAgent(loader, className);
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    @Override
    public void start(InstrumentationProxy instrumentation, boolean isRetransformSupported) {
        boolean canRetransform = isRetransformSupported && isRetransformSupported();
        if (isEnabled()) {
            instrumentation.addTransformer(this, canRetransform);
            start();
        }
    }

    /**
     * This is called by {@link #start(InstrumentationProxy, boolean)} after this transformer has been added to
     * {@link Instrumentation}.
     * 
     * @see Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer)
     */
    protected void start() {
    }

    /**
     * If this transformer is not enabled it won't be added to {@link Instrumentation}.
     * 
     */
    protected boolean isEnabled() {
        return enabled;
    }

    protected abstract boolean isRetransformSupported();

    protected abstract ClassVisitor getClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader);

    protected abstract boolean matches(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer);

}
