/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.newrelic.weave.violation.WeaveViolationType.CLASS_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_EXTENDS_ILLEGAL_SUPERCLASS;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_IMPLEMENTS_ILLEGAL_INTERFACE;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_MISSING_REQUIRED_ANNOTATIONS;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_NESTED_NONSTATIC_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_WEAVE_IS_INTERFACE;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_FIELD_ACCESS_VIOLATION;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_METHOD_ACCESS_VIOLATION;
import static com.newrelic.weave.violation.WeaveViolationType.ENUM_NEW_FIELD;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_FINAL_ASSIGNMENT;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_FINAL_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_PRIVATE_BASE_CLASS_MATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_SERIALVERSIONUID_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_STATIC_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_TYPE_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.INCOMPATIBLE_BYTECODE_VERSION;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_ILLEGAL_CALL_ORIGINAL;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_NEW_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_BASE_CONCRETE_WEAVE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_EXACT_ABSTRACT_WEAVE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_INDIRECT_INTERFACE_WEAVE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_MISSING_REQUIRED_ANNOTATIONS;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NATIVE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_ABSTRACT_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_INVOKE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_NON_PRIVATE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_RETURNTYPE_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_STATIC_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_SYNTHETIC_WEAVE_ILLEGAL;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_THROWS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.MULTIPLE_WEAVE_ALL_METHODS;
import static com.newrelic.weave.violation.WeaveViolationType.NON_STATIC_WEAVE_INTO_ALL_METHODS;
import static com.newrelic.weave.violation.WeaveViolationType.NON_VOID_NO_PARAMETERS_WEAVE_ALL_METHODS;

/**
 * Matches an original class with a weave class and checks for API violations. This class is not threadsafe.
 */
public class ClassMatch {
    private static final String SERIAL_VERSION_UID_FIELD_NAME = "serialVersionUID";

    private final ClassNode original;
    private final ClassNode weave;
    private final boolean isBaseMatch;
    private final boolean isInterfaceMatch;
    private final Map<MethodKey, MatchableMethod> matchableMethods;
    private final Set<String> allOriginalInterfaces;
    private final List<WeaveViolation> violations = new ArrayList<>();

    /**
     * To match, original class must have at least on of these class or method annotations.
     */
    private final Set<String> requiredClassAnnotations;
    private final Set<String> requiredMethodAnnotations;

    private final Set<String> newFields = Sets.newHashSetWithExpectedSize(1);
    private final Set<String> matchedFields = Sets.newHashSetWithExpectedSize(3);

    private final Set<Method> newMethods = Sets.newHashSetWithExpectedSize(5);
    private final Set<Method> matchedMethods = Sets.newHashSetWithExpectedSize(5);
    private final Map<Method, GeneratedNewFieldMethod> generatedNewFieldMethods = Maps.newHashMapWithExpectedSize(1);

    private final Set<String> newInnerClasses = Sets.newHashSetWithExpectedSize(0);
    private final Set<String> matchedInnerClasses = Sets.newHashSetWithExpectedSize(0);

    private Map<String, AnnotationNode> classAnnotationMap = new HashMap<>();

    // these are created here during validation but used in PreparedMatch
    private MethodNode extensionClassInit;
    private final Map<Method, CallOriginalReplacement> originalReplacements = new HashMap<>();

    private boolean weavesAllConstructors;
    private MethodNode weavesAllMethod;
    private Map<MethodNode, MethodNode> weaveAllMatchedMethods = new HashMap<>();

    private Set<MethodNode> classAnnotationGetters = new HashSet<>();
    private Map<MethodKey, Map<String, AnnotationNode>> methodsToAnnotations = new HashMap<>();

    private boolean fatalWeaveViolation = false;

    /**
     * Match a class with a weaved class.
     *
     * @param original ASM ClassNode representing the original class
     * @param weave ASM ClassNode representing the class to weave
     * @param isBaseMatch whether or not the match type should consider the original class a superclass/interface
     * @param matchableMethods matchable method map
     * @param requiredClassAnnotations annotations required to match this weave class
     */
    private ClassMatch(ClassNode original, ClassNode weave, boolean isBaseMatch,
            Map<MethodKey, MatchableMethod> matchableMethods, Set<String> allOriginalInterfaces,
            Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations) {
        this.original = original;
        this.weave = weave;
        this.requiredClassAnnotations = requiredClassAnnotations;
        this.requiredMethodAnnotations = requiredMethodAnnotations;
        this.isBaseMatch = isBaseMatch;
        this.isInterfaceMatch = (original.access & Opcodes.ACC_INTERFACE) != 0;
        this.matchableMethods = matchableMethods;
        this.allOriginalInterfaces = allOriginalInterfaces;
    }

    /**
     * Match a class with a weaved class.
     *
     * @param original ASM ClassNode representing the original class
     * @param weave ASM ClassNode representing the class to weave
     * @param isBaseMatch whether or not the match type should consider the original class a superclass/interface
     * @param requiredClassAnnotations
     * @param cache ClassCache used to lookup supertype/interface hierarchies for base class matching
     */
    public static ClassMatch match(ClassNode original, ClassNode weave, boolean isBaseMatch,
            Set<String> requiredClassAnnotations, Set<String> requiredMethodAnnotations, ClassCache cache)
            throws IOException {
        Map<MethodKey, MatchableMethod> matchableMethods = MatchableMethod.findMatchableMethods(original, cache,
                isBaseMatch);
        Set<String> allOriginalInterfaces = ClassInformation.fromClassNode(original).getAllInterfaces(cache);
        ClassMatch result = new ClassMatch(original, weave, isBaseMatch, matchableMethods, allOriginalInterfaces,
                requiredClassAnnotations, requiredMethodAnnotations);
        result.match(cache);
        return result;
    }

