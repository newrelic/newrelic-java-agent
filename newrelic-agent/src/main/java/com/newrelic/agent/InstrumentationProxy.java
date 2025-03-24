/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.instrumentation.PointCutClassTransformer;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.InstrumentationWrapper;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.utils.BootstrapLoader;
import com.newrelic.weave.utils.Streams;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This interface wraps the normal {@link Instrumentation} interface and adds a few extra methods. In previous agent
 * versions it was used to create a consistent api across Java 5/6, but now it's kind of useless. We could remove this
 * class and move most of the logic it contains to a static helper.
 */
public class InstrumentationProxy extends InstrumentationWrapper {
    private final boolean bootstrapClassIntrumentationEnabled;

    protected InstrumentationProxy(Instrumentation instrumentation, boolean enableBootstrapClassInstrumentationDefault) {
        super(instrumentation);
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        bootstrapClassIntrumentationEnabled = config.getProperty(
                AgentConfigImpl.ENABLE_BOOTSTRAP_CLASS_INSTRUMENTATION, enableBootstrapClassInstrumentationDefault);
    }

    public static InstrumentationProxy getInstrumentationProxy(Instrumentation inst) {
        if (inst == null) {
            return null;
        }
        return new InstrumentationProxy(inst, true);
    }

    protected Instrumentation getInstrumentation() {
        return delegate;
    }

    @Override
    public void redefineClasses(ClassDefinition... definitions) throws ClassNotFoundException,
            UnmodifiableClassException {
        if (isRedefineClassesSupported()) {
            super.redefineClasses(definitions);
        }
    }

    public Class<?>[] retransformUninstrumentedClasses(String... classNames) throws UnmodifiableClassException,
            ClassNotFoundException {
        if (!isRetransformClassesSupported()) {
            return new Class<?>[0];
        }

        List<Class<?>> classList = new ArrayList<>(classNames.length);
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);
            if (!PointCutClassTransformer.isInstrumented(clazz)) {
                classList.add(clazz);
            }
        }

        Class<?>[] classArray = classList.toArray(new Class<?>[0]);
        if (!classList.isEmpty()) {
            retransformClasses(classArray);
        }

        return classArray;
    }

    public int getClassReaderFlags() {
        return ClassReader.EXPAND_FRAMES;
    }

    /**
     * Returns true if instrumentation should be turned on for bootstrap classes. This can be controlled with the agent
     * configuration switch 'enable_bootstrap_class_instrumentation'.
     * 
     */
    public final boolean isBootstrapClassInstrumentationEnabled() {
        return bootstrapClassIntrumentationEnabled;
    }

    public boolean isAppendToClassLoaderSearchSupported() {
        return true;
    }

    /**
     * The {@link #retransformClasses(Class...)} method is known to ignore retransform requests for bootstrap classes. A
     * call to {@link #redefineClasses(ClassDefinition...)} with the original class bytes seems to trigger the
     * retransform, and that's what this method does.
     */
    public static void forceRedefinition(Instrumentation instrumentation, Class<?>... classes)
            throws ClassNotFoundException, UnmodifiableClassException {

        List<ClassDefinition> toRedefine = new ArrayList<>();
        for (Class<?> clazz : classes) {
            String classResourceName = Utils.getClassResourceName(clazz);
            URL resource = clazz.getResource(classResourceName);
            if (resource == null) {
                resource = BootstrapLoader.get().findResource(classResourceName);
            }
            if (resource != null) {
                byte[] classfileBuffer;
                try {
                    classfileBuffer = Streams.read(resource.openStream(), true);

                    toRedefine.add(new ClassDefinition(clazz, classfileBuffer));
                } catch (Exception e) {
                    Agent.LOG.finer("Unable to redefine " + clazz.getName() + " - " + e.toString());
                }
            } else {
                Agent.LOG.finer("Unable to find resource to redefine " + clazz.getName());
            }
        }

        if (!toRedefine.isEmpty()) {
            instrumentation.redefineClasses(toRedefine.toArray(new ClassDefinition[0]));
        }
    }
}
