/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import com.newrelic.agent.Agent;
import com.newrelic.agent.InstrumentationProxy;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.instrumentation.classmatchers.ClassMatcher;
import com.newrelic.agent.instrumentation.classmatchers.ExactClassMatcher;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.logging.IAgentLogger;

public abstract class AbstractImplementationClassTransformer implements StartableClassFileTransformer {

    protected final IAgentLogger logger;
    protected final int classreaderFlags;
    protected final PointCutClassTransformer classTransformer;
    private final ClassMatcher classMatcher;
    protected final Class interfaceToImplement;
    protected final String originalInterface;
    protected final Type originalInterfaceType;
    private final boolean enabled;
    private final ClassMatcher skipClassMatcher;

    public AbstractImplementationClassTransformer(PointCutClassTransformer classTransformer, boolean enabled,
            Class interfaceToImplement, ClassMatcher classMatcher, ClassMatcher skipMatcher,
            String originalInterfaceName) {
        logger = Agent.LOG.getChildLogger(PointCutClassTransformer.class);
        this.classMatcher = classMatcher;
        this.skipClassMatcher = skipMatcher;
        this.interfaceToImplement = interfaceToImplement;
        this.originalInterface = originalInterfaceName;
        this.originalInterfaceType = Type.getObjectType(originalInterface);
        this.enabled = enabled;

        this.classTransformer = classTransformer;
        classreaderFlags = classTransformer.getClassReaderFlags();
    }

    public AbstractImplementationClassTransformer(PointCutClassTransformer classTransformer, boolean enabled,
            Class interfaceToImplement) {
        this(classTransformer, enabled, interfaceToImplement, getClassMatcher(interfaceToImplement),
                getSkipClassMatcher(interfaceToImplement), getOriginalInterface(interfaceToImplement));
    }

    private static String getOriginalInterface(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        return interfaceMapper.originalInterfaceName();
    }

    private static ClassMatcher getClassMatcher(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        if (interfaceMapper.className().length == 0) {
            return new ExactClassMatcher(interfaceMapper.originalInterfaceName());
        } else {
            return ExactClassMatcher.or(interfaceMapper.className());
        }
    }

    private static ClassMatcher getSkipClassMatcher(Class interfaceToImplement) {
        InterfaceMapper interfaceMapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        return ExactClassMatcher.or(interfaceMapper.skip());
    }

    @Override
    public void start(InstrumentationProxy instrumentation, boolean isRetransformSupported) {
        if (enabled) {
            instrumentation.addTransformer(this, false);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

        if (!PointCutClassTransformer.isValidClassName(className)) {
            return null;
        }

        final boolean isLoggable = false; // Agent.LOG.isLoggable(Level.FINEST); // Even FINEST causes too much logging
        if (classBeingRedefined != null) {
            // this is a redefine - adding fields not permitted
            if (isLoggable) {
                Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: class being redefined", className));
            }
            return null;
        }
        if ((loader == null) && (AgentBridge.getAgent().getClass().getClassLoader() != null)) {
            if (isLoggable) {
                Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: bootstrap classloader", className));
            }
            // we can't add our classes to classes loaded by the bootstrap classloader unless we are on the bootstrap
            // classpath
            return null;
        }
        ClassReader cr = new ClassReader(classfileBuffer);
        if (InstrumentationUtils.isInterface(cr)) {
            // we can't add implementation methods to interfaces
            if (isLoggable) {
                Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: class is an interface", className));
            }
            return null;
        }
        if (skipClassMatcher.isMatch(loader, cr)) {
            if (isLoggable) {
                Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: skip class match", className));
            }
            return null;
        }
        boolean matches = classMatcher.isMatch(loader, cr);

        try {
            if (!matches) {
                if (!isGenericInterfaceSupportEnabled()) {
                    if (isLoggable) {
                        Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: generic interface not supported",
                                className));
                    }
                    return null;
                }
                if (excludeClass(className)) {
                    if (isLoggable) {
                        Agent.LOG.finest(MessageFormat.format("Not instrumenting {0}: class excluded", className));
                    }
                    return null;
                }
                // skip classes which don't directly implement the original interface
                if (!matches(cr, originalInterface)) {
                    if (isLoggable) {
                        Agent.LOG.finest(MessageFormat.format(
                                "Not instrumenting {0}: class does not implement {1} directly", className,
                                originalInterface));
                    }
                    return null;
                }
                // What if we don't hit any returns above?
            }
            if (!InstrumentationUtils.isAbleToResolveAgent(loader, className)) {
                if (Agent.LOG.isLoggable(Level.FINER)) {
                    String msg = MessageFormat.format(
                            "Not instrumenting {0}: class loader unable to load agent classes", className);
                    Agent.LOG.log(Level.FINER, msg);
                }
                return null;
            }
            if ("org/eclipse/jetty/server/Request".equals(className)) {
                className.hashCode();
            }
            byte[] classBytesWithUID = InstrumentationUtils.generateClassBytesWithSerialVersionUID(cr,
                    classreaderFlags, loader);
            ClassReader crWithUID = new ClassReader(classBytesWithUID);
            ClassWriter cwWithUID = InstrumentationUtils.getClassWriter(crWithUID, loader);
            cr.accept(createClassVisitor(crWithUID, cwWithUID, className, loader), classreaderFlags);
            if (isLoggable) {
                Agent.LOG.finest(MessageFormat.format("Instrumented {0} with {1}", className,
                        interfaceToImplement.getName()));
            }
            return cwWithUID.toByteArray();
        } catch (StopProcessingException e) {
            String msg = MessageFormat.format("Instrumentation aborted for {0} - {1} ", className, e);
            Agent.LOG.finer(msg);
            return null;
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINER, t, "Instrumentation error for {0}", className);
            return null;
        }
    }

    protected boolean excludeClass(String className) {
        return classTransformer.isExcluded(className);
    }

    protected boolean isGenericInterfaceSupportEnabled() {
        return true;
    }

    protected int getClassReaderFlags() {
        return classreaderFlags;
    }

    protected abstract ClassVisitor createClassVisitor(ClassReader cr, ClassWriter cw, String className,
            ClassLoader loader);

    private boolean matches(ClassReader cr, String interfaceNameToMatch) {
        String[] interfaces = cr.getInterfaces();
        if (interfaces != null) {
            for (String interfaceName : interfaces) {
                if (interfaceNameToMatch.equals(interfaceName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
