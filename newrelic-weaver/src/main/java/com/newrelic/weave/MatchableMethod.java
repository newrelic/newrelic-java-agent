/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static com.newrelic.weave.MatchableMethod.Source.INTERFACE;
import static com.newrelic.weave.MatchableMethod.Source.ORIGINAL;
import static com.newrelic.weave.MatchableMethod.Source.SUPERCLASS;

/**
 * Most inherited methods cannot be weaved but may still be matched by an abstract method; this pairs a MethodNode with
 * that information and is intended for use only in ClassMatch.
 *
 * @see ClassMatch
 */
class MatchableMethod {
    /**
     * Indicates whether the polymorphic method declaration is in the original class, a super class, or simply declared
     * in an interface. This is used to determine the type of {@link com.newrelic.weave.violation.WeaveViolation} to
     * create if a user attempts to weave a method that is declared but does not exist in an original class.
     */
    enum Source {
        ORIGINAL, INTERFACE, SUPERCLASS
    }

    /**
     * Source of the the polymorphic method declaration.
     */
    public final Source source;

    /**
     * The ASM {@link MethodNode}.
     */
    public final MethodNode methodNode;

    /**
     * Whether or not this method should be "weaveable". Some methods can be declared in a weave class but not weaved,
     * e.g. abstract methods.
     */
    public final boolean isWeavable;

    private MatchableMethod(Source source, MethodNode methodNode, boolean isWeavable) {
        this.source = source;
        this.methodNode = methodNode;
        this.isWeavable = isWeavable;
    }

    /**
     * Given an original class, find all of the methods that should be considered for matching in a {@link ClassMatch}
     * using the specified {@link ClassCache} for inheritance hierarchy lookups and a flag indicating whether inherited
     * methods should be considered.
     * 
     * @param original original class
     * @param cache a @link ClassCache} for inheritance hierarchy lookups
     * @param isBaseMatch flag indicating whether inherited methods should be considered
     * @return a map of {@link MethodKey} to {@link MatchableMethod}, used by {@link ClassMatch} for quickly looking up
     *         which methods can be considered for matching
     * @throws IOException
     * @see ClassMatch#match()
     */
    static Map<MethodKey, MatchableMethod> findMatchableMethods(ClassNode original, ClassCache cache,
            boolean isBaseMatch) throws IOException {

        Map<MethodKey, MatchableMethod> methodMap = new HashMap<>();

        // original class
        boolean isInnerOriginal = WeaveUtils.isNonstaticInnerClass(original);
        for (MethodNode originalMethod : original.methods) {
            // Allow instrumentation to target specific lambdas (in exact match)
            if (isSyntheticOrBridge(originalMethod.access) && !originalMethod.name.contains("lambda$")) {
                continue;
            }

            String desc = originalMethod.desc;

            // inner (nonstatic nested) classes always take the parent class as an argument in the constructor
            // we adjust the method desc so that it looks like a static nested class constructor
            // this is only for matching purposes - this case is handled specially when weaving also
            if (isInnerOriginal && originalMethod.name.equals(WeaveUtils.INIT_NAME)) {
                Type[] constructorArgs = Type.getArgumentTypes(desc);
                Type[] truncatedArgs = Arrays.copyOfRange(constructorArgs, 1, constructorArgs.length);
                desc = Type.getMethodDescriptor(Type.VOID_TYPE, truncatedArgs);
            }

            boolean isAbstract = (originalMethod.access & Opcodes.ACC_ABSTRACT) != 0;
            boolean isWeavable = !isAbstract || isBaseMatch;
            MethodKey key = new MethodKey(originalMethod.name, desc);
            methodMap.put(key, new MatchableMethod(ORIGINAL, originalMethod, isWeavable));
        }

        // superclasses
        if (!original.superName.equals(WeaveUtils.JAVA_LANG_OBJECT_NAME)) {
            String superName = original.superName;
            while (!superName.equals(WeaveUtils.JAVA_LANG_OBJECT_NAME)) {
                if (!cache.hasClassResource(superName)) {
                    break;
                }
                ClassNode superNode = WeaveUtils.convertToClassNode(cache.getClassResource(superName));

                // add super methods that are required to have an implementation in their children
                // this includes abstract methods and methods that only throw exceptions
                for (MethodNode methodNode : superNode.methods) {
                    if (isSyntheticOrBridge(methodNode.access)) {
                        continue;
                    }
                    if (methodNode.name.equals(WeaveUtils.INIT_NAME) || methodNode.name.equals(
                            WeaveUtils.CLASS_INIT_NAME)) {
                        continue; // cannot use parent init/clinit
                    }
                    MethodKey key = new MethodKey(methodNode);
                    if (methodMap.containsKey(key)) {
                        continue; // we only need to check the most direct implementations
                    }
                    if ((methodNode.access & Opcodes.ACC_PRIVATE) != 0) {
                        continue; // private methods are not accessible in child classes
                    }
                    if (!isBaseMatch) {
                        methodMap.put(key, new MatchableMethod(SUPERCLASS, methodNode, false));
                        continue; // cannot weave if not a base match
                    }

                    if ((methodNode.access & Opcodes.ACC_FINAL) != 0) {
                        methodMap.put(key, new MatchableMethod(SUPERCLASS, methodNode, false));
                        continue; // final methods can not be weaved
                    }
                    if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) {
                        methodMap.put(key, new MatchableMethod(SUPERCLASS, methodNode, true));
                        continue; // abstract methods can be weaved
                    }

                    // unfortunately we need to allow weaving of concrete methods that only throw exceptions
                    // this requires us to examine the method body to check if there's a RETURN instruction
//                    boolean hasReturnInstruction = false;
//                    InsnList instructions = methodNode.instructions;
//                    int instructionsSize = instructions.size();
//                    for (int i = 0; i < instructionsSize; i++) {
//                        int opcode = instructions.get(i).getOpcode();
//                        if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
//                            hasReturnInstruction = true;
//                            break;
//                        }
//                    }
//                    methodMap.put(key, new MatchableMethod(SUPERCLASS, methodNode, !hasReturnInstruction));
                    // Basically allowing concrete base methods weaving.
                    methodMap.put(key, new MatchableMethod(SUPERCLASS, methodNode, true));
                }
                superName = superNode.superName;
            }
        }

