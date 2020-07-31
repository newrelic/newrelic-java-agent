/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;
import java.util.logging.Level;

import com.newrelic.agent.Agent;

/**
 * This delegating wrapper of an {@link Instrumentation} instance.
 */
public class InstrumentationWrapper implements Instrumentation {
    protected final Instrumentation delegate;

    public InstrumentationWrapper(Instrumentation delegate) {
        super();
        this.delegate = delegate;
    }

    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        delegate.addTransformer(transformer, canRetransform);
    }

    public void addTransformer(ClassFileTransformer transformer) {
        delegate.addTransformer(transformer);
    }

    public boolean removeTransformer(ClassFileTransformer transformer) {
        return delegate.removeTransformer(transformer);
    }

    public boolean isRetransformClassesSupported() {
        return delegate.isRetransformClassesSupported();
    }

    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be retransformed: ");
            for (Class<?> current : classes) {
                sb.append(current.getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        delegate.retransformClasses(classes);
    }

    public boolean isRedefineClassesSupported() {
        return delegate.isRedefineClassesSupported();
    }

    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
            UnmodifiableClassException {
        if (Agent.LOG.isFinestEnabled()) {
            StringBuilder sb = new StringBuilder("Classes about to be redefined: ");
            for (ClassDefinition current : definitions) {
                sb.append(current.getDefinitionClass().getName()).append(" ");
            }
            Agent.LOG.log(Level.FINEST, sb.toString());
        }
        delegate.redefineClasses(definitions);
    }

    public boolean isModifiableClass(Class<?> theClass) {
        return delegate.isModifiableClass(theClass);
    }

    @SuppressWarnings("rawtypes")
    public Class[] getAllLoadedClasses() {
        return delegate.getAllLoadedClasses();
    }

    @SuppressWarnings("rawtypes")
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return delegate.getInitiatedClasses(loader);
    }

    public long getObjectSize(Object objectToSize) {
        return delegate.getObjectSize(objectToSize);
    }

    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        delegate.appendToBootstrapClassLoaderSearch(jarfile);
    }

    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        delegate.appendToSystemClassLoaderSearch(jarfile);
    }

    public boolean isNativeMethodPrefixSupported() {
        return delegate.isNativeMethodPrefixSupported();
    }

    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        delegate.setNativeMethodPrefix(transformer, prefix);
    }

}
