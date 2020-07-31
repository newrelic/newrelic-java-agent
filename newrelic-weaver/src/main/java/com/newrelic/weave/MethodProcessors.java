/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.AnnotationProxyTemplate;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.MethodRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Method processors.
 */
public class MethodProcessors {

    private static final AtomicLong uniqueIdGenerator = new AtomicLong(0);

    private MethodProcessors() {
    }

    /**
     * Inline all of the specified method calls in the subject method.
     *
     * @param inlineOwnerClassName owner class of the methods to inline
     * @param inlineMethods methods to inline
     * @param subjectOwnerClassName owner class of the subject method that will have calls inlined in
     * @param subjectMethod subject method to inline calls into
     * @return new subject method with all specified methods inlined
     */
    public static MethodNode inlineMethods(final String inlineOwnerClassName, final Iterable<MethodNode> inlineMethods,
            final String subjectOwnerClassName, final MethodNode subjectMethod) {

        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);

        subjectMethod.accept(getInlineMethodsVisitor(subjectOwnerClassName, subjectMethod.access, subjectMethod.name,
                subjectMethod.desc, result, inlineOwnerClassName, inlineMethods));

        return result;
    }

    /**
     * Get a visitor that inlines all of the specified method calls into a subject method.
     *
     * @param owner subject method owner
     * @param access subject method access
     * @param name subject method name
     * @param desc subject method desc
     * @param delegate delegate to collect results
     * @param inlineOwnerClassName owner of methods to inline
     * @param inlineMethods methods to inline
     * @return visitor that inlines all of the specified method calls into a subject method
     */
    public static MethodVisitor getInlineMethodsVisitor(String owner, int access, String name, String desc,
            MethodVisitor delegate, final String inlineOwnerClassName, final Iterable<MethodNode> inlineMethods) {

        return new MethodCallInlinerAdapter(owner, access, name, desc, delegate, false) {

            @Override
            protected InlinedMethod mustInline(String owner, String name, String desc) {
                if (owner.equals(inlineOwnerClassName)) {
                    for (MethodNode methodToInline : inlineMethods) {
                        if (methodToInline.name.equals(name) && desc.equals(methodToInline.desc)) {
                            return new InlinedMethod(methodToInline, WeaveUtils.NO_OP_REMAPPER);
                        }
                    }
                }
                return null;
            }
        };
    }

    /**
     * Update calls to the specified old owner's fields/methods to use the new owner.
     *
     * @param subjectMethod method contianing field/method calls that need to be updated
     * @param newOwnerClassName class name of the new owner
     * @param oldOwnerClassName class name of the old owner
     * @param methodsToUpdate method names/descs that should have their owner updated in the subject method
     * @param fieldsToUpdate field names that should have their owner updated in the subject method
     * @return new method with updated owners for the specified fields/methods
     */
    public static MethodNode updateOwner(MethodNode subjectMethod, final String newOwnerClassName,
            final String oldOwnerClassName, final Collection<Method> methodsToUpdate,
            final Collection<String> fieldsToUpdate) {

        if (newOwnerClassName.equals(oldOwnerClassName)) {
            return subjectMethod; // nothing to do
        }

        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);

        subjectMethod.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {
            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                if (owner.equals(oldOwnerClassName) && fieldsToUpdate.contains(name)) {
                    super.visitFieldInsn(opcode, newOwnerClassName, name, desc);
                } else {
                    super.visitFieldInsn(opcode, owner, name, desc);
                }
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (owner.equals(oldOwnerClassName) && methodsToUpdate.contains(new Method(name, desc))) {
                    super.visitMethodInsn(opcode, newOwnerClassName, name, desc, itf);
                } else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }

            @Override
            public void visitLdcInsn(Object cst) {
                if (cst instanceof Type) {
                    if (oldOwnerClassName.equals(((Type) cst).getInternalName())) {
                        super.visitLdcInsn(Type.getType("L" + newOwnerClassName + ";"));
                        return;
                    }
                }
                super.visitLdcInsn(cst);
            }

            @Override
            public void visitTypeInsn(int opcode, String type) {
                if (oldOwnerClassName.equals(type)) {
                    super.visitTypeInsn(opcode, newOwnerClassName);
                    return;
                }
                super.visitTypeInsn(opcode, type);
            }
        });

        return result;
    }

    /**
     * Update a method using the specified type map.
     *
     * @param subjectMethod method to update
     * @param typeMap mapping type name map
     * @return new method node with types updated
     */
    public static MethodNode updateTypes(MethodNode subjectMethod, final Map<String, String> typeMap) {
        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);
        MethodRemapper typeRemapper = new MethodRemapper(result, new SimpleRemapper(typeMap));
        subjectMethod.accept(typeRemapper);
        return result;
    }

    /**
     * Update inner class constructor calls in the weave method to use the original class name instead of the weave
     * name.
     *
     * @param weaveMethod weave method to update
     * @param weaveClassName weave class name
     * @param originalClassName original class name
     * @param innerClassNames set of new inner classes to update constructor arguments from weave to original class
     * @return method with new inner class constructor arguments updated
     */
    public static MethodNode updateConstructorArgsForInnerClass(MethodNode weaveMethod, final String weaveClassName,
            final String originalClassName, final Set<String> innerClassNames) {

        MethodNode result = WeaveUtils.newMethodNode(weaveMethod);
        final Type originalType = Type.getObjectType(originalClassName);

        // adjust argument of anonymous inner class ctor to use the original instead of weave class
        MethodVisitor anonInitArgRemapper = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (name.equals(WeaveUtils.INIT_NAME) && innerClassNames.contains(owner)) {
                    // rename init param from weave to original class
                    Type[] args = Type.getArgumentTypes(desc);
                    for (int i = 0; i < args.length; i++) {
                        int sort = args[i].getSort();
                        if (sort == Type.OBJECT && args[i].getInternalName().equals(weaveClassName)) {
                            args[i] = originalType;
                        }
                    }
                    desc = Type.getMethodDescriptor(Type.VOID_TYPE, args);
                }
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        };

        weaveMethod.accept(anonInitArgRemapper);
        return result;
    }

    /**
     * Removes legacy JSR/RET instructions by inlining subroutine calls. These instructions are deprecated and not
     * necessary, and their presence does not allow ASM to compute frames.
     *
     * @param subjectMethod method to remove JSR/RET instructions from
     * @return method without JSR/RET instructions that functions exactly the same as the original subject method
     */
    public static MethodNode removeJSRInstructions(MethodNode subjectMethod) {
        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);

        subjectMethod.accept(new JSRInlinerAdapter(result, subjectMethod.access, subjectMethod.name,
                subjectMethod.desc, subjectMethod.signature,
                subjectMethod.exceptions.toArray(new String[subjectMethod.exceptions.size()])));

        return result;
    }

    /**
     * Strips line number instructions from a method.
     *
     * @param subjectMethod method to remove line numbers from
     * @return method without line numbers
     */
    public static MethodNode removeLineNumbers(MethodNode subjectMethod) {
        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);

        subjectMethod.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {
            @Override
            public void visitLineNumber(int line, Label start) {
                // don't visit
            }
        });

        return result;
    }

    /**
     * Extracts all instructions in the constructor that occur <i>after</i> the first call to any init method. This is
     * used to extract constructor bodies from weave classes, and relies on the fact that weave classes may not extend
     * other objects or have convenience constructors that call other constructors.
     *
     * @param ctor constructor method
     * @return method containing instructions after the first init call
     */
    public static MethodNode extractConstructorInstructionsAfterInit(final MethodNode ctor) {
        MethodNode result = WeaveUtils.copy(ctor);

        while (result.instructions.size() > 0) {
            AbstractInsnNode insn = result.instructions.getFirst();
            result.instructions.remove(insn);
            if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                AbstractInsnNode nextInsn = result.instructions.getFirst();

                boolean isConstructor = WeaveUtils.INIT_NAME.equals(methodInsn.name);
                boolean nextInsnIsConstructor = nextInsn != null && nextInsn.getType() == AbstractInsnNode.METHOD_INSN
                        && WeaveUtils.INIT_NAME.equals(((MethodInsnNode) nextInsn).name);
                if (isConstructor && !nextInsnIsConstructor) {
                    // This instruction is a constructor and the next instruction is not a constructor so we've found
                    // the "real" <init> call in this weave method, so let's break to capture the correct instructions
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Adds "dummy" invocations to the weave constructor method at every RETURN in the specified target constructor.
     *
     * @param weaveCtor matched/processed weave ctor method
     * @param weaveClassName weave class name
     * @param originalClassName original class name
     * @param targetCtor target ctor method
     * @param isNonstaticInnerTarget whether or not the target class is a non-static inner class
     * @return copy of target constructor with calls to weave constructor added at every return
     * @see ClassWeave#weaveMethod(MethodNode, MethodNode)
     */
    public static MethodNode addWeaveConstructorInvocationsAtEveryReturn(final MethodNode weaveCtor,
            final String weaveClassName, final String originalClassName, MethodNode targetCtor,
            final boolean isNonstaticInnerTarget) {
        MethodNode composite = WeaveUtils.newMethodNode(targetCtor);
        {
            targetCtor.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, composite) {
                @Override
                public void visitInsn(int opcode) {

                    if (opcode == Opcodes.RETURN) {
                        this.visitVarInsn(Opcodes.ALOAD, 0);
                        int index = isNonstaticInnerTarget ? 2 : 1;
                        for (Type argType : Type.getArgumentTypes(weaveCtor.desc)) {
                            this.visitVarInsn(argType.getOpcode(Opcodes.ILOAD), index);
                            index += argType.getSize();
                        }
                        this.visitMethodInsn(Opcodes.INVOKEVIRTUAL, weaveClassName, weaveCtor.name, weaveCtor.desc,
                                false);
                    }
                    super.visitInsn(opcode);
                }
            });
        }
        return composite;
    }

    /**
     * Because we create classes that are weaved into interfaces, the invoke instructions in our bytecode can get a
     * little messed up. This goes through the given bytecode and converts INVOKEVIRTUAL instructions to INVOKEINTERFACE
     * if the target of the invocation is a weaved interface.
     *
     * @param cv delegate visitor
     * @return visitor that will update instructions to INVOKEINTERFACE is needed when visited
     */
    public static ClassVisitor fixInvocationInstructions(ClassVisitor cv, final Map<String, MatchType> weaveMatches) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (opcode == Opcodes.INVOKEVIRTUAL) {
                            MatchType matchType = weaveMatches.get(owner);
                            if (matchType == MatchType.Interface) {
                                itf = true;
                                opcode = Opcodes.INVOKEINTERFACE;
                            }
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        };
    }

    /**
     * Replace calls to {@link Weaver#getImplementationTitle()} with the given constant.
     *
     * @param cv delegate visitor
     * @param implementationTitle value to replace calls with
     * @return visitor that when visited will do the replacement
     */
    public static ClassVisitor replaceGetImplementationTitle(ClassVisitor cv, final String implementationTitle) {
        final Method getTitleMethod = new Method("getImplementationTitle", Type.getType(String.class), new Type[0]);
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (owner.equals(WeaveUtils.WEAVER_TYPE.getInternalName())
                                && name.equals(getTitleMethod.getName()) && desc.equals(getTitleMethod.getDescriptor())) {
                            super.visitLdcInsn(implementationTitle);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    }
                };
            }
        };
    }

    /**
     * Rewrite methods doing getfield or putfield on a weaver NewField to use the extension class instead.
     *
     * @param weaveClassName The name of the class we're rewriting.
     * @param methodsToRewrite A map of Method->MethodNode containing the methods we're going to rewrite.
     * @param newFieldNames new field names present on weaveClassName.
     * @param matchedFieldNames matched field names present on weaveClassName.
     * @param preparedExtensions The {@link PreparedExtension}s containg the newfield maps.
     * @param superPreparedMatches Ordered list of weaveClassName's {@link PreparedMatch} supertypes.
     */
    public static void rewriteNewFieldCalls(String weaveClassName, Map<Method, MethodNode> methodsToRewrite,
            Set<String> newFieldNames, Set<String> matchedFieldNames, List<PreparedExtension> preparedExtensions,
            List<PreparedMatch> superPreparedMatches) {
        MethodProcessors.rewriteSuperNewFieldOwners(weaveClassName, methodsToRewrite.values(),
                newFieldNames, matchedFieldNames, superPreparedMatches);
        for (MethodNode weaveMethod : methodsToRewrite.values()) {
            for (PreparedExtension extension : preparedExtensions) {
                extension.rewriteNewFieldCalls(weaveMethod);
            }
        }
    }

    /**
     * It's possible for weave classes to inherit NewFields from superclasses. This will scan a set of methods for
     * inherited NewFields and update the owner to be the class where the newfield originates. This allows us to later
     * rewrite those field ops via {@link #rewriteNewFieldCalls(String, Map, Set, Set, List, List)}.
     *
     * @param weaveClassName The name of the weave class.
     * @param methodsToRewrite Methods which will be scanned and updated.
     * @param newFieldNames new field names present on weaveClassName.
     * @param matchedFieldNames matched field names present on weaveClassName.
     * @param superPreparedMatches Ordered list of weaveClassName's {@link PreparedMatch} supertypes.
     */
    private static void rewriteSuperNewFieldOwners(String weaveClassName, Collection<MethodNode> methodsToRewrite,
            Set<String> newFieldNames, Set<String> matchedFieldNames, List<PreparedMatch> superPreparedMatches) {
        for (MethodNode methodNode : methodsToRewrite) {
            AbstractInsnNode current = methodNode.instructions.getFirst();
            while (null != current) {
                if (current.getType() == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) current;
                    if (weaveClassName.equals(fieldInsn.owner)) {
                        if ((!newFieldNames.contains(fieldInsn.name)) && (!matchedFieldNames.contains(fieldInsn.name))) {
                            // a field op on this class that is neither matched nor new. Must be an inherited
                            // newfield. We need to update the owner to class the newfield is on.
                            for (PreparedMatch superMatch : superPreparedMatches) {
                                if (superMatch.getNewFields().contains(fieldInsn.name)) {
                                    fieldInsn.owner = superMatch.getWeaveName();
                                    break;
                                }
                            }
                        }
                    }
                }
                current = current.getNext();
            }
        }
    }

    public static MethodNode removeReturnInstructions(MethodNode subjectMethod) {
        MethodNode result = WeaveUtils.newMethodNode(subjectMethod);

        subjectMethod.accept(new MethodVisitor(WeaveUtils.ASM_API_LEVEL, result) {
            @Override
            public void visitInsn(int opcode) {
                if (opcode == Opcodes.RETURN) {
                    return;
                }

                super.visitInsn(opcode);
            }
        });

        return result;
    }

    static MethodNode renameGetAnnotationCalls(MethodNode preparedMethod, final ClassNode originalClass,
            final Collection<AnnotationNode> classAnnotations, final Collection<AnnotationNode> methodAnnotations,
            final Map<String, ClassNode> annotationProxyClasses) {

        // Find the matching method node from the original class
        final MethodNode originalMethod = WeaveUtils.findMatch(originalClass.methods, preparedMethod.name, preparedMethod.desc);
        if (originalMethod != null) {
            // Make a copy of the weaved method so we can modify the code
            MethodNode result = WeaveUtils.newMethodNode(preparedMethod);

            preparedMethod.accept(new GeneratorAdapter(WeaveUtils.ASM_API_LEVEL, result, result.access, result.name, result.desc) {
                private Object previousLdc;

                @Override
                public void visitLdcInsn(Object cst) {
                    // Keep track of the last ldc command that we saw so we can know what annotation class we're looking
                    // for as soon as we run into one of the "get*Annotation(Class<?> annotationClass)" methods
                    previousLdc = cst;
                    super.visitLdcInsn(cst);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                    if (WeaveUtils.isClassAnnotationGetter(owner, name, desc)) {
                        for (AnnotationNode node : classAnnotations) {
                            // Try to find the annotation we're looking for
                            if (previousLdc != null && node.desc.equals(previousLdc.toString())) {
                                pop(); // pop annotation class off of stack
                                generateAndLoadAnnotationProxyOnStack(this, node, originalClass.name, annotationProxyClasses);
                                previousLdc = null;
                                return;
                            }
                        }

                        // If we got here it meant that we couldn't find a matching annotation. Put null on the stack
                        pop();
                        visitInsn(Opcodes.ACONST_NULL);
                    } else if (WeaveUtils.isMethodAnnotationGetter(owner, name, desc)) {
                        for (AnnotationNode node : methodAnnotations) {
                            // Try to find the annotation we're looking for
                            if (previousLdc != null && node.desc.equals(previousLdc.toString())) {
                                pop(); // pop annotation class off of stack
                                generateAndLoadAnnotationProxyOnStack(this, node, originalClass.name, annotationProxyClasses);
                                previousLdc = null;
                                return;
                            }
                        }

                        // If we got here it meant that we couldn't find a matching annotation. Put null on the stack
                        pop();
                        visitInsn(Opcodes.ACONST_NULL);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    previousLdc = null;
                }
            });

            return result;
        }

        // We didn't modify anything so we can just return the original preparedMethod
        return preparedMethod;
    }

    private static void loadOnStack(Object value, GeneratorAdapter adapter, String className, final Map<String, ClassNode> annotationProxyClasses) {
        if (value == null) {
            adapter.visitInsn(Opcodes.ACONST_NULL);
        }
        if (value instanceof String) {
            adapter.push((String) value);
        }
        if (value instanceof Boolean) {
            adapter.push((Boolean) value);
            adapter.box(Type.BOOLEAN_TYPE);
        } else if (value instanceof Integer) {
            adapter.push((Integer) value);
            adapter.box(Type.INT_TYPE);
        } else if (value instanceof Long) {
            adapter.push((Long) value);
            adapter.box(Type.LONG_TYPE);
        } else if (value instanceof Float) {
            adapter.push((Float) value);
            adapter.box(Type.FLOAT_TYPE);
        } else if (value instanceof Double) {
            adapter.push((Double) value);
            adapter.box(Type.DOUBLE_TYPE);
        } else if (value instanceof Byte) {
            adapter.push(((Byte) value).intValue());
            adapter.box(Type.BYTE_TYPE);
        } else if (value instanceof Character) {
            adapter.push((Character) value);
            adapter.box(Type.CHAR_TYPE);
        } else if (value instanceof Short) {
            adapter.push((Short) value);
            adapter.box(Type.SHORT_TYPE);
        } else if (value instanceof String[]) {
            String[] casted = (String[]) value;
            Type castType = Type.getType(casted[0]);
            adapter.getStatic(castType, casted[1], castType);
        } else if (value instanceof AnnotationNode) {
            generateAndLoadAnnotationProxyOnStack(adapter, (AnnotationNode) value, className, annotationProxyClasses);
        } else if (value instanceof ArrayList) {
            ArrayList<Object> values = (ArrayList<Object>) value;

            if (values.get(0) instanceof String) {
                adapter.push(values.size());
                adapter.visitTypeInsn(Opcodes.ANEWARRAY, Type.getType(String.class).getInternalName());

                for (int i = 0; i < values.size(); i++) {
                    adapter.visitInsn(Opcodes.DUP);
                    adapter.push(i);
                    adapter.visitLdcInsn(values.get(i));
                    adapter.visitInsn(Opcodes.AASTORE);
                }
            } else if (values.get(0) instanceof Integer) {
                adapter.push(values.size());
                adapter.newArray(Type.INT_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Integer) values.get(i));
                    adapter.arrayStore(Type.INT_TYPE);
                }
            } else if (values.get(0) instanceof Double) {
                adapter.push(values.size());
                adapter.newArray(Type.DOUBLE_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Double) values.get(i));
                    adapter.arrayStore(Type.DOUBLE_TYPE);
                }
            } else if (values.get(0) instanceof Short) {
                adapter.push(values.size());
                adapter.newArray(Type.SHORT_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Short) values.get(i));
                    adapter.arrayStore(Type.SHORT_TYPE);
                }
            } else if (values.get(0) instanceof Float) {
                adapter.push(values.size());
                adapter.newArray(Type.FLOAT_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Float) values.get(i));
                    adapter.arrayStore(Type.FLOAT_TYPE);
                }
            } else if (values.get(0) instanceof Byte) {
                adapter.push(values.size());
                adapter.newArray(Type.BYTE_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Byte) values.get(i));
                    adapter.arrayStore(Type.BYTE_TYPE);
                }
            } else if (values.get(0) instanceof Character) {
                adapter.push(values.size());
                adapter.newArray(Type.CHAR_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Character) values.get(i));
                    adapter.arrayStore(Type.CHAR_TYPE);
                }
            } else if (values.get(0) instanceof Boolean) {
                adapter.push(values.size());
                adapter.newArray(Type.BOOLEAN_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Boolean) values.get(i));
                    adapter.arrayStore(Type.BOOLEAN_TYPE);
                }
            } else if (values.get(0) instanceof Long) {
                adapter.push(values.size());
                adapter.newArray(Type.LONG_TYPE);
                for (int i = 0; i < values.size(); i++) {
                    adapter.dup();
                    adapter.push(i);
                    adapter.push((Long) values.get(i));
                    adapter.arrayStore(Type.LONG_TYPE);
                }
            } else if (values.get(0) instanceof AnnotationNode) {
                adapter.push(values.size());
                adapter.visitTypeInsn(Opcodes.ANEWARRAY, Type.getType(((AnnotationNode) values.get(0)).desc).getInternalName());

                for (int i = 0; i < values.size(); i++) {
                    adapter.visitInsn(Opcodes.DUP);
                    adapter.push(i);
                    generateAndLoadAnnotationProxyOnStack(adapter, (AnnotationNode) values.get(i), className, annotationProxyClasses);
                    adapter.visitInsn(Opcodes.AASTORE);
                }
            } else if (values.get(0) instanceof String[]) {
                adapter.push(values.size());
                adapter.visitTypeInsn(Opcodes.ANEWARRAY, Type.getType(((String[]) values.get(0))[0]).getInternalName());

                for (int i = 0; i < values.size(); i++) {
                    adapter.visitInsn(Opcodes.DUP);
                    adapter.push(i);

                    String[] valueArray = (String[]) values.get(i);
                    adapter.getStatic(Type.getObjectType(Type.getType(valueArray[0]).getInternalName()), valueArray[1], Type.getType(valueArray[0]));
                    adapter.visitInsn(Opcodes.AASTORE);
                }
            }
        } else if (value instanceof int[]) {
            int[] nums = (int[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.INT_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.INT_TYPE);
            }
        } else if (value instanceof double[]) {
            double[] nums = (double[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.DOUBLE_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.DOUBLE_TYPE);
            }
        } else if (value instanceof float[]) {
            float[] nums = (float[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.FLOAT_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.FLOAT_TYPE);
            }
        } else if (value instanceof short[]) {
            short[] nums = (short[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.SHORT_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.SHORT_TYPE);
            }
        } else if (value instanceof char[]) {
            char[] nums = (char[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.CHAR_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.CHAR_TYPE);
            }
        } else if (value instanceof byte[]) {
            byte[] nums = (byte[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.BYTE_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.BYTE_TYPE);
            }
        } else if (value instanceof long[]) {
            long[] nums = (long[]) value;
            adapter.push(nums.length);
            adapter.newArray(Type.LONG_TYPE);
            for (int i = 0; i < nums.length; i++) {
                adapter.dup();
                adapter.push(i);
                adapter.push(nums[i]);
                adapter.arrayStore(Type.LONG_TYPE);
            }
        }
    }

    private static void generateAndLoadAnnotationProxyOnStack(GeneratorAdapter generatorAdapter, AnnotationNode node, String className,
            final Map<String, ClassNode> annotationProxyClasses) {
        // We need to generate a proxy class that represents this annotation. This will define a new class
        // and wire it up at this point in the code
        String annotationProxyClassName = className + "$AnnotationHolder_" + uniqueIdGenerator.incrementAndGet();
        ClassNode annotationProxyTemplate = getAnnotationProxyTemplate();
        if (annotationProxyTemplate == null) {
            return;
        }

        annotationProxyTemplate = WeaveUtils.copyAndRename(annotationProxyTemplate,
                "com/newrelic/weave/weavepackage/AnnotationProxyTemplate", annotationProxyClassName);
        annotationProxyClasses.put(annotationProxyClassName, annotationProxyTemplate);

        generatorAdapter.visitLdcInsn(Type.getType("L" + className + ";"));
        generatorAdapter.visitLdcInsn(Type.getType(node.desc)); // the annotation class

        int annotationValuesSize = 0;
        if (node.values != null) {
            annotationValuesSize = node.values.size();
        }

        generatorAdapter.push(annotationValuesSize);
        generatorAdapter.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");

        for (int i = 0; i < annotationValuesSize; i++) {
            generatorAdapter.visitInsn(Opcodes.DUP);

            generatorAdapter.push(i);
            loadOnStack(node.values.get(i), generatorAdapter, className, annotationProxyClasses);
            generatorAdapter.visitInsn(Opcodes.AASTORE);
        }

        generatorAdapter.visitMethodInsn(Opcodes.INVOKESTATIC, annotationProxyClassName, "getOrCreateAnnotationHolder",
                "(Ljava/lang/Class;Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/annotation/Annotation;", false);
    }

    private static ClassNode getAnnotationProxyTemplate() {
        try {
            return WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                    AnnotationProxyTemplate.class.getName(),
                    AnnotationProxyTemplate.class.getClassLoader()));
        } catch (Throwable t) {
            return null;
        }
    }
}
