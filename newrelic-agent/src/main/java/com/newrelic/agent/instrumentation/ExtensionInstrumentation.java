/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.JarExtension;
import com.newrelic.agent.util.InstrumentationWrapper;

/**
 * This delegating wrapper of an {@link Instrumentation} instance registers two class transformers so that any class
 * transformer registered by a jar extension is hooked up in front of our class transformers so that it can add Trace
 * annotations which we then pick up.
 * 
 * @see JarExtension#create(com.newrelic.agent.logging.IAgentLogger, com.newrelic.agent.extension.ExtensionParsers,
 *      java.io.File)
 */
class ExtensionInstrumentation extends InstrumentationWrapper {

    private final MultiClassFileTransformer transformer = new MultiClassFileTransformer();
    private final MultiClassFileTransformer retransformingTransformer = new MultiClassFileTransformer();

    public ExtensionInstrumentation(Instrumentation delegate) {
        super(delegate);

        delegate.addTransformer(transformer);
        delegate.addTransformer(retransformingTransformer, true);
    }

    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        if (canRetransform) {
            this.retransformingTransformer.addTransformer(transformer);
        } else {
            this.transformer.addTransformer(transformer);
        }
    }

    public void addTransformer(ClassFileTransformer transformer) {
        this.transformer.addTransformer(transformer);
    }

    public boolean removeTransformer(ClassFileTransformer transformer) {
        if (!this.transformer.removeTransformer(transformer)) {
            return this.retransformingTransformer.removeTransformer(transformer);
        }
        return false;
    }

    private static final class MultiClassFileTransformer implements ClassFileTransformer {
        private final List<ClassFileTransformer> transformers = new CopyOnWriteArrayList<>();

        @Override
        public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

            if (!PointCutClassTransformer.isValidClassName(className)) {
                return null;
            }

            final byte[] originalBytes = classfileBuffer;

            for (ClassFileTransformer transformer : transformers) {
                try {
                    byte[] newBytes = transformer.transform(loader, className, classBeingRedefined, protectionDomain,
                            classfileBuffer);
                    if (null != newBytes) {
                        classfileBuffer = newBytes;
                    }
                } catch (Throwable t) {
                    Agent.LOG.log(Level.FINE, "An error occurred transforming class {0} : {1}", className,
                            t.getMessage());
                    Agent.LOG.log(Level.FINEST, t, t.getMessage());
                }
            }

            // if no bytes were modified, return null
            return originalBytes == classfileBuffer ? null : classfileBuffer;
        }

        public boolean removeTransformer(ClassFileTransformer transformer) {
            return transformers.remove(transformer);
        }

        public void addTransformer(ClassFileTransformer transformer) {
            transformers.add(transformer);
        }
    }

}