    private void match(ClassCache cache) {
        // class-level validation
        // cannot annotate an interface
        if ((weave.access & Opcodes.ACC_INTERFACE) != 0) {
            addViolation(CLASS_WEAVE_IS_INTERFACE);
            fatalWeaveViolation = true;
            return;
        }

        if ((weave.access & Opcodes.ACC_ENUM) != 0) {
            processEnumMethods();
        }

        if (weave.version > WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION) {
            addViolation(INCOMPATIBLE_BYTECODE_VERSION);
        }

        boolean isWeaveWithAnnotation = !requiredClassAnnotations.isEmpty() || !requiredMethodAnnotations.isEmpty();

        // cannot increase class visibility
        if ((original.access & Opcodes.ACC_PUBLIC) != (weave.access & Opcodes.ACC_PUBLIC)) {
            // WeaveWithAnnotation class access does not need to match original class access
            if (!isWeaveWithAnnotation) {
                addViolation(CLASS_ACCESS_MISMATCH);
            }
        }

        // weave can only implement interfaces that the original implements
        if (weave.interfaces.size() > 0) {
            for (String weaveInterface : weave.interfaces) {
                if (!allOriginalInterfaces.contains(weaveInterface)) {
                    addViolation(CLASS_IMPLEMENTS_ILLEGAL_INTERFACE);
                }
            }
        }

        if (!requiredClassAnnotations.isEmpty()) {
            processRequiredClassAnnotations(cache);
        }

        if (requiredClassAnnotations.isEmpty() && !requiredMethodAnnotations.isEmpty()) {
            processRequiredMethodAnnotations(cache);
        }

        // weave can only extend the exact superclass that the original extends
        if (!weave.superName.equals(WeaveUtils.JAVA_LANG_OBJECT_NAME) && !weave.superName.equals(original.superName)) {
            addViolation(CLASS_EXTENDS_ILLEGAL_SUPERCLASS);
        }

        // weave cannot be a non-static inner class
        if (WeaveUtils.isNonstaticInnerClass(weave)) {
            addViolation(CLASS_NESTED_NONSTATIC_UNSUPPORTED);
        }

        weavesAllMethod = findAndValidateWeaveIntoAllMethod();

        // identify new, anonymous, and matched inner classes
        Map<String, InnerClassNode> originalInnerClasses = new HashMap<>();
        for (InnerClassNode innerClassNode : original.innerClasses) {
            originalInnerClasses.put(innerClassNode.name, innerClassNode);
        }
        for (InnerClassNode weaveInnerClass : weave.innerClasses) {
            String name = weaveInnerClass.name;
            if (weave.name.equals(name)) {
                continue; // if the weave class IS an inner class, it will be listed - skip it
            }

            if (weaveInnerClass.outerName != null && !weaveInnerClass.outerName.equals(weave.name)) {
                continue; // we're using an inner class from another class - skip it
            }

            if (WeaveUtils.isAnonymousInnerClass(weaveInnerClass)) {
                newInnerClasses.add(name); // all anon classes are new
            } else if (originalInnerClasses.containsKey(name)) {
                matchedInnerClasses.add(name);
            } else {
                newInnerClasses.add(name);
            }
        }

        // identify new and matched fields
        Map<String, FieldNode> originalFields = new HashMap<>();
        for (FieldNode originalField : original.fields) {
            originalFields.put(originalField.name, originalField);
        }
        for (FieldNode weaveField : weave.fields) {
            if (weaveField.name.equals(SERIAL_VERSION_UID_FIELD_NAME)) {
                addViolation(FIELD_SERIALVERSIONUID_UNSUPPORTED, weaveField);
                continue;
            }

            FieldNode originalField = originalFields.get(weaveField.name);
            if (originalField == null) {
                newFields.add(weaveField.name);
            } else {
                validateMatchedField(originalField, weaveField);
                matchedFields.add(weaveField.name);
            }
        }

        if ((weave.access & Opcodes.ACC_ENUM) != 0) {
            if (newFields.size() > 0) {
                addViolation(ENUM_NEW_FIELD);
            }
        }

        // identify new and matched methods
        MethodNode clinit = null;
        for (MethodNode weaveMethod : weave.methods) {
            if (weaveMethod.name.equals(WeaveUtils.CLASS_INIT_NAME)) {
                clinit = weaveMethod;
                continue; // we don't weave <clinit> into a target (only extensions)
            }

            if (weaveMethod.name.equals(WeaveUtils.INIT_NAME)) {
                // validate ctor field assignment calls and remove redundant Weaver.callOriginal() invocations
                // must happen before we skip empty ctors below to support final field assignments w/out a ctor match
                // see ConstructorWeaveTest.testInitFinalFieldWithNoDefaultConstructor()
                processInitFieldAssignment(weaveMethod);
            }

            if (WeaveUtils.isEmptyConstructor(weaveMethod)) {
                continue; // skip empty constructors
            }

            if ((weaveMethod.access & Opcodes.ACC_BRIDGE) != 0) {
                continue; // skip autogenerated bridge methods
            }

            if (WeaveUtils.isSyntheticAccessor(weaveMethod.name)) {
                if ((weaveMethod.access & Opcodes.ACC_SYNTHETIC) != Opcodes.ACC_SYNTHETIC) {
                    addViolation(METHOD_SYNTHETIC_WEAVE_ILLEGAL, weaveMethod);
                    continue;
                }

                GeneratedNewFieldMethod generatedNewFieldMethod = GeneratedNewFieldMethod.isGeneratedNewFieldMethod(
                        weaveMethod, newFields);
                if (generatedNewFieldMethod != null) {
                    generatedNewFieldMethods.put(generatedNewFieldMethod.method, generatedNewFieldMethod);
                }
                continue; // generated methods are handled specially
            }

            if (WeaveUtils.hasWeaveIntoAllMethodsAnnotation(weaveMethod)) {
                processWeaveIntoAllMethods(weaveMethod, cache);
                continue;
            }

            MatchableMethod matchedMethod = matchableMethods.get(new MethodKey(weaveMethod));
            Method asmMethod = new Method(weaveMethod.name, weaveMethod.desc);
            if (matchedMethod == null) {
                newMethods.add(asmMethod);
            } else {
                validateMatchedMethod(matchedMethod, weaveMethod);
                matchedMethods.add(asmMethod);
            }
        }
        if (null != clinit) {
            // clinit matching needs to go last, which is why we didn't do it in the loop
            extensionClassInit = validateClassInit(clinit);
        }

        // interfaces never have a constructor
        // we allow the no-arg constructor to be written in the weave class so interface matches can be initialized
        // the instructions get added at the end of *every* constructor in the target
        // for the purposes of validation, we validate it as though it was a matched weave method
        // we do not call validateMatchedMethod because there is no original
        if (isInterfaceMatch && newMethods.contains(WeaveUtils.DEFAULT_CONSTRUCTOR)) {
            newMethods.remove(WeaveUtils.DEFAULT_CONSTRUCTOR);
            matchedMethods.add(WeaveUtils.DEFAULT_CONSTRUCTOR);
        }

        weavesAllConstructors = validateWeaveAllConstructors();

        boolean collectClassAnnotations = false;
        // validate weave methods *after* match - match info is needed, e.g. to ensure no new methods call each other
        for (MethodNode weaveMethod : weave.methods) {
            WeaveMethodInstructionScanner instructionInfo = new WeaveMethodInstructionScanner();
            weaveMethod.accept(instructionInfo);
            validateWeaveMethod(weaveMethod, instructionInfo);

            if (instructionInfo.isClassAnnotationGetter) {
                classAnnotationGetters.add(weaveMethod);
                collectClassAnnotations = true;
            }

            if (instructionInfo.isMethodAnnotationGetter) {
                if (weaveMethod.equals(weavesAllMethod)) {
                    for (MethodNode originalMethod : original.methods) {
                        storeMethodAnnotations(originalMethod);
                    }
                } else {
                    MethodNode originalMethod = WeaveUtils.getMethodNode(original, weaveMethod.name, weaveMethod.desc);
                    storeMethodAnnotations(originalMethod);
                }

                if (WeaveUtils.isWeaveWithAnnotationInterfaceMatch(weave)) {
                    // Add store method annotations for methods in interface
                    for (String interfaceName : original.interfaces) {
                        try {
                            ClassInformation interfaceInformation = cache.getClassInformation(interfaceName);

                            Set<MethodKey> weaveMethodKeys = new HashSet<>();
                            for (MethodNode method : weave.methods) {
                                weaveMethodKeys.add(new MethodKey(method));
                            }

                            for (MethodNode method : weaveAllMatchedMethods.keySet()) {
                                weaveMethodKeys.add(new MethodKey(method));
                            }

                            for (ClassInformation.MemberInformation method : interfaceInformation.methods) {
                                // If method is same as weave method,
                                // Pull out method annotations and add it to methodToAnnotations
                                MethodKey methodKey = new MethodKey(method.name, method.desc);
                                if (weaveMethodKeys.contains(methodKey)) {
                                    if (!methodsToAnnotations.containsKey(methodKey)) {
                                        methodsToAnnotations.put(methodKey, new HashMap<String, AnnotationNode>());
                                    }

                                    for (AnnotationNode annotation : method.annotations) {
                                        methodsToAnnotations.get(methodKey).put(Type.getType(annotation.desc).getClassName(), annotation);
                                    }
                                }
                            }

                        } catch (IOException ignored) {
                        }
                        // Need to get annotation nodes for each member in interfaceInformation.
                    }
                }
            }
        }

        if (collectClassAnnotations) {
            List<AnnotationNode> classAnnotations = WeaveUtils.getClassAnnotations(original);
            for (AnnotationNode classAnnotation : classAnnotations) {
                classAnnotationMap.put(Type.getType(classAnnotation.desc).getClassName(), classAnnotation);
            }

            if (WeaveUtils.isWeaveWithAnnotationInterfaceMatch(weave)) {
                // Collect interface annotations.
                for (String interfaceName : original.interfaces) {
                    try {
                        ClassInformation interfaceInformation = cache.getClassInformation(interfaceName);
                        for (AnnotationNode annotationNode : interfaceInformation.classAnnotationNodes) {
                            classAnnotationMap.put(Type.getType(annotationNode.desc).getClassName(), annotationNode);
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private void storeMethodAnnotations(MethodNode originalMethod) {
        MethodKey key = new MethodKey(originalMethod);
        if (methodsToAnnotations.get(key) == null) {
            methodsToAnnotations.put(key, new HashMap<String, AnnotationNode>());
        }

        for (AnnotationNode methodAnnotation : WeaveUtils.getMethodAnnotations(originalMethod)) {
            String annotationInternalName = Type.getType(methodAnnotation.desc).getClassName();
            methodsToAnnotations.get(key).put(annotationInternalName, methodAnnotation);
        }
    }

    private void processWeaveIntoAllMethods(MethodNode weaveMethod, ClassCache cache) {
        Set<String> methodRequiredAnnotations = WeaveUtils.getMethodRequiredAnnotations(WeaveUtils.getMethodAnnotations(weaveMethod));
        boolean requiresMethodAnnotationToMatch = !methodRequiredAnnotations.isEmpty();

        for (MethodNode originalMethod : original.methods) {
            if (WeaveUtils.isSyntheticAccessor(originalMethod.name) || WeaveUtils.isConstructor(originalMethod.name) ||
                    WeaveUtils.isStaticInitializer(originalMethod.name)) {
                continue;
            }

            if (WeaveUtils.isMethodWeNeverInstrument(originalMethod)) {
                continue;
            }

            if (requiresMethodAnnotationToMatch) {
                // Does the originalMethod in this class have the annotations required to match?
                boolean methodHasRequiredAnnotation = WeaveUtils.hasRequiredAnnotations(originalMethod, methodRequiredAnnotations);

                if (!methodHasRequiredAnnotation && !WeaveUtils.isWeaveWithAnnotationInterfaceMatch(weave)) {
                    // Don't have anywhere else to look for required annotations
                    continue;
                }

                // If not, can we find the required annotations in one of the direct interfaces
                if (!methodHasRequiredAnnotation && !requiredMethodAnnotationInInterface(originalMethod, methodRequiredAnnotations, cache)) {
                    continue;
                }
            }

            weaveAllMatchedMethods.put(originalMethod, weaveMethod);
        }
    }

    private boolean requiredMethodAnnotationInInterface(MethodNode originalMethod, Set<String> methodRequiredAnnotations, ClassCache cache) {
        try {
            for (String interfaceName : original.interfaces) {
                ClassInformation interfaceInformation = cache.getClassInformation(interfaceName);
                for (ClassInformation.MemberInformation method : interfaceInformation.methods) {
                    if (method.name.equals(originalMethod.name) && method.desc.equals(originalMethod.desc)) {
                        Set<AnnotationNode> annotations = method.annotations;
                        Set<String> annotationClasses = new HashSet<>();
                        for (AnnotationNode annotation : annotations) {
                            annotationClasses.add(Type.getType(annotation.desc).getClassName());
                        }

                        if (WeaveUtils.hasRequiredAnnotations(annotationClasses, methodRequiredAnnotations)) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }

        return false;
    }

    private void processRequiredClassAnnotations(ClassCache cache) {
        final List<AnnotationNode> annotationsInOriginal = WeaveUtils.getClassAnnotations(original);
        Set<String> classAnnotations = Sets.newHashSetWithExpectedSize(annotationsInOriginal.size());
        for (AnnotationNode annotationNode : annotationsInOriginal) {
            classAnnotations.add(Type.getType(annotationNode.desc).getClassName());
        }

        // Exact match
        if (isOneOfRequiredAnnotations(classAnnotations, requiredClassAnnotations)) {
            return;
        }

        if (WeaveUtils.isWeaveWithAnnotationInterfaceMatch(weave)) {
            try {
                for (String interfaceName : original.interfaces) {
                    ClassInformation interfaceInformation = cache.getClassInformation(interfaceName);
                    if (isOneOfRequiredAnnotations(interfaceInformation.classAnnotationNames, requiredClassAnnotations)) {
                        return;
                    }
                }
            } catch (IOException e) {
            }
        }

        addViolation(CLASS_MISSING_REQUIRED_ANNOTATIONS);
    }

    private void processRequiredMethodAnnotations(ClassCache cache) {
        final List<AnnotationNode> annotationsInOriginal = WeaveUtils.getMethodAnnotations(original);
        Set<String> methodAnnotations = Sets.newHashSetWithExpectedSize(annotationsInOriginal.size());
        for (AnnotationNode annotationNode : annotationsInOriginal) {
            methodAnnotations.add(Type.getType(annotationNode.desc).getClassName());
        }

        if (isOneOfRequiredAnnotations(methodAnnotations, requiredMethodAnnotations)) {
            return;
        }

        addViolation(METHOD_MISSING_REQUIRED_ANNOTATIONS);
    }

    /**
     * @param annotations annotations to check.
     * @return true if one of the annotations passed in is one of the class annotations required to match.
     */
    private boolean isOneOfRequiredAnnotations(Set<String> annotations, Set<String> requiredClassAnnotations) {
        for (String requiredAnnotation : requiredClassAnnotations) {
            if (annotations.contains(requiredAnnotation)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return true if there is a valid {@link WeaveAllConstructors}, false otherwise.
     */
    private boolean validateWeaveAllConstructors() {
        List<MethodNode> ctors = getConstructors();
        List<MethodNode> weaveAllConstructors = getWeaveAllConstructors(ctors);

        if (weaveAllConstructors.isEmpty()) {
            return false;
        }

        // There should only be one constructor with @WeaveAllConstructor
        if (weaveAllConstructors.size() > 1) {
            addViolation(INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED);
            return false;
        }

        // If we have a @WeaveAllConstructor, there should be no other constructors
        if (weaveAllConstructors.size() == 1 && ctors.size() > 1) {
            addViolation(INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED);
            return false;
        }

        // Only allow @WeaveAllConstructors in default constructor
        MethodNode weaveAllCtor = weaveAllConstructors.get(0);
        if (!WeaveUtils.DEFAULT_CONSTRUCTOR.getDescriptor().equals(weaveAllCtor.desc)) {
            addViolation(INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED);
            return false;
        }

        return true;
    }

    private List<MethodNode> getConstructors() {
        List<MethodNode> ctors = new ArrayList<>();
        for (MethodNode weaveMethod : weave.methods) {
            if (WeaveUtils.INIT_NAME.equals(weaveMethod.name)) {
                ctors.add(weaveMethod);
            }
        }
        return ctors;
    }

    private List<MethodNode> getWeaveAllConstructors(List<MethodNode> ctors) {
        List<MethodNode> weaveAllConstructors = new ArrayList<>();

        for (MethodNode ctor : ctors) {
            if (ctor.visibleAnnotations == null) {
                continue;
            }

            for (AnnotationNode annotation : ctor.visibleAnnotations) {
                if (annotation.desc.equals(Type.getType(WeaveAllConstructors.class).getDescriptor())) {
                    weaveAllConstructors.add(ctor);
                }
            }
        }

        return weaveAllConstructors;
    }

    private MethodNode findAndValidateWeaveIntoAllMethod() {
        MethodNode result = null;

        for (MethodNode method : weave.methods) {
            final List<AnnotationNode> visibleAnnotations = method.visibleAnnotations;

            if (visibleAnnotations == null) {
                continue;
            }

            final boolean methodHasVoidReturnNoParameters = method.desc.endsWith("()V");

            for (AnnotationNode annotationNode : visibleAnnotations) {
                if (WeaveUtils.WEAVE_ALL_METHODS_TYPE.getDescriptor().equals(annotationNode.desc)) {

                    if (!methodHasVoidReturnNoParameters) {
                        // @WeaveIntoAllMethods should have a void return type and no parameters
                        addViolation(NON_VOID_NO_PARAMETERS_WEAVE_ALL_METHODS);
                        return null;
                    }

                    if ((method.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
                        addViolation(NON_STATIC_WEAVE_INTO_ALL_METHODS);
                    }

                    if (weavesAllMethod != null) {
                        addViolation(MULTIPLE_WEAVE_ALL_METHODS);
                        return null;
                    }

                    result = method;
                }
            }
        }

        if (result != null) {
            weave.methods.remove(result);
            result = MethodProcessors.removeLineNumbers(result);
            result = MethodProcessors.removeJSRInstructions(result);
            /*
             * Let's remove the RETURN instruction inserted by the compiler. We verify that
             * com.newrelic.api.agent.weaver.WeaveIntoAllMethods methods have a void return type. See
             * com.newrelic.weave.violation.WeaveViolationType#NON_VOID_NO_PARAMETERS_WEAVE_ALL_METHODS
             */
            result = MethodProcessors.removeReturnInstructions(result);
            weave.methods.add(result);
        }
        return result;
    }

    /**
     * Remove class static initializer, constructors, and "values" method.
     */
    private void processEnumMethods() {
        MethodNode clinitMethod = WeaveUtils.getMethodNode(weave, WeaveUtils.CLASS_INIT_NAME,
                WeaveUtils.CLASS_INIT_METHOD.getDescriptor());
        weave.methods.remove(clinitMethod);

        MethodNode valuesMethod = WeaveUtils.getMethodNode(weave, "values", "()[L" + weave.name + ";");
        weave.methods.remove(valuesMethod);

        Set<MethodNode> constructorMethods = new HashSet<>();
        List<MethodNode> methods = weave.methods;
        for (MethodNode methodNode : methods) {
            if (methodNode.name.equals(WeaveUtils.INIT_NAME)) {
                constructorMethods.add(methodNode);
            }
        }
        weave.methods.removeAll(constructorMethods);
    }

    private void validateMatchedField(FieldNode originalField, FieldNode weaveField) {
        if (!Type.getType(originalField.desc).equals(Type.getType(weaveField.desc))) {
            addViolation(FIELD_TYPE_MISMATCH, weaveField);
        }

        if ((originalField.access & Opcodes.ACC_STATIC) != (weaveField.access & Opcodes.ACC_STATIC)) {
            addViolation(FIELD_STATIC_MISMATCH, weaveField);
        }

        if ((originalField.access & Opcodes.ACC_FINAL) != (weaveField.access & Opcodes.ACC_FINAL)) {
            addViolation(FIELD_FINAL_MISMATCH, weaveField);
        }

        if (isBaseMatch && (originalField.access & Opcodes.ACC_PRIVATE) != 0) {
            addViolation(FIELD_PRIVATE_BASE_CLASS_MATCH, weaveField);
        }

        // If we have a private weave field we don't need to worry about the field access, otherwise check that they match
        boolean isWeaveFieldPrivateAccess = (weaveField.access & Opcodes.ACC_PRIVATE) != 0;
        if (!isWeaveFieldPrivateAccess
                && ((originalField.access & Opcodes.ACC_PUBLIC) != (weaveField.access & Opcodes.ACC_PUBLIC)
                || (originalField.access & Opcodes.ACC_PROTECTED) != (weaveField.access & Opcodes.ACC_PROTECTED)
                || (originalField.access & Opcodes.ACC_PRIVATE) != (weaveField.access & Opcodes.ACC_PRIVATE))) {
            addViolation(FIELD_ACCESS_MISMATCH, weaveField);
        }
    }

    private void validateMatchedMethod(MatchableMethod matchedMethod, MethodNode weaveMethod) {
        MethodNode originalMethod = matchedMethod.methodNode;

        if (!Type.getReturnType(originalMethod.desc).equals(Type.getReturnType(weaveMethod.desc))) {
            addViolation(METHOD_RETURNTYPE_MISMATCH, weaveMethod);
        }

        if ((originalMethod.access & Opcodes.ACC_NATIVE) != 0 || (weaveMethod.access & Opcodes.ACC_NATIVE) != 0) {
            addViolation(METHOD_NATIVE_UNSUPPORTED, weaveMethod);
        }

        if ((originalMethod.access & Opcodes.ACC_STATIC) != (weaveMethod.access & Opcodes.ACC_STATIC)) {
            addViolation(METHOD_STATIC_MISMATCH, weaveMethod);
        }

        if ((originalMethod.access & Opcodes.ACC_PUBLIC) != (weaveMethod.access & Opcodes.ACC_PUBLIC)
                || (originalMethod.access & Opcodes.ACC_PROTECTED) != (weaveMethod.access & Opcodes.ACC_PROTECTED)
                || (originalMethod.access & Opcodes.ACC_PRIVATE) != (weaveMethod.access & Opcodes.ACC_PRIVATE)) {
            addViolation(METHOD_ACCESS_MISMATCH, weaveMethod);
        }

        Set<String> originalMethodExceptions = new HashSet<>();
        if (originalMethod.exceptions != null) {
            originalMethodExceptions.addAll(originalMethod.exceptions);
        }
        if (weaveMethod.exceptions != null) {
            // Special case for weave method which has only "throws Exception"
            if (!(weaveMethod.exceptions.size() == 1 && weaveMethod.exceptions.get(0).equals("java/lang/Exception"))) {
                // We only want to throw a violation if an exception is used in the throws clause that doesn't
                // exist in the original method. Not declaring any throws clause is completely acceptable.
                if (!originalMethodExceptions.containsAll(weaveMethod.exceptions)) {
                    addViolation(METHOD_THROWS_MISMATCH, weaveMethod);
                }
            }
        }

        // Validate WeaveWithAnnotation methods
        Set<String> requiredMethodAnnotations = WeaveUtils.getMethodRequiredAnnotations(weaveMethod.visibleAnnotations);
        if (!requiredMethodAnnotations.isEmpty()) {
            boolean foundRequiredAnnotation = false;
            List<AnnotationNode> methodAnnotations = WeaveUtils.getMethodAnnotations(originalMethod);
            for (AnnotationNode methodAnnotation : methodAnnotations) {
                if (requiredMethodAnnotations.contains(Type.getType(methodAnnotation.desc).getClassName())) {
                    foundRequiredAnnotation = true;
                    break;
                }
            }

            if (!foundRequiredAnnotation) {
                addViolation(METHOD_MISSING_REQUIRED_ANNOTATIONS, originalMethod);
            }
        }

        boolean isWeaveAbstract = (weaveMethod.access & Opcodes.ACC_ABSTRACT) != 0;
        if (!matchedMethod.isWeavable && !isWeaveAbstract) {
            WeaveViolationType type;
            switch (matchedMethod.source) {
                case INTERFACE:
                    type = METHOD_INDIRECT_INTERFACE_WEAVE;
                    break;
                case SUPERCLASS:
                    type = METHOD_BASE_CONCRETE_WEAVE;
                    break;
                default:
                    type = METHOD_EXACT_ABSTRACT_WEAVE;
                    break;
            }
            addViolation(type, weaveMethod);
        }
    }

    private void validateWeaveMethod(MethodNode weaveMethod, WeaveMethodInstructionScanner instructionInfo) {
        // weave nested classes must not call synthetic accessors
        if (instructionInfo.callsSyntheticAccessor) {
            // this is a nested class calling an access$ method
            addViolation(CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED, weaveMethod);
        }

        // new method validations
        final Method methodKey = new Method(weaveMethod.name, weaveMethod.desc);
        boolean isNewMethod = newMethods.contains(methodKey);
        if (isNewMethod && (!weaveMethod.name.equals(WeaveUtils.INIT_NAME) || !weavesAllConstructors)) {
            // ensure there aren't any new constructors
            if (weaveMethod.name.equals(WeaveUtils.INIT_NAME)) {
                addViolation(INIT_NEW_UNSUPPORTED, weaveMethod);
            }

            // new methods cannot call themselves or each other
            if (instructionInfo.callsNewMethod) {
                addViolation(METHOD_NEW_INVOKE_UNSUPPORTED, weaveMethod);
            }

            // new methods cannot invoke callOriginal
            if (instructionInfo.numCallOriginalInvocations > 0) {
                addViolation(METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED, weaveMethod);
            }

            // new methods cannot be declared abstract as they will not have a body to inline
            if ((weaveMethod.access & Opcodes.ACC_ABSTRACT) != 0) {
                addViolation(METHOD_NEW_ABSTRACT_UNSUPPORTED, weaveMethod);
            }

            // new methods must be declared private because they are inlined
            // (see JAVA-1402 for ideas on how we could support some non-private new methods if needed)
            if ((weaveMethod.access & Opcodes.ACC_PRIVATE) == 0) {
                addViolation(METHOD_NEW_NON_PRIVATE_UNSUPPORTED, weaveMethod);
            }
        }

        // validate callOriginal instructions and save result for later use
        if (!isNewMethod && !weaveMethod.name.equals(WeaveUtils.CLASS_INIT_NAME)
                && !weaveMethod.name.equals(WeaveUtils.INIT_NAME)) {

            // weave methods can invoke callOriginal at most once
            // clinit and init are excluded b/c matched final fields must be assigned Weaver.callOriginal()
            // this is handled in validateClassInit and processInitFieldAssignment respectively
            if (instructionInfo.numCallOriginalInvocations > 1) {
                addViolation(METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE, weaveMethod);
            }

            if (instructionInfo.numCallOriginalInvocations == 1) {
                CallOriginalReplacement replacement = CallOriginalReplacement.replace(weave.name, weaveMethod);
                if (!replacement.isSuccess()) {
                    addViolation(METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE, weaveMethod);
                } else {
                    originalReplacements.put(methodKey, replacement);
                }
            }
        }

        // constructor validations
        if (weaveMethod.name.equals(WeaveUtils.INIT_NAME)) {

            // only allow default/no-arg constructor when original is an INTERFACE
            if (isInterfaceMatch && weaveMethod.name.equals(WeaveUtils.INIT_NAME) && !weaveMethod.desc.equals("()V")) {
                addViolation(INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED, weaveMethod);
            }
        }
    }

    public Map<String, AnnotationNode> getClassAnnotationMap() {
        return classAnnotationMap;
    }

    public Map<MethodNode, MethodNode> getWeaveAllMatches() {
        return weaveAllMatchedMethods;
    }

    public Set<MethodNode> getClassAnnotationGetters() {
        return classAnnotationGetters;
    }

    public Map<MethodKey, Map<String, AnnotationNode>> getMethodAnnotationMap() {
        return methodsToAnnotations;
    }

    /**
     * This visits a weave method and collects information necessary to validate its instructions.
     */
    private class WeaveMethodInstructionScanner extends MethodVisitor {
        public int numCallOriginalInvocations = 0;
        public boolean callsNewMethod = false;
        public boolean callsSyntheticAccessor = false;
        public boolean isClassAnnotationGetter = false;
        public boolean isMethodAnnotationGetter = false;

        public WeaveMethodInstructionScanner() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            // count original invocations - new methods cannot call original, and weave methods can at most once
            if (WeaveUtils.isOriginalMethodInvocation(owner, name, desc)) {
                numCallOriginalInvocations++;
            }

            if (WeaveUtils.isClassAnnotationGetter(owner, name, desc)) {
                isClassAnnotationGetter = true;
            }

            if (WeaveUtils.isMethodAnnotationGetter(owner, name, desc)) {
                isMethodAnnotationGetter = true;
            }

            // check if a new method is invoked - we do not currently allow them to call each other
            Method method = new Method(name, desc);
            if (owner.equals(weave.name) && newMethods.contains(method)) {
                callsNewMethod = true;
            }

            // weave methods cannot call synthetic accessors (unless they were generated to access/mutate new fields)
            if (WeaveUtils.isSyntheticAccessor(name) && !generatedNewFieldMethods.containsKey(method)) {
                // this is a nested class calling an access$XXX method
                callsSyntheticAccessor = true;
            }
        }
    }

    /**
     * Validate a new inner or nested class node and add appropriate violations to this class match. Specifically, we
     * check for compiler-generated outer class access methods that occur when an inner class needs to access a
     * non-public outer class member. We generally disallow these because we cannot add these methods to the target
     * during weave time, however we allow accessing non-public new fields in new inner or nested classes because we can
     * replace those accessor methods with the appropriate public method calls on the extension class.
     *
     * @param newInnerClassNode new inner or nested class of the original class in this match
     */
    public void validateNewInnerClass(ClassNode newInnerClassNode) {
        NewInnerClassInfoCollector collector = new NewInnerClassInfoCollector();
        newInnerClassNode.accept(collector);
        if (collector.callsSyntheticAccessor) {
            violations.add(new WeaveViolation(WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED,
                    newInnerClassNode.name));
        }
    }

    private class NewInnerClassInfoCollector extends ClassVisitor {
        private boolean callsSyntheticAccessor = false;

        NewInnerClassInfoCollector() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

            return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);

                    if (WeaveUtils.isSyntheticAccessor(name)
                            && !generatedNewFieldMethods.containsKey(new Method(name, desc))) {
                        // this is a nested class calling an access$ method
                        callsSyntheticAccessor = true;
                    }
                }
            };
        }
    }

    /**
     * Field initialization instructions get embedded into constructors. This method makes sure those initializations
     * are legal (final fields are only assigned Weaver.callOriginal()) and removes all of these redundant
     * callOriginal() invocations.
     *
     * @param init weave constructor method
     */
    private void processInitFieldAssignment(MethodNode init) {
        CallOriginalInitState state = CallOriginalInitState.INIT;
        int size = init.instructions.size();
        List<AbstractInsnNode> toRemove = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            AbstractInsnNode insn = init.instructions.get(i);
            state = nextState(state, insn, Opcodes.PUTFIELD);
            if (state == null) {
                addViolation(INIT_ILLEGAL_CALL_ORIGINAL, init);
                return; // callOriginal() value was not assigned to a matched field
            } else if (state == CallOriginalInitState.INIT) {

                // check for assigning something else to a matched field
                if (insn.getType() == AbstractInsnNode.FIELD_INSN
                        && ((insn.getOpcode() == Opcodes.PUTSTATIC) || (insn.getOpcode() == Opcodes.PUTFIELD))) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;

                    // only check fields in this object that are matched
                    if (fieldInsn.owner.equals(weave.name) && !newFields.contains(fieldInsn.name)) {
                        FieldNode fieldNode = WeaveUtils.findRequiredMatch(weave.fields, fieldInsn.name);
                        boolean isFinal = (fieldNode.access & Opcodes.ACC_FINAL) == Opcodes.ACC_FINAL;

                        // don't allow matched fields to be overwritten
                        if (isFinal) {
                            addViolation(FIELD_FINAL_ASSIGNMENT, fieldNode);
                            return;
                        }
                    }
                }
            } else {
                if (state == CallOriginalInitState.INVOKESTATIC) {
                    // we expect an ALOAD 0 somewhere before the PUTFIELD call that we need to remove
                    // currently we see it just before INVOKESTATIC (hopefully all compilers put it there)
                    AbstractInsnNode beforeInvokeStaticInsn = init.instructions.get(i - 1);
                    if (beforeInvokeStaticInsn.getOpcode() == Opcodes.ALOAD) {
                        if (((VarInsnNode) beforeInvokeStaticInsn).var == 0) {
                            toRemove.add(beforeInvokeStaticInsn);
                        }
                    }
                }
                toRemove.add(insn);
            }
        }
        for (AbstractInsnNode node : toRemove) {
            init.instructions.remove(node);
        }
    }

    /**
     * This validates class init method instructions and extracts instructions necessary to initialize new static
     * members. A few notes:
     * <ol>
     * <li>Matched members must <b>always</b> use Weaver.callOriginal() as an assignment</li>
     * <li>New static members must <b>never</b> use Weaver.callOriginal() as an assignment</li>
     * <li>Since the original class may not have a clinit, we do static initialization in the extension class. This
     * means that only public members in the original class may be accessed.</li>
     * </ol>
     */
    private MethodNode validateClassInit(MethodNode weaveClassInit) {
        weaveClassInit = MethodProcessors.removeJSRInstructions(weaveClassInit);
        MethodNode extensionClinit = WeaveUtils.newMethodNode(weaveClassInit);

        CallOriginalInitState state = CallOriginalInitState.INIT;
        int size = weaveClassInit.instructions.size();
        int numViolationsBeforeClinitValidation = violations.size();
        for (int i = 0; i < size; i++) {
            AbstractInsnNode insn = weaveClassInit.instructions.get(i);
            state = nextState(state, insn, Opcodes.PUTSTATIC);
            if (state == null) {
                addViolation(INIT_ILLEGAL_CALL_ORIGINAL, weaveClassInit);
                break;
            } else if (state == CallOriginalInitState.INIT) {

                // access checks - only public matched members cannot be accessed from the extension class
                if (insn.getType() == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                    boolean matchedField = !newFields.contains(fieldInsn.name);

                    // don't allow matched fields to be overwritten
                    if (insn.getOpcode() == Opcodes.PUTSTATIC && matchedField) {
                        addViolation(CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED, weaveClassInit);
                    }

                    // only allow access of matched public fields
                    if (fieldInsn.owner.equals(weave.name) && matchedField) {
                        FieldNode fieldNode = WeaveUtils.findRequiredMatch(weave.fields, fieldInsn.name);
                        if ((fieldNode.access & Opcodes.ACC_PUBLIC) == 0) {
                            addViolation(CLINIT_FIELD_ACCESS_VIOLATION, fieldNode);
                        }
                    }

                } else if (insn.getType() == AbstractInsnNode.METHOD_INSN) {

                    // only allow access of matched public methods
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    Method methodKey = new Method(methodInsn.name, methodInsn.desc);
                    if (methodInsn.owner.equals(weave.name) && !newMethods.contains(methodKey)) {
                        MethodNode match = WeaveUtils.findMatch(weave.methods, methodKey);
                        if (match != null && (match.access & Opcodes.ACC_PUBLIC) == 0) {
                            addViolation(CLINIT_METHOD_ACCESS_VIOLATION, methodKey);
                        }
                    }
                }

                // pass instruction through to extension <clinit> method
                insn.accept(extensionClinit);
            }
        }
        return violations.size() > numViolationsBeforeClinitValidation ? null : extensionClinit;
    }

    private enum CallOriginalInitState {
        INIT, INVOKESTATIC, CHECKCAST, INVOKEVIRTUAL, PUT
    }

    private CallOriginalInitState nextState(CallOriginalInitState state, AbstractInsnNode insn, int putOpcode) {
        // state changes
        switch (state) {
            case INIT:
                if (insn.getType() == AbstractInsnNode.METHOD_INSN && insn.getOpcode() == Opcodes.INVOKESTATIC) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (WeaveUtils.isOriginalMethodInvocation(methodInsn.owner, methodInsn.name, methodInsn.desc)) {
                        return CallOriginalInitState.INVOKESTATIC;
                    }
                }
                return state;
            case INVOKESTATIC:
                if (insn.getOpcode() == Opcodes.CHECKCAST) {
                    return CallOriginalInitState.CHECKCAST;
                } else if (insn.getOpcode() == putOpcode) {
                    return checkPut(insn);
                }
                break;
            case CHECKCAST:
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    return CallOriginalInitState.INVOKEVIRTUAL;
                } else if (insn.getOpcode() == putOpcode) {
                    return checkPut(insn);
                }
                break;
            case INVOKEVIRTUAL:
                if (insn.getOpcode() == putOpcode) {
                    return checkPut(insn);
                }
                break;
            case PUT:
                return CallOriginalInitState.INIT;
        }

        // these instructions are legal but do not change the state (loops in the FSM)
        if (insn.getType() == AbstractInsnNode.LABEL || insn.getType() == AbstractInsnNode.LINE) {
            return state;
        }

        // invalid instruction sequence
        return null;
    }

    private CallOriginalInitState checkPut(AbstractInsnNode insn) {
        // using Weaver.callOriginal() for new fields is prohibited
        FieldInsnNode fieldInsn = (FieldInsnNode) insn;
        if (fieldInsn.owner.equals(weave.name) && matchedFields.contains(fieldInsn.name)) {
            return CallOriginalInitState.PUT;
        }
        return null;
    }

    public ClassNode getOriginal() {
        return original;
    }

    public ClassNode getWeave() {
        return weave;
    }

    public Collection<WeaveViolation> getViolations() {
        return violations;
    }

    public Set<String> getNewFields() {
        return newFields;
    }

    public Set<String> getMatchedFields() {
        return matchedFields;
    }

    public Set<Method> getNewMethods() {
        return newMethods;
    }

    public Set<Method> getMatchedMethods() {
        return matchedMethods;
    }

    Map<Method, GeneratedNewFieldMethod> getGeneratedNewFieldMethods() {
        return generatedNewFieldMethods;
    }

    public Set<String> getNewInnerClasses() {
        return newInnerClasses;
    }

    public Set<String> getMatchedInnerClasses() {
        return matchedInnerClasses;
    }

    public boolean isInterfaceMatch() {
        return isInterfaceMatch;
    }

    public boolean weavesAllConstructors() {
        return weavesAllConstructors;
    }

    public boolean weavesAllMethods() {
        return weavesAllMethod != null && !isInterfaceMatch();
    }

    public MethodNode getWeavesAllMethod() {
        return weavesAllMethod;
    }

    public boolean isBaseMatch() {
        return isBaseMatch;
    }

    public boolean isFatalWeaveViolation() {
        return fatalWeaveViolation;
    }

    Map<Method, CallOriginalReplacement> getOriginalReplacements() {
        return originalReplacements;
    }

    MethodNode getExtensionClassInit() {
        return extensionClassInit;
    }

    private void addViolation(WeaveViolationType type) {
        violations.add(new WeaveViolation(type, weave.name));
    }

    private void addViolation(WeaveViolationType type, FieldNode field) {
        violations.add(new WeaveViolation(type, weave.name, field.name));
    }

    private void addViolation(WeaveViolationType type, MethodNode methodNode) {
        addViolation(type, new Method(methodNode.name, methodNode.desc));
    }

    private void addViolation(WeaveViolationType type, Method method) {
        violations.add(new WeaveViolation(type, weave.name, method));
    }
}
