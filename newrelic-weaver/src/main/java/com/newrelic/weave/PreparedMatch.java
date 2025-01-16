/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uses the result of ClassMatch to prepare the weave & extension class for ClassWeave. This can save some processing
 * because we only need to prepare weave & extension once for non-exact match types.
 */
public class PreparedMatch {
    private static final String ANONYMOUS_CLASS_FORMAT = "com/newrelic/weave/%s_%d_nr_anon";
    public static final String NR_WEAVE = "_nr_weave_";

    // these are copied from ClassMatch so we don't have to keep a reference to it and it can be GCed
    private final String originalName;
    private final String weaveName;
    private final String weaveSuperName;
    private final AnnotationInfo weaveClassAnnotations;
    private final Map<String, AnnotationInfo> matchedWeaveFieldAnnotations;
    private final Set<String> newInnerClasses;

    private final Set<String> newFields;
    private final Set<String> matchedFields;
    private final Set<MethodNode> newMethods = new HashSet<>();
    private final Map<String, String> anonymousInnerClassTypeMap = new HashMap<>();
    private final Map<Method, MethodNode> preparedMatchedMethods = new HashMap<>();
    private final PreparedExtension extension;
    private final boolean removeLineNumbers;
    private final Map<String, AnnotationNode> getClassAnnotationMap;
    private final Map<MethodKey, Map<String, AnnotationNode>> getMethodAnnotationMap;
    private final Set<MethodNode> classAnnotationGetters;
    private final Map<MethodKey, Map<String, AnnotationNode>> methodAnnotationGetters;
    private final Map<String, ClassNode> annotationProxyClasses = new HashMap<>();
    private MethodNode preparedWeaveAllConstructor;

    private ClassNode errorHandleClassNode = ErrorTrapHandler.NO_ERROR_TRAP_HANDLER;
    private ClassNode extensionClassTemplate = ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE;

    private PreparedMatch(ClassMatch match, ClassNode errorHandleClassNode, ClassNode extensionTemplate,
            boolean removeLineNumbers) {
        this(match, extensionTemplate, removeLineNumbers);
        if (errorHandleClassNode != ErrorTrapHandler.NO_ERROR_TRAP_HANDLER) {
            // Make sure the error trap class is good to use
            MethodNode method = WeaveUtils.getMethodNode(errorHandleClassNode, ErrorTrapHandler.HANDLER_METHOD_NAME,
                    ErrorTrapHandler.HANDLER_METHOD_DESC);
            if (null != method && ErrorTrapHandler.class.getCanonicalName().replace('.', '/').equals(
                    errorHandleClassNode.superName) && (method.access & Opcodes.ACC_STATIC) != 0) {
                this.errorHandleClassNode = errorHandleClassNode;
            }
        }
    }

    private PreparedMatch(ClassMatch match, ClassNode extensionTemplate, boolean removeLineNumbers) {
        // copy data from the class match
        this.removeLineNumbers = removeLineNumbers;
        this.originalName = match.getOriginal().name;
        this.weaveName = match.getWeave().name;
        this.weaveSuperName = match.getWeave().superName;
        this.weaveClassAnnotations = new AnnotationInfo(match.getWeave().visibleAnnotations,
                match.getWeave().invisibleAnnotations);
        this.matchedWeaveFieldAnnotations = new HashMap<>();
        for (String fieldName : match.getMatchedFields()) {
            FieldNode weaveField = WeaveUtils.findRequiredMatch(match.getWeave().fields, fieldName);
            AnnotationInfo annotationInfo = new AnnotationInfo(weaveField.visibleAnnotations,
                    weaveField.invisibleAnnotations);
            matchedWeaveFieldAnnotations.put(fieldName, annotationInfo);
        }
        this.newInnerClasses = match.getNewInnerClasses();
        this.extensionClassTemplate = extensionTemplate;
        this.newFields = match.getNewFields();
        this.matchedFields = match.getMatchedFields();
        this.getClassAnnotationMap =  match.getClassAnnotationMap();
        this.classAnnotationGetters = match.getClassAnnotationGetters();
        this.methodAnnotationGetters = match.getMethodAnnotationMap();
        this.getMethodAnnotationMap = match.getMethodAnnotationMap();


        MethodNode weavesAllMethod = match.getWeavesAllMethod();

        // create set of new methods
        for (Method newMethod : match.getNewMethods()) {
            if (weavesAllMethod != null && weavesAllMethod.name.equals(newMethod.getName())
                    && weavesAllMethod.desc.equals(newMethod.getDescriptor())) {
                continue;
            }

            MethodNode newMethodNode = WeaveUtils.findMatch(match.getWeave().methods, newMethod);
            if (newMethodNode != null) {
                newMethods.add(newMethodNode);
            }
        }

        // if weave has anonymous inner classes, generate type map to rewrite their names
        if (!match.getNewInnerClasses().isEmpty()) {
            for (String newClassName : match.getNewInnerClasses()) {
                for (InnerClassNode innerClassNode : match.getWeave().innerClasses) {
                    if (innerClassNode.name.equals(newClassName) && WeaveUtils.isAnonymousInnerClass(innerClassNode)) {
                        String anonName = String.format(ANONYMOUS_CLASS_FORMAT, newClassName, System.identityHashCode(
                                this));
                        anonymousInnerClassTypeMap.put(newClassName, anonName);
                    }
                }
            }
        }

        // create extension if necessary
        extension = match.getNewFields().size() > 0 ? new PreparedExtension(match, this.extensionClassTemplate) : null;
    }

