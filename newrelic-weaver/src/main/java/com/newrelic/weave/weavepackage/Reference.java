/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassInformation;
import com.newrelic.weave.utils.ClassInformation.MemberInformation;
import com.newrelic.weave.utils.SynchronizedFieldNode;
import com.newrelic.weave.utils.SynchronizedMethodNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.ReferenceViolation;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Weaved code often references non-weaved original code. For example, an instrumentation author may use an original
 * utility class but not weave it. We need to check those references at runtime to ensure that the weaved code will
 * invoke a class that actually exists.
 * <p/>
 * This class represents a single reference to an original class. It also contains methods for verifying against
 * original class nodes.
 */
public class Reference {
    /**
     * the weave or util class that created this reference
     */
    public final String referenceOrigin;

    /**
     * asm flags we must see on the class accessor
     */
    private int[] requiredAccess;

    /**
     * asm flags which cannot be on the class accessor
     */
    private int[] illegalAccess;

    /**
     * name of the class
     */
    public final String className;

    private final Map<Method, MemberReference> methods = new HashMap<>();
    private final Map<String, MemberReference> fields = new HashMap<>();

    /**
     * For testing.
     */
    public Map<Method, MemberReference> getMethods() {
        return methods;
    }

    /**
     * Return a set of references created from everything a ClassNode references.<br/>
     * This includes
     * <ul>
     * <li>Extended Supertypes</li>
     * <li>Implemented Interfaces</li>
     * <li>Class Fields</li>
     * <li>Class Methods (Including constructors and static initializers)</li>
     * </ul>
     *
     * @param classNode The class node to be scanned.
     * @return A set of all References made by classNode
     */
    public static Set<Reference> create(ClassNode classNode) {
        Set<Reference> references = new HashSet<>();
        final String referenceSource = classNode.name;

        // add supertype + interfaces on classNode
        if (null != classNode.superName) {
            references.add(createFromSupertype(referenceSource, classNode.superName));
        }
        if (null != classNode.interfaces) {
            for (String interfaceName : classNode.interfaces) {
                references.add(createFromInterface(referenceSource, interfaceName));
            }
        }
        // add references made in classNode's fields
        if (null != classNode.fields) {
            for (FieldNode fieldNode : classNode.fields) {
                references.addAll(createFromField(referenceSource, classNode.name, fieldNode));
            }
        }
        // add references made in classNode's method
        if (null != classNode.methods) {
            for (MethodNode methodNode : classNode.methods) {
                references.addAll(createFromMethod(referenceSource, classNode.name, methodNode));
            }
        }
        return references;
    }

    private static Reference createFromSupertype(String referenceOrigin, String superName) {
        int[] classIllegalAccess = new int[] { Opcodes.ACC_INTERFACE, Opcodes.ACC_FINAL };
        return new Reference(referenceOrigin, superName, new int[] {}, classIllegalAccess);
    }

    private static Reference createFromInterface(String referenceOrigin, String interfaceName) {
        int[] classRequiredAccess = new int[] { Opcodes.ACC_INTERFACE };
        return new Reference(referenceOrigin, interfaceName, classRequiredAccess, new int[] {});
    }

    /**
     * Return a set of references created from everything a FieldNode references.
     */
    private static Set<Reference> createFromField(String referenceOrigin, String fieldNodeClassOwner, FieldNode fieldNode) {
        Set<Reference> references = new HashSet<>();

        Type fieldType = getReferencedType(fieldNode.desc);
        if (fieldType.getSort() == Type.OBJECT) {
            // don't care if the class references itself
            if (!Type.getType(fieldNode.desc).getInternalName().equals(fieldNodeClassOwner)) {
                references.add(new Reference(referenceOrigin, getReferencedType(fieldNode.desc).getInternalName(),
                        new int[] {}, new int[] {}));
            }
        }
        return references;
    }

