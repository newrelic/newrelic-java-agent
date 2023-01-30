package com.newrelic.agent.util;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.jar.JarFile;

public class DelegatingInstrumentation implements Instrumentation {
    protected final Instrumentation delegate;

    public DelegatingInstrumentation(Instrumentation instrumentation) {
        this.delegate = instrumentation;
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer, boolean canRetransform) {
        delegate.addTransformer(transformer, canRetransform);
    }

    @Override
    public void addTransformer(ClassFileTransformer transformer) {
        delegate.addTransformer(transformer);
    }

    @Override
    public boolean removeTransformer(ClassFileTransformer transformer) {
        return delegate.removeTransformer(transformer);
    }

    @Override
    public boolean isRetransformClassesSupported() {
        return delegate.isRetransformClassesSupported();
    }

    @Override
    public void retransformClasses(Class<?>... classes) throws UnmodifiableClassException {
        delegate.retransformClasses(classes);
    }

    @Override
    public boolean isRedefineClassesSupported() {
        return delegate.isRedefineClassesSupported();
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException, UnmodifiableClassException {
        delegate.redefineClasses(definitions);
    }

    @Override
    public boolean isModifiableClass(Class<?> theClass) {
        return delegate.isModifiableClass(theClass);
    }

    @Override
    public Class[] getAllLoadedClasses() {
        return delegate.getAllLoadedClasses();
    }

    @Override
    public Class[] getInitiatedClasses(ClassLoader loader) {
        return delegate.getInitiatedClasses(loader);
    }

    @Override
    public long getObjectSize(Object objectToSize) {
        return delegate.getObjectSize(objectToSize);
    }

    @Override
    public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
        delegate.appendToBootstrapClassLoaderSearch(jarfile);
    }

    @Override
    public void appendToSystemClassLoaderSearch(JarFile jarfile) {
        delegate.appendToSystemClassLoaderSearch(jarfile);
    }

    @Override
    public boolean isNativeMethodPrefixSupported() {
        return delegate.isNativeMethodPrefixSupported();
    }

    @Override
    public void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix) {
        delegate.setNativeMethodPrefix(transformer, prefix);
    }
}