    /**
     * Create a {@link PreparedMatch} for the specified {@link ClassMatch} using the specified error handler and
     * extension template. This class assumes that the {@link ClassMatch} is valid; if it contains any
     * {@link com.newrelic.weave.violation.WeaveViolation}s, you will get undefined results for obvious reasons.
     *
     * @param match valid {@link ClassMatch} to prepare for weaving
     * @param errorHandle {@link ClassNode} containing the appropriate error trap to use, see {@link ErrorTrapHandler}
     * @param extensionTemplate extension template to use, see {@link ExtensionClassTemplate} and
     *        {@link PreparedExtension} for more information
     * @param removeLineNumbers whether or not to strip line numbers from the weave class
     * @return resulting {@link PreparedMatch}
     */
    public static PreparedMatch prepare(ClassMatch match, ClassNode errorHandle, ClassNode extensionTemplate,
            boolean removeLineNumbers) {
        PreparedMatch result = new PreparedMatch(match, errorHandle, extensionTemplate, removeLineNumbers);
        result.prepare(match);
        return result;
    }

    private void prepare(ClassMatch match) {
        if (match.weavesAllMethods()) {
            preparedMatchedMethods.putAll(buildWeaveAllMatches(match));
        }

        // prepare matched weave methods
        for (Method matchedMethod : match.getMatchedMethods()) {
            preparedMatchedMethods.put(matchedMethod, prepare(match, WeaveUtils.findMatch(match.getWeave().methods, matchedMethod), match.getOriginalReplacements()));
        }

        // prepare default constructor for interface matches and matches that have valid @WeaveAllConstructors
        if (match.isInterfaceMatch() || match.weavesAllConstructors()) {
            preparedWeaveAllConstructor = prepare(match, WeaveUtils.findMatch(match.getWeave().methods, WeaveUtils.DEFAULT_CONSTRUCTOR), match.getOriginalReplacements());
        }
    }