    /**
     * Return a set of references created from everything a MethodNode references.
     */
    private static Set<Reference> createFromMethod(String referenceOrigin, String methodNodeClassOwner, MethodNode methodNode) {
        Set<Reference> references = new HashSet<>();
        Type returnType = getReferencedType(Type.getReturnType(methodNode.desc).getDescriptor());
        if (returnType.getSort() == Type.OBJECT) {
            references.add(new Reference(referenceOrigin, returnType.getInternalName(), new int[] {}, new int[] {}));
        }

        // method locals + args
        if (null != methodNode.localVariables) {
            for (LocalVariableNode local : methodNode.localVariables) {
                if (local.name.equals("this")) {
                    continue;
                }
                Type localType = getReferencedType(local.desc);
                if (localType.getSort() == Type.OBJECT) {
                    references.add(new Reference(referenceOrigin, localType.getInternalName(), new int[] {},
                            new int[] {}));
                }
            }
        }
        if ((Opcodes.ACC_ABSTRACT & methodNode.access) == 0) {
            // this method is not abstract. Add references for every method + field invoke
            AbstractInsnNode current = methodNode.instructions.getFirst();
            while (current != null) {
                if (current.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsn = (MethodInsnNode) current;
                    Type ownerType = getReferencedType(Type.getObjectType(methodInsn.owner).getDescriptor());
                    if (isPrimitiveType(ownerType)) {
                        current = current.getNext();
                        continue;
                    }

                    // don't care if the class references itself
                    if (!ownerType.getInternalName().equals(methodNodeClassOwner)) {
                        int[] classRequiredAccess;
                        int[] classIllegalAccess;
                        if (methodInsn.itf) {
                            classRequiredAccess = new int[] { Opcodes.ACC_INTERFACE };
                            classIllegalAccess = new int[] {};
                        } else {
                            classRequiredAccess = new int[] {};
                            classIllegalAccess = new int[] { Opcodes.ACC_INTERFACE };
                        }
                        Reference reference = new Reference(referenceOrigin, ownerType.getInternalName(),
                                classRequiredAccess, classIllegalAccess);

                        int[] methodIllegalAccess = new int[] { Opcodes.ACC_PRIVATE };
                        int[] methodRequiredAccess;
                        if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC) {
                            methodRequiredAccess = new int[] { Opcodes.ACC_STATIC };
                        } else {
                            methodRequiredAccess = new int[] {};
                        }
                        reference.addOrMergeMethodReference(methodInsn.name, methodInsn.desc, methodRequiredAccess,
                                methodIllegalAccess);

                        references.add(reference);
                    }
                } else if (current.getType() == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) current;
                    // don't care if the class references itself
                    Type ownerType = getReferencedType(Type.getObjectType(fieldInsn.owner).getDescriptor());
                    if (isPrimitiveType(ownerType)) {
                        current = current.getNext();
                        continue;
                    }
                    if (!ownerType.getInternalName().equals(methodNodeClassOwner)) {
                        Reference reference = new Reference(referenceOrigin, ownerType.getInternalName(), new int[] {},
                                new int[] {});

                        int[] fieldIllegalAccess = new int[] { Opcodes.ACC_PRIVATE };
                        int[] fieldRequiredAccess;
                        if (fieldInsn.getOpcode() == Opcodes.INVOKESTATIC) {
                            fieldRequiredAccess = new int[] { Opcodes.ACC_STATIC };
                        } else {
                            fieldRequiredAccess = new int[] {};
                        }
                        reference.addOrMergeFieldReference(fieldInsn.name, fieldInsn.desc, fieldRequiredAccess,
                                fieldIllegalAccess);

                        references.add(reference);
                    }
                } else if (current.getType() == AbstractInsnNode.TYPE_INSN) {
                    // cast instructions
                    TypeInsnNode typeInsn = (TypeInsnNode) current;
                    Type castType = getReferencedType(Type.getObjectType(typeInsn.desc).getDescriptor());
                    if (castType.getSort() == Type.OBJECT) {
                        references.add(new Reference(referenceOrigin, castType.getInternalName(), new int[0],
                                new int[0]));
                    }
                } else if (current.getType() == AbstractInsnNode.MULTIANEWARRAY_INSN) {
                    MultiANewArrayInsnNode arrayNode = (MultiANewArrayInsnNode) current;
                    Type arrayType = getReferencedType(arrayNode.desc);
                    if (arrayType.getSort() == Type.OBJECT) {
                        references.add(new Reference(referenceOrigin, arrayType.getInternalName(), new int[0],
                                new int[0]));
                    }
                }
                current = current.getNext();
            }
        }

        return references;
    }

    /**
     * Returns desc's type. If desc is an array it will return the type of the array.
     */
    private static Type getReferencedType(String desc) {
        Type type = Type.getType(desc);
        if (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        return type;
    }

    private static boolean isPrimitiveType(Type type) {
        return type.getSort() < Type.ARRAY;
    }

    public Reference(String referenceOrigin, String className, int[] requiredAccess, int[] illegalAccess) {
        this.referenceOrigin = referenceOrigin;
        this.className = className;
        this.requiredAccess = requiredAccess;
        this.illegalAccess = illegalAccess;
    }

    /**
     * Add a new referenced method or update an existing one. Returns true if the method was successfully added.
     */
    private boolean addOrMergeMethodReference(String name, String desc, int[] requiredAccess, int[] illegalAccess) {
        Method method = new Method(name, desc);
        if (methods.containsKey(method)) {
            return methods.get(method).merge(new MemberReference(name, desc, requiredAccess, illegalAccess));
        } else {
            methods.put(method, new MemberReference(name, desc, requiredAccess, illegalAccess));
            return true;
        }
    }

    /**
     * Add a new referenced field or update an existing one. Returns true if the field was successfully added.
     */
    private boolean addOrMergeFieldReference(String name, String desc, int[] requiredAccess, int[] illegalAccess) {
        if (fields.containsKey(name)) {
            return fields.get(name).merge(new MemberReference(name, desc, requiredAccess, illegalAccess));
        } else {
            fields.put(name, new MemberReference(name, desc, requiredAccess, illegalAccess));
            return true;
        }
    }

    /**
     * Merge another reference into this one.
     *
     * Returns indicates the success of the merge. False results indicate there was a piece of contradictory information
     * between the two references (e.g. trying to merge a reference that has a private field into a reference which has
     * a public version of that field).
     *
     * @param otherReference reference to merge
     * @return the result of the merge.
     */
    public boolean merge(Reference otherReference) {
        boolean merged = this.className.equals(otherReference.className);

        requiredAccess = mergeFlags(requiredAccess, otherReference.requiredAccess);
        illegalAccess = mergeFlags(illegalAccess, otherReference.illegalAccess);
        if (accessFlagsOverlap(requiredAccess, illegalAccess)) {
            merged = false;
        }

        for (MemberReference otherField : otherReference.fields.values()) {
            if (fields.containsKey(otherField.name)) {
                merged = merged && fields.get(otherField.name).merge(otherField);
            } else {
                fields.put(otherField.name, otherField);
            }
        }

        for (MemberReference otherMethodReference : otherReference.methods.values()) {
            Method otherMethod = new Method(otherMethodReference.name, otherMethodReference.desc);
            if (methods.containsKey(otherMethod)) {
                merged = merged && methods.get(otherMethod).merge(otherMethodReference);
            } else {
                methods.put(otherMethod, otherMethodReference);
            }
        }
        return merged;
    }

    /**
     * Validate this reference against a class node and return a list of violations indicating what does not match.
     *
     * @param classCache the classloader used to load the original class.
     * @param classNode the original class to match against.
     * @return list of {@link ReferenceViolation}s
     * @throws IOException
     */
    List<WeaveViolation> validateClassNode(ClassCache classCache, ClassNode classNode) throws IOException {
        List<WeaveViolation> violations = new ArrayList<>();
        if (!this.className.equals(classNode.name)) {
            violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                    classNode.name, "class name mismatch"));
        }

        // first check the class accessors
        if (this.requiredAccess.length > 0) {
            int total = 0;
            for (int requiredAcces : requiredAccess) {
                total += requiredAcces;
            }
            if (!WeaveUtils.flagsMatch(total, classNode.access, requiredAccess)) {
                violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                        classNode.name, "required class access mismatch. weave expects: "
                        + WeaveUtils.humanReadableAccessFlags(total) + " original has: "
                        + WeaveUtils.humanReadableAccessFlags(classNode.access)));
            }
        }
        if (this.illegalAccess.length > 0) {
            if (!WeaveUtils.flagsMatch(0, classNode.access, illegalAccess)) {
                violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                        classNode.name, "illegal class access mismatch. Original has: "
                        + WeaveUtils.humanReadableAccessFlags(classNode.access)));
            }
        }

        List<MethodNode> originalMethods = new ArrayList<>();
        List<FieldNode> originalFields = new ArrayList<>();
        addMethodsAndFields(classCache, classNode, originalMethods, originalFields);

        // now check the fields and methods
        violations.addAll(validateFieldsAndMethods(classNode.name, originalMethods, originalFields));
        return violations;
    }

    private List<WeaveViolation> validateFieldsAndMethods(String originalName, List<MethodNode> originalMethods,
            List<FieldNode> originalFields) {
        List<WeaveViolation> violations = new ArrayList<>();
        for (MemberReference method : methods.values()) {
            MethodNode methodNode = WeaveUtils.findMatch(originalMethods, method.name, method.desc);
            if (null == methodNode) {
                violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                        originalName, "missing method. weave code expects method: " + method.name + method.desc));
            } else {
                if (!method.matches(methodNode.name, methodNode.desc, methodNode.access)) {
                    violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                            originalName, "weave and origin method method mismatch. weave code expects method: "
                            + method.name + method.desc + WeaveUtils.humanReadableAccessFlags(
                            method.requiredAccess) + "original has " + methodNode.name + methodNode.desc
                            + WeaveUtils.humanReadableAccessFlags(methodNode.access)));
                }
            }
        }
        for (MemberReference field : fields.values()) {
            FieldNode fieldNode = WeaveUtils.findMatch(originalFields, field.name);
            if (null == fieldNode) {
                violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                        originalName, "missing field. weave code expects field: " + field.name + field.desc
                        + WeaveUtils.humanReadableAccessFlags(field.requiredAccess)));
            } else {
                if (!field.matches(fieldNode.name, fieldNode.desc, fieldNode.access)) {
                    violations.add(new ReferenceViolation(WeaveViolationType.INVALID_REFERENCE, referenceOrigin,
                            originalName, "missing field. weave code expects field: " + field.name + field.desc
                            + WeaveUtils.humanReadableAccessFlags(field.requiredAccess) + " original has: "
                            + fieldNode.name + fieldNode.desc + WeaveUtils.humanReadableAccessFlags(
                            fieldNode.access)));
                }
            }
        }
        return violations;
    }

    /**
     * A reference to a field or method.
     */
    private class MemberReference {
        public final String name;
        public final String desc;
        int[] requiredAccess;
        int[] illegalAccess;

        private MemberReference(String name, String desc, int[] requiredAccess, int[] illegalAccess) {
            this.name = name;
            this.desc = desc;
            this.requiredAccess = requiredAccess;
            this.illegalAccess = illegalAccess;
        }

        public boolean merge(MemberReference otherMember) {
            boolean merged = name.equals(otherMember.name);
            requiredAccess = mergeFlags(requiredAccess, otherMember.requiredAccess);
            illegalAccess = mergeFlags(illegalAccess, otherMember.illegalAccess);
            if (accessFlagsOverlap(requiredAccess, illegalAccess)) {
                merged = false;
            }
            merged = merged && requiredAccess == otherMember.requiredAccess;
            merged = merged && desc.equals(otherMember.desc);
            return merged;
        }

        public boolean matches(String name, String desc, int access) {
            boolean match = this.name.equals(name) && this.desc.equals(desc);
            if (this.requiredAccess.length > 0) {
                int total = 0;
                for (int requiredAcces : requiredAccess) {
                    total += requiredAcces;
                }
                match = match && WeaveUtils.flagsMatch(total, access, requiredAccess);
            }
            if (this.illegalAccess.length > 0) {
                match = match && WeaveUtils.flagsMatch(0, access, illegalAccess);
            }
            return match;
        }
    }

    /**
     * Merge two sets of access flags, ignoring duplicates.
     */
    private static int[] mergeFlags(int[] access1, int[] access2) {
        List<Integer> merged = new ArrayList<>(access1.length + access2.length);
        for (int anAccess1 : access1) {
            merged.add(anAccess1);
        }
        for (int anAccess2 : access2) {
            if (!merged.contains(anAccess2)) {
                merged.add(anAccess2);
            }
        }

        int[] mergedArray = new int[merged.size()];
        for (int i = 0; i < mergedArray.length; ++i) {
            mergedArray[i] = merged.get(i);
        }
        return mergedArray;
    }

    /**
     * Returns true if there is one access flag present in both sets of access flags.
     */
    private static boolean accessFlagsOverlap(int[] access1, int[] access2) {
        int total1 = 0;
        for (int anAccess1 : access1) {
            total1 += anAccess1;
        }
        int total2 = 0;
        for (int anAccess2 : access2) {
            total2 += anAccess2;
        }
        return (total1 & total2) != 0;
    }

    /**
     * Fetch all methods and fields of a class node including those inherited from supertypes+interfaces. The results
     * will be stored in the passed in methods and fields lists.
     *
     * @param classCache Cache to use to fetch supertype resources
     * @param classNode The node to scan
     * @param methods classNode's methods will be appended to this list.
     * @param fields classNode's fields will be appended to this list.
     */
    private static void addMethodsAndFields(ClassCache classCache, ClassNode classNode, List<MethodNode> methods,
            List<FieldNode> fields) throws IOException {
        ClassInformation classInfo = classCache.getClassInformation(classNode.name);
        for (MemberInformation methodInfo : classInfo.getAllMethods(classCache)) {
            MethodNode methodNode = new SynchronizedMethodNode();
            methodNode.name = methodInfo.name;
            methodNode.desc = methodInfo.desc;
            methodNode.access = methodInfo.access;
            methods.add(methodNode);
        }
        for (MemberInformation fieldInfo : classInfo.getAllFields(classCache)) {
            fields.add(new SynchronizedFieldNode(WeaveUtils.ASM_API_LEVEL, fieldInfo.access, fieldInfo.name, fieldInfo.desc, null, null));
        }
    }

}