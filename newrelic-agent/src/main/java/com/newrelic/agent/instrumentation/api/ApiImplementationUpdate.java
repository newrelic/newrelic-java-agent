/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.api;

import com.google.common.collect.ImmutableMultimap;
import com.newrelic.agent.instrumentation.classmatchers.ClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.classmatchers.OptimizedClassMatcher.Match;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.ContextClassTransformer;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Find classes that implement older versions of our API interfaces and add methods that were added in later releases.
 */
public class ApiImplementationUpdate implements ContextClassTransformer {

    private final DefaultApiImplementations defaultImplementations;
    private final ClassMatchVisitorFactory matcher;

    public static void setup(InstrumentationContextManager manager) throws Exception {
        ApiImplementationUpdate transformer = new ApiImplementationUpdate();
        manager.addContextClassTransformer(transformer.matcher, transformer);
    }

    protected ApiImplementationUpdate() throws Exception {
        defaultImplementations = new DefaultApiImplementations();
        matcher = new ClassMatchVisitorFactory() {

            @Override
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                    ClassReader reader, ClassVisitor cv, final InstrumentationContext context) {
                for (String name : reader.getInterfaces()) {
                    Map<Method, MethodNode> unmodifiableMethods = defaultImplementations.getApiClassNameToDefaultMethods().get(
                            name);
                    // we have a default implementation for this interface. Let's see if any methods are missing
                    if (unmodifiableMethods != null) {
                        // create a copy of the default methods so we can remove them as we see implementations
                        final Map<Method, MethodNode> methods = new HashMap<>(unmodifiableMethods);
                        cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                    String[] exceptions) {
                                // remove methods as we see them
                                methods.remove(new Method(name, desc));
                                return super.visitMethod(access, name, desc, signature, exceptions);
                            }

                            @Override
                            public void visitEnd() {
                                if (!methods.isEmpty()) {
                                    // methods are missing! Create a match that we'll use in the transform method. The
                                    // contents of the match isn't used, it just needs to exist.
                                    context.putMatch(matcher, new Match(
                                            ImmutableMultimap.<ClassAndMethodMatcher, String> of(), methods.keySet(),
                                            null));
                                }
                                super.visitEnd();
                            }

                        };
                    }
                }
                return cv;
            }

        };
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer, InstrumentationContext context, Match match)
            throws IllegalClassFormatException {

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = writer;
        for (String name : reader.getInterfaces()) {
            Map<Method, MethodNode> methods = defaultImplementations.getApiClassNameToDefaultMethods().get(name);
            if (methods != null) {
                // initially, the potential methods to add set is all methods on the api
                final Map<Method, MethodNode> methodsToAdd = new HashMap<>(methods);
                cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                            String[] exceptions) {
                        // remove methods as we see them
                        methodsToAdd.remove(new Method(name, desc));
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }

                    @Override
                    public void visitEnd() {
                        // if any methods are left, append them to the implementation
                        if (!methodsToAdd.isEmpty()) {
                            // create a new map to avoid modifying the map during iteration because visitMethod is
                            // called in the call to accept.
                            Map<Method, MethodNode> missingMethods = new HashMap<>(methodsToAdd);
                            for (Entry<Method, MethodNode> entry : missingMethods.entrySet()) {
                                entry.getValue().accept(this);
                            }
                        }
                        super.visitEnd();
                    }

                };
            }
        }
        reader.accept(cv, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    /**
     * For testing.
     */
    protected ClassMatchVisitorFactory getMatcher() {
        return matcher;
    }

}