    private Map<Method, MethodNode> buildWeaveAllMatches(ClassMatch match) {
        Map<Method, MethodNode> weavesAllMatches = new HashMap<>();

        for (Map.Entry<MethodNode, MethodNode> weaveAllMatch : match.getWeaveAllMatches().entrySet()) {
            final MethodNode originalMethod = weaveAllMatch.getKey();
            MethodNode weavesAllMethodsNode = weaveAllMatch.getValue();

            final MethodNode weaveCode = WeaveUtils.copy(weavesAllMethodsNode);
            weaveCode.name = originalMethod.name;
            weaveCode.desc = originalMethod.desc;
            weaveCode.signature = originalMethod.signature;
            weaveCode.access = originalMethod.access;
            weaveCode.exceptions = originalMethod.exceptions;
            weaveCode.parameters = originalMethod.parameters;
            weaveCode.maxStack += originalMethod.maxStack;
            weaveCode.maxLocals += originalMethod.maxLocals;


            final List<LocalVariableNode> localVariableNodes = new ArrayList<>();

            // Copy parameter locals
            final boolean isStatic = (originalMethod.access & Opcodes.ACC_STATIC) != 0;
            // >> 2 is to remove the size of the return value. See
            // http://asm.ow2.org/asm40/javadoc/user/org/objectweb/asm/Type.html#getArgumentsAndReturnSizes()
            // we subtract 1 for static methods to account for the "this" variable
            final int argumentsSize = (Type.getArgumentsAndReturnSizes(originalMethod.desc) >> 2) - (isStatic ? 1 : 0);
            for (LocalVariableNode localVariableNode : originalMethod.localVariables) {
                if (localVariableNode.index < argumentsSize) {
                    localVariableNodes.add(localVariableNode);
                }
            }

            for (LocalVariableNode localVariableNode : weaveCode.localVariables) {
                // Namespace our local variables to avoid collisions
                localVariableNode.name = NR_WEAVE + localVariableNode.name;
                localVariableNodes.add(localVariableNode);
            }

            weaveCode.localVariables.clear();
            weaveCode.localVariables.addAll(localVariableNodes);

            final MethodNode result = WeaveUtils.newMethodNode(weaveCode);

            weaveCode.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {

                @Override
                public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
                        int index) {
                    // Any locals from the weaved code will be given a new index = (current + argumentsSize)
                    if (name.startsWith(NR_WEAVE)) {
                        super.visitLocalVariable(name, desc, signature, start, end, (index + argumentsSize));
                    } else {
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }
                }

                @Override
                public void visitVarInsn(int opcode, int var) {
                    super.visitVarInsn(opcode, var + argumentsSize);
                }

                @Override
                public void visitIincInsn(int var, int increment) {
                    super.visitIincInsn(var + argumentsSize, increment);
                }

                @Override
                public void visitParameter(String name, int access) {
                    // Don't!
                }
            });

