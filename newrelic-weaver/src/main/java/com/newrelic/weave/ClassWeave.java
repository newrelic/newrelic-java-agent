/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.ObjectArrays;
import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.WeavePackage;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.io.File;

/**
 * Weaves a match (original and weave classes) into a target class.
 */
public class ClassWeave {
    private final PreparedMatch match;
    private final ClassNode target;
    private final boolean isNonstaticInnerTarget;
    private final List<Method> weavedMethods;
    private final Map<Method, Collection<String>> skipWeaveMethods;
    private final WeavePackage weavePackage;

    private ClassNode composite = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL);

    private ClassWeave(PreparedMatch match,
                       ClassNode target,
                       WeavePackage weavePackage,
                       Map<Method, Collection<String>> skipWeaveMethods) {
        this.match = match;
        this.target = target;
        this.isNonstaticInnerTarget = WeaveUtils.isNonstaticInnerClass(target);
        this.weavedMethods = new ArrayList<>(match.getPreparedMatchedMethods().size());
        this.skipWeaveMethods = skipWeaveMethods;
        this.weavePackage = weavePackage;
    }

    public static ClassWeave weave(PreparedMatch match,
                                   ClassNode target,
                                   WeavePackage weavePackage,
                                   Map<Method, Collection<String>> skipWeaveMethods) {
        ClassWeave result = new ClassWeave(match, target, weavePackage, skipWeaveMethods);
        result.weave();
        return result;
    }

    private void weave() {
        WeaveUtils.createReadableClassFileFromClassNode(target, false, target.name, "ClassLoader", "/Users/katherineanderson/Downloads");
        weavedMethods.clear();
        // copy target to composite without any jsr instructions
        target.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL, composite) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
            }
        });

        WeaveUtils.updateClassVersion(composite);

        // copy class annotations
        composite.visibleAnnotations = merge(match.getWeaveClassAnnotations().visibleAnnotations,
                target.visibleAnnotations);
        composite.invisibleAnnotations = merge(match.getWeaveClassAnnotations().invisibleAnnotations,
                target.invisibleAnnotations);

        // copy field annotations
        Map<String, PreparedMatch.AnnotationInfo> matchedWeaveFieldAnnotations = match.getMatchedWeaveFieldAnnotations();
        for (String fieldName : matchedWeaveFieldAnnotations.keySet()) {
            FieldNode compositeField = WeaveUtils.findMatch(composite.fields, fieldName);
            if (compositeField == null) {
                continue; // happens when weaving a child class in a base match and the parent has a matched field
            }
            PreparedMatch.AnnotationInfo weaveField = matchedWeaveFieldAnnotations.get(fieldName);
            compositeField.visibleAnnotations = merge(weaveField.visibleAnnotations, compositeField.visibleAnnotations);
            compositeField.invisibleAnnotations = merge(weaveField.invisibleAnnotations,
                    compositeField.invisibleAnnotations);
        }

        // This comes from either a default constructor in an interface or a @WeaveAllConstructor.
        MethodNode preparedWeaveAllConstructor = match.getPreparedWeaveAllConstructor();

        // weave matched methods
        for (MethodNode matchedMethod : match.getPreparedMatchedMethods().values()) {
            Collection<String> skipMethodOwningInterfaces =
              skipWeaveMethods.get(new Method(matchedMethod.name, matchedMethod.desc));
            if (skipMethodOwningInterfaces != null && skipMethodOwningInterfaces.contains(match.getWeaveName())) {
              continue; // this can happen when weaving an inherited base class
            }

            // If we have a prepared default constructor from an interface or a @WeavesAllConstructor,
            // skip "regular" weaving of constructors here.
            if (matchedMethod.name.equals(WeaveUtils.INIT_NAME) && preparedWeaveAllConstructor != null) {
                continue;
            }

            // nonstatic inner constructors will have an additional argument for their parent
            // account for this in the method desc so that we can find the correct constructor to weave
            String desc = matchedMethod.desc;
            if (matchedMethod.name.equals(WeaveUtils.INIT_NAME) && isNonstaticInnerTarget) {
                Type[] types = ObjectArrays.concat(WeaveUtils.getOuterClassType(target), Type.getArgumentTypes(desc));
                desc = Type.getMethodDescriptor(Type.VOID_TYPE, types);
            }

            // find target
            MethodNode targetMethod = WeaveUtils.findMatch(composite.methods, new Method(matchedMethod.name, desc));
            if (targetMethod == null) {
                continue; // this can happen when weaving an inherited base class
            }

            if ((targetMethod.access & Opcodes.ACC_BRIDGE) != 0) {
                // instead of weaving the bridge method, weave the method it invokes as long as we aren't already
                // weaving this method (or going to weave this method).
                Method newTarget = whereDoesTheBridgeGo(targetMethod);
                if (match.getPreparedMatchedMethods().containsKey(newTarget)) {
                    // This prevents a method from being weaved twice
                    continue;
                }
                MethodNode newTargetMethod = WeaveUtils.findMatch(composite.methods, newTarget);
                if (null != newTargetMethod) {
                    targetMethod = newTargetMethod;
                    matchedMethod = makeNewMatchForBridgeWeave(WeaveUtils.INLINER_PREFIX + match.getWeaveName(),
                            matchedMethod, newTarget);
                }
            }

            // weave match and target into a composite
            MethodNode compositeMethod = weaveMethod(matchedMethod, targetMethod);
            if (compositeMethod != null) {
                MethodNode match = WeaveUtils.findMatch(composite.methods, compositeMethod);
                int index = composite.methods.indexOf(match);
                if (index == -1) {
                    composite.methods.add(compositeMethod);
                } else {
                    composite.methods.remove(index);
                    composite.methods.add(index, compositeMethod);
                }
            }
        }

        // Insert prepared default constructor into all target constructors
        // This comes from either a default constructor in an interface or a @WeaveAllConstructor.
        if (preparedWeaveAllConstructor != null) {
            List<MethodNode> initMethods = new ArrayList<>();
            for (MethodNode targetMethod : composite.methods) {
                if (targetMethod.name.equals(WeaveUtils.INIT_NAME)) {
                    MethodNode compositeMethod = weaveMethod(preparedWeaveAllConstructor, targetMethod);
                    if (compositeMethod != null) {
                        initMethods.add(compositeMethod);
                    }
                }
            }
            for (MethodNode initMethod : initMethods) {
                MethodNode match = WeaveUtils.findMatch(composite.methods, initMethod);
                int index = composite.methods.indexOf(match);
                if (index == -1) {
                    composite.methods.add(initMethod);
                } else {
                    composite.methods.remove(index);
                    composite.methods.add(index, initMethod);
                }
            }
        }
    }

    /**
     * Use this to figure out which method a bridge method is calling.
     * 
     * @param bridgeMethod a potential bridge method
     * @return The bridge method target, or null if the given method isn't a bridge method
     */
    public static Method whereDoesTheBridgeGo(MethodNode bridgeMethod) {
        if ((bridgeMethod.access & Opcodes.ACC_BRIDGE) == 0) {
            return null;
        }
        MethodNode visitor = WeaveUtils.newMethodNode(bridgeMethod);
        bridgeMethod.accept(visitor);
        // a bridge method will have one method call. In certain languages there may be multiple method calls (such as
        // with Kotlin default parameter bridge methods) so we will need to handle those differently.
        MethodInsnNode lastMethodInsn = null;
        AbstractInsnNode current = visitor.instructions.getFirst();
        while (null != current) {
            if (current.getType() == AbstractInsnNode.METHOD_INSN) {
                lastMethodInsn = (MethodInsnNode) current;
            }
            current = current.getNext();
        }

        if (lastMethodInsn != null) {
            return new Method(lastMethodInsn.name, lastMethodInsn.desc);
        }
        return null;
    }

    /**
     * Create a new matcher method node for bridge methods.
     * 
     * @param bridgeMatchMethod
     * @param newTarget
     * @return
     */
    private MethodNode makeNewMatchForBridgeWeave(final String weaveClassName, final MethodNode bridgeMatchMethod,
            final Method newTarget) {
        final MethodNode result = WeaveUtils.newMethodNode(bridgeMatchMethod);
        bridgeMatchMethod.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (owner.equals(weaveClassName) && name.equals(bridgeMatchMethod.name)
                        && desc.equals(bridgeMatchMethod.desc)) {
                    result.name = newTarget.getName();
                    result.desc = newTarget.getDescriptor();
                    super.visitMethodInsn(opcode, owner, newTarget.getName(), newTarget.getDescriptor(), itf);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }
        });

        return result;
    }

    private MethodNode weaveMethod(final MethodNode weaveMethod, MethodNode targetMethod) {

        // disregard weave abstract methods
        if ((weaveMethod.access & Opcodes.ACC_ABSTRACT) != 0) {
            return targetMethod;
        }

        // don't weave into target abstract methods
        if ((targetMethod.access & Opcodes.ACC_ABSTRACT) != 0) {
            return targetMethod;
        }

        MethodNode composite = weaveMethod;
        composite.access = targetMethod.access;

        // Use the signature from the target to prevent issues caused by instrumentation
        // that utilizes generics when the target method doesn't use them.
        composite.signature = targetMethod.signature;

        // Use the "throws" clause from the target method to prevent issues with weaved
        // code using a subset of the throws clause, causing an UndeclaredThrowableException
        composite.exceptions = targetMethod.exceptions;

        final String weaveClassName = match.getWeaveName();
        final String originalClassName = match.getOriginalName();
        Set<MethodNode> toInline = new HashSet<>();
        if (weaveMethod.name.equals(WeaveUtils.INIT_NAME)) {
            // we weave every potential top-level constructor
            // only the actual top-level constructor will execute
            composite = MethodProcessors.addWeaveConstructorInvocationsAtEveryReturn(weaveMethod,
                    WeaveUtils.INLINER_PREFIX + weaveClassName,
                    originalClassName, targetMethod, isNonstaticInnerTarget);
            toInline.add(weaveMethod);

        } else {
            // replace callOriginal with actual original invocation
            toInline.add(targetMethod);
        }

        // inline original invocation
        composite = MethodProcessors.inlineMethods(WeaveUtils.INLINER_PREFIX + weaveClassName, toInline, target.name,
                composite);
        // the inliner sometimes like to sneak in some jsr instructions. Sneaky inliner!
        composite = MethodProcessors.removeJSRInstructions(composite);

        // make matched fields/method calls operate on the original class
        composite = MethodProcessors.updateOwner(composite, originalClassName, weaveClassName,
                match.getPreparedMatchedMethods().keySet(), match.getMatchedWeaveFieldAnnotations().keySet());

        // copy annotations from weave
        composite.invisibleAnnotations = merge(weaveMethod.invisibleAnnotations, targetMethod.invisibleAnnotations);
        composite.visibleAnnotations = merge(weaveMethod.visibleAnnotations, targetMethod.visibleAnnotations);
        for (int i = 0; i < Type.getArgumentTypes(composite.desc).length; i++) {

            if (composite.visibleParameterAnnotations != null) {
                composite.visibleParameterAnnotations[i] = merge(safeGet(weaveMethod.visibleParameterAnnotations, i),
                        safeGet(targetMethod.visibleParameterAnnotations, i));
            }

            if (composite.invisibleParameterAnnotations != null) {
                composite.invisibleParameterAnnotations[i] = merge(safeGet(weaveMethod.invisibleParameterAnnotations, i),
                        safeGet(targetMethod.invisibleParameterAnnotations, i));
            }
        }
        weavedMethods.add(new Method(targetMethod.name, targetMethod.desc));
        return composite;
    }

    private static List<AnnotationNode> safeGet(List<AnnotationNode>[] annotations, int i) {        
        return annotations == null ? null : annotations[i];
    }

    private static List<AnnotationNode> merge(List<AnnotationNode> weave, List<AnnotationNode> composite) {
        if (weave == null || weave.isEmpty()) {
            return composite;
        }

        if (composite == null || composite.isEmpty()) {
            return new ArrayList<>(weave);
        }

        Map<String, AnnotationNode> annotationMap = new HashMap<>();
        for (AnnotationNode compositeAnnotation : composite) {
            annotationMap.put(compositeAnnotation.desc, compositeAnnotation);
        }
        for (AnnotationNode weaveAnnotation : weave) {
            annotationMap.put(weaveAnnotation.desc, weaveAnnotation);
        }
        return new ArrayList<>(annotationMap.values());
    }

    /**
     * The <i>composite</i> ClassNode, representing the fully weaved class.
     * @return fully weaved class
     */
    public ClassNode getComposite() {
        return composite;
    }

    /**
     * The {@link PreparedMatch} that was used to generate this match.
     * @return
     */
    public PreparedMatch getMatch() {
        return match;
    }

    /**
     * The methods this class weave wrote bytecode into the last time weave() was called.
     */
    public List<Method> getWeavedMethods() {
        return this.weavedMethods;
    }
}