        // interfaces
        if (original.interfaces != null && !original.interfaces.isEmpty()) {
            Set<String> processedInterfaces = new HashSet<>();
            Queue<String> queuedInterfaces = new LinkedList<>(original.interfaces);
            while (!queuedInterfaces.isEmpty()) {
                String currentInterface = queuedInterfaces.remove();
                if (!cache.hasClassResource(currentInterface)) {
                    continue;
                }
                ClassNode interfaceNode = WeaveUtils.convertToClassNode(cache.getClassResource(currentInterface));
                for (MethodNode methodNode : interfaceNode.methods) {
                    if (isSyntheticOrBridge(methodNode.access)) {
                        continue;
                    }
                    MethodKey key = new MethodKey(methodNode);
                    if (!methodMap.containsKey(key)) {
                        // we can never weave indirect interface methods because we cannot guarantee that an
                        // implementing class will implement them (they can be implemented in a superclass of the
                        // implementing class) - these can, however, be matched with an abstract method
                        methodMap.put(key, new MatchableMethod(INTERFACE, methodNode, false));
                    }
                }
                processedInterfaces.add(currentInterface);
                if (interfaceNode.interfaces != null && !interfaceNode.interfaces.isEmpty()) {
                    for (String nextInterface : interfaceNode.interfaces) {
                        if (!processedInterfaces.contains(nextInterface)) {
                            queuedInterfaces.add(nextInterface);
                        }
                    }
                }
            }
        }

        return methodMap;
    }

    private static boolean isSyntheticOrBridge(int access) {
        return (access & Opcodes.ACC_SYNTHETIC) != 0 || (access & Opcodes.ACC_BRIDGE) != 0;
    }
}