            // Add Weaver.callOriginal(), followed by a CHECKCAST instruction (if returning something
            //  that is not a java.lang.Object), followed by an unboxing operation (if needed).
            result.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    WeaveUtils.WEAVER_TYPE.getInternalName(), WeaveUtils.CALL_ORIGINAL_METHOD.getName(),
                    WeaveUtils.CALL_ORIGINAL_METHOD.getDescriptor(), false));

            Type returnType = Type.getReturnType(originalMethod.desc);
            if (Type.VOID_TYPE.equals(returnType)) {
                result.instructions.add(new InsnNode(Opcodes.POP));
            }
            else {
                String returnTypeClassInternalName = WeaveUtils.getClassInternalName(returnType);

                AbstractInsnNode checkCastInstruction = WeaveUtils.getCheckCastInstruction(returnTypeClassInternalName);
                // null when returning a java.lang.Object
                if (checkCastInstruction != null) {
                    result.instructions.add(checkCastInstruction);
                }

                MethodInsnNode unboxingInstruction = WeaveUtils.getUnboxingInstruction(returnType);
                // null when returning a reference, or something that doesn't need unboxing
                if (unboxingInstruction != null) {
                    result.instructions.add(unboxingInstruction);
                }
            }

            result.instructions.add(new InsnNode(WeaveUtils.getReturnOpcodeForReturnType(returnType)));

            CallOriginalReplacement replacement = CallOriginalReplacement.replace(match.getWeave().name, result);
            Method method = new Method(originalMethod.name, originalMethod.desc);

            if (classAnnotationGetters.contains(weavesAllMethodsNode)) {
                classAnnotationGetters.add(result);
            }
            if (methodAnnotationGetters.containsKey(new MethodKey(weavesAllMethodsNode))) {
                classAnnotationGetters.add(result);
            }

            MethodNode preparedMethod = prepare(match, result, Collections.singletonMap(method, replacement));
            weavesAllMatches.put(method, preparedMethod);
        }

        return weavesAllMatches;
    }

    private MethodNode prepare(ClassMatch classMatch, MethodNode matchedMethod, Map<Method, CallOriginalReplacement> callOriginalReplacementMap) {
        // remove JSR instructions
        MethodNode prepared = MethodProcessors.removeJSRInstructions(matchedMethod);

        LabelNode startOfOriginal = null;
        LabelNode endOfOriginal = null;
        // handle original invocation
        if (prepared.name.equals(WeaveUtils.INIT_NAME)) {

            // for constructors, simply extract all instructions after the first <init> prepared call
            prepared = MethodProcessors.extractConstructorInstructionsAfterInit(prepared);
            prepared.instructions.resetLabels();

            // write the error trap
            prepared = ErrorTrapWeaveMethodsProcessor.writeErrorTrap(prepared, errorHandleClassNode, startOfOriginal,
                    endOfOriginal);

            // It's very important that we do this here, otherwise the LabelNodes created for the `CallOriginalReplacement` and for the
            // `ErrorTrapWeaveMethodsProcessor` will be incorrectly re-used causing instructions after that label to get dropped. This can manifest as
            // an "absent code attribute" error from the JVM.
            prepared.instructions.resetLabels();

            // inline the error handler
            prepared = MethodProcessors.inlineMethods(errorHandleClassNode.name,
                    ErrorTrapHandler.NO_ERROR_TRAP_HANDLER == errorHandleClassNode ? Collections.<MethodNode> emptySet()
                            : this.errorHandleClassNode.methods, weaveName, prepared);
        } else {

            // for normal methods, see if we created a CallOriginalReplacement during validation in ClassMatch
            CallOriginalReplacement replacementResult = callOriginalReplacementMap.get(new Method(prepared.name,
                    prepared.desc));
            if (replacementResult != null && replacementResult.isSuccess()) {
                startOfOriginal = replacementResult.getStartOfOriginalMethodLabelNode();
                endOfOriginal = replacementResult.getEndOfOriginalMethodLabelNode();

                prepared = replacementResult.getResult();

                // write the error trap
                prepared = ErrorTrapWeaveMethodsProcessor.writeErrorTrap(prepared, errorHandleClassNode,
                        startOfOriginal, endOfOriginal);

                // It's very important that we do this here, otherwise the LabelNodes created for the `CallOriginalReplacement` and for the
                // `ErrorTrapWeaveMethodsProcessor` will be incorrectly re-used causing instructions after that label to get dropped. This can manifest as
                // an "absent code attribute" error from the JVM.
                prepared.instructions.resetLabels();

                // inline the error handler
                prepared = MethodProcessors.inlineMethods(errorHandleClassNode.name,
                        ErrorTrapHandler.NO_ERROR_TRAP_HANDLER == errorHandleClassNode
                                ? Collections.<MethodNode> emptySet() : this.errorHandleClassNode.methods, weaveName,
                        prepared);
            }

        }

        Collection<AnnotationNode> classAnnotations = getClassAnnotationMap.values();
        Collection<AnnotationNode> methodAnnotations = Collections.emptyList();
        Map<String, AnnotationNode> methodAnnotationsMap = getMethodAnnotationMap.get(new MethodKey(prepared));
        if (methodAnnotationsMap != null) {
            methodAnnotations = methodAnnotationsMap.values();
        }

        // Replace any Weaver.getClassAnnotation()/Weaver.getMethodAnnotation calls with inlined code to return a proxy of the annotation
        prepared = MethodProcessors.renameGetAnnotationCalls(prepared, classMatch.getOriginal(), classAnnotations,
                methodAnnotations, annotationProxyClasses);

        // strip line numbers from weave code
        if (removeLineNumbers) {
            prepared = MethodProcessors.removeLineNumbers(prepared);
        }

        // inline new methods
        if (!newMethods.isEmpty()) {
            prepared = MethodProcessors.inlineMethods(weaveName, newMethods, weaveName, prepared);
        }

        // update inner class constructor args to use original instead of weave class (e.g. this$0)
        prepared = MethodProcessors.updateConstructorArgsForInnerClass(prepared, weaveName, originalName,
                classMatch.getNewInnerClasses());

        // update anonymous inner class names so they won't conflict with any anonymous inner classes in the target
        if (!anonymousInnerClassTypeMap.isEmpty()) {
            prepared = MethodProcessors.updateTypes(prepared, anonymousInnerClassTypeMap);
        }

        // rewrite new fields to use the extension class
        if (extension != null) {
            prepared = extension.rewriteNewFieldCalls(prepared);
        }

        return prepared;
    }

    /**
     * Prepares a <i>new</i> inner class (i.e. child of the weave class for this match) so it can be loaded into a
     * classloader. New inner classes can access new fields (but NOT new methods) in their parent, and anonymous inner
     * classes need to have thier names rewritten so they don't conflict with those in the original or target. This
     * rewrites new inner classes so all of this works.
     *
     * <p>
     * New inner classes should be validated with {@link ClassMatch} before calling this method.
     * </p>
     *
     * <p>
     * This method is not to be used with <i>matched</i> inner classes; they have more restrictions and will be modified
     * through the standard weave process.
     * </p>
     *
     * @param newInnerClassNode new inner class
     * @return processed inner class that can be loaded into a classloader
     * @see ClassMatch#validateNewInnerClass(ClassNode)
     */
    public ClassNode prepareNewInnerClass(final ClassNode newInnerClassNode) {
        // rewrite new fields
        if (null != extension && null != newInnerClassNode.methods) {
            for (MethodNode method : newInnerClassNode.methods) {
                extension.rewriteNewFieldCalls(method);
            }
        }
        ClassNode anonClass = new SynchronizedClassNode(WeaveUtils.ASM_API_LEVEL);

        // remaps anonymous inner class names and maps weave -> original
        Map<String, String> typeMap = new HashMap<>(anonymousInnerClassTypeMap);
        typeMap.put(weaveName, originalName);
        ClassVisitor remapper = new ClassRemapper(anonClass, new SimpleRemapper(typeMap));

        // inlines new methods and rewrites new fields to use the extension class
        ClassVisitor rewriteNewVisitor = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, remapper) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                super.visit(version, makePublic(access), name, signature, superName, interfaces);
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                return super.visitField(makePublic(access), name, desc, signature, value);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                return super.visitMethod(makePublic(access), name, desc, signature, exceptions);
            }
        };

        // note the delegation order - we will rewrite new methods/fields BEFORE remapping types
        newInnerClassNode.accept(rewriteNewVisitor);
        return anonClass;
    }

    private static int makePublic(int access) {
        access |= Opcodes.ACC_PUBLIC;
        access &= ~(Opcodes.ACC_PRIVATE + Opcodes.ACC_PROTECTED);
        return access;
    }

    public String nameNewInnerClass(String originalName) {
        String anonName = anonymousInnerClassTypeMap.get(originalName);
        return anonName == null ? originalName : anonName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getWeaveName() {
        return weaveName;
    }

    public String getWeaveSuperName() {
        return weaveSuperName;
    }

    public Map<Method, MethodNode> getPreparedMatchedMethods() {
        return preparedMatchedMethods;
    }

    public Set<String> getNewFields() {
        return this.newFields;
    }

    public Set<String> getMatchedFields() {
        return this.matchedFields;
    }

    public MethodNode getPreparedWeaveAllConstructor() {
        return preparedWeaveAllConstructor;
    }

    public PreparedExtension getExtension() {
        return extension;
    }

    public AnnotationInfo getWeaveClassAnnotations() {
        return weaveClassAnnotations;
    }

    public Map<String, AnnotationInfo> getMatchedWeaveFieldAnnotations() {
        return matchedWeaveFieldAnnotations;
    }

    public Set<String> getNewInnerClasses() {
        return newInnerClasses;
    }

    public Map<String, ClassNode> getAnnotationProxyClasses() {
        return annotationProxyClasses;
    }

    /**
     * Stores annotation information from the weave class, so that they can be merged with target annotations in
     * {@link ClassWeave}.
     */
    static class AnnotationInfo {
        final List<AnnotationNode> visibleAnnotations;
        final List<AnnotationNode> invisibleAnnotations;

        AnnotationInfo(List<AnnotationNode> visibleAnnotations, List<AnnotationNode> invisibleAnnotations) {
            this.visibleAnnotations = visibleAnnotations;
            this.invisibleAnnotations = invisibleAnnotations;
        }
    }
}
