/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.instrumentation.pointcuts.InterfaceMapper;
import com.newrelic.agent.util.Annotations;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class InterfaceImplementationClassTransformer extends AbstractImplementationClassTransformer {

    private final Map<Method, java.lang.reflect.Method> methods;

    public static StartableClassFileTransformer[] getClassTransformers(PointCutClassTransformer classTransformer) {
        Collection<Class<?>> interfaces = new Annotations().getInterfaceMapperClasses();
        List<StartableClassFileTransformer> transformers = new ArrayList<>(
                interfaces.size());

        for (Class interfaceClass : interfaces) {
            transformers.add(new InterfaceImplementationClassTransformer(classTransformer, true, interfaceClass));
        }

        return transformers.toArray(new StartableClassFileTransformer[0]);
    }

    private final boolean genericInterfaceSupportEnabled;

    public InterfaceImplementationClassTransformer(PointCutClassTransformer classTransformer, boolean enabled,
            Class interfaceToImplement) {
        super(classTransformer, enabled, interfaceToImplement);
        boolean genericInterfaceSupportEnabled = true;

        Map<Method, java.lang.reflect.Method> methods2 = Collections.emptyMap();
        InterfaceMapper mapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        /*
         * In 3.x Agents through 3.12.0, the default value of mapper.classVisitor() was
         * InterfaceImplementationClassVisitor.class. Then as part of the fix for JAVA-609, the InterfaceMapper was
         * moved to the bootstrap class loader. Since classes on the bootstrap must form a closed set with respect to
         * load-time dependency resolution, leaving that default value would have forced us to pull all of ASM on to the
         * bootstrap - a terrible idea. So the default value was changed to Object.class, and callers who care now have
         * to check and substitute the intended default value.
         */
        Class<?> visitorClass = mapper.classVisitor();
        if (visitorClass == Object.class) {
            visitorClass = InterfaceImplementationClassVisitor.class;
        }
        if (visitorClass == InterfaceImplementationClassVisitor.class) {
            genericInterfaceSupportEnabled = false;
            methods2 = MethodMappersAdapter.getMethodMappers(interfaceToImplement);
        }
        methods = Collections.unmodifiableMap(methods2);

        this.genericInterfaceSupportEnabled = genericInterfaceSupportEnabled && mapper.className().length == 0;
    }

    @Override
    protected boolean isGenericInterfaceSupportEnabled() {
        return genericInterfaceSupportEnabled;
    }

    @Override
    protected ClassVisitor createClassVisitor(ClassReader cr, ClassWriter cw, String className, ClassLoader loader) {
        InterfaceMapper mapper = (InterfaceMapper) interfaceToImplement.getAnnotation(InterfaceMapper.class);
        Set<Method> methods2 = new HashSet<>(methods.keySet());
        /*
         * In 3.x Agents through 3.12.0, the default value of mapper.classVisitor() was
         * InterfaceImplementationClassVisitor.class. Then as part of the fix for JAVA-609, the InterfaceMapper was
         * moved to the bootstrap class loader. Since classes on the bootstrap must form a closed set with respect to
         * load-time dependency resolution, leaving that default value would have forced us to pull all of ASM on to the
         * bootstrap - a terrible idea. So the default value was changed to Object.class, and callers who care now have
         * to check and substitute the intended default value.
         */
        Class<?> classVisitorClass = mapper.classVisitor();
        if (classVisitorClass == Object.class) {
            classVisitorClass = InterfaceImplementationClassVisitor.class;
        }
        if (InterfaceImplementationClassVisitor.class == classVisitorClass) {
            ClassVisitor classVisitor = new AddInterfaceAdapter(cw, className, interfaceToImplement);
            classVisitor = RequireMethodsAdapter.getRequireMethodsAdaptor(classVisitor, methods2, className,
                    interfaceToImplement.getName(), loader);
            classVisitor = MethodMappersAdapter.getMethodMappersAdapter(classVisitor, methods, originalInterface,
                    className);
            return classVisitor;
        }
        if (ClassVisitor.class.isAssignableFrom(mapper.classVisitor())) {
            try {
                Constructor constructor = mapper.classVisitor().getConstructor(ClassVisitor.class, String.class);
                return (ClassVisitor) constructor.newInstance(cw, className);
            } catch (Throwable e) {
                Agent.LOG.log(Level.FINEST, "while creating ClassVisitor for InterfaceMapper transformation", e);
            }
        }
        Agent.LOG.log(Level.FINEST, "Unable to create ClassVisitor (type {0}) for {1} with loader {2}",
                classVisitorClass, className, loader);
        return cw;
    }

    public class InterfaceImplementationClassVisitor extends ClassVisitor {

        public InterfaceImplementationClassVisitor(int api) {
            super(api);
        }
    }
}
