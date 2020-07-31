/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Attempts to replace Weaver.callOriginal() instructions w/ instructions to invoke the original method, including
 * whether the replacement was successful, LabelNodes representing the bounds of the replacement, and the processed
 * resulting MethodNode.
 *
 * @see CallOriginalReplacement#replace(String, MethodNode)
 */
class CallOriginalReplacement {

    private final String weaveClassName;
    private final MethodNode weaveMethod;
    private MethodNode result;
    private boolean success;

    private LabelNode startOfCallOriginal = null;
    private LabelNode endOfCallOriginal = null;

    private CallOriginalReplacement(String weaveClassName, MethodNode weaveMethod) {
        this.weaveClassName = WeaveUtils.INLINER_PREFIX + weaveClassName;
        this.weaveMethod = weaveMethod;
    }

    /**
     * Replaces Weaver.callOriginal() instructions w/ instructions to invoke the original method.
     *
     * @param weaveClassName weave class name
     * @param weaveMethod weave method
     * @return info about the replacement, incl. whether the replacement was successful & the processed method node
     */
    public static CallOriginalReplacement replace(String weaveClassName, MethodNode weaveMethod) {
        CallOriginalReplacement result = new CallOriginalReplacement(weaveClassName, weaveMethod);
        result.replace();
        return result;
    }

    private void replace() {
        MethodNode result = WeaveUtils.copy(weaveMethod);
        MethodInsnNode originalInsn = findOriginalIndex(result.instructions);
        if (originalInsn == null) {
            this.success = true;
            this.result = result;
            return; // use weave implementation
        }
        startOfCallOriginal = WeaveUtils.makeLabelNode();
        endOfCallOriginal = WeaveUtils.makeLabelNode();

        result.instructions.insertBefore(originalInsn, startOfCallOriginal);

        // insert insns to invoke the original method
        boolean isStatic = (weaveMethod.access & Opcodes.ACC_STATIC) != 0;

        // push args on operand stack
        int index = 0;
        if (!isStatic) {
            result.instructions.insertBefore(originalInsn, new VarInsnNode(Opcodes.ALOAD, index++));
        }

        for (Type argType : Type.getArgumentTypes(result.desc)) {
            result.instructions.insertBefore(originalInsn, new VarInsnNode(argType.getOpcode(Opcodes.ILOAD), index));
            index += argType.getSize();
        }

        // invoke method
        int invokeOpcode = isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL;
        result.instructions.insertBefore(originalInsn, new MethodInsnNode(invokeOpcode, weaveClassName,
                result.name, result.desc, false));

        // add/remove instructions following callOriginal() based on the return type of the original method and how
        // it is used in the weave method
        Type returnType = Type.getReturnType(weaveMethod.desc);
        AbstractInsnNode nextInsn = originalInsn.getNext();
        switch (returnType.getSort()) {
            case Type.VOID:
                if (nextInsn.getOpcode() != Opcodes.POP) {
                    return;
                }
                result.instructions.remove(nextInsn);
                result.instructions.insert(originalInsn, endOfCallOriginal);
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
            case Type.FLOAT:
            case Type.LONG:
            case Type.DOUBLE:
                if (nextInsn.getOpcode() == Opcodes.POP) {
                    // the weaved method calls the original method and pops the return value. Let's make sure the
                    // correct pop instruction is used for the primitive value
                    if (returnType.getSize() == 2) {
                        result.instructions.remove(nextInsn);
                        result.instructions.insert(originalInsn, new InsnNode(Opcodes.POP2));
                    }
                    result.instructions.insert(originalInsn, endOfCallOriginal);
                    break;
                }

                // the weaved class uses the return value
                boolean unboxesReturnValue = false;
                Type boxedType = PRIMITIVE_TO_OBJECT_TYPE.get(returnType);
                if (nextInsn.getOpcode() == Opcodes.CHECKCAST) {
                    // make sure the cast is to the proper boxed type
                    TypeInsnNode castInsn = (TypeInsnNode) nextInsn;
                    if (!Type.getObjectType(castInsn.desc).equals(boxedType)) {
                        return;
                    }
                    result.instructions.remove(castInsn);

                    // generally we expect to see the the boxed type to be unboxed with a method invocation (e.g.
                    // Integer.intValue()) however this may not be the case - let's see what actually happens
                    AbstractInsnNode thirdInsn = originalInsn.getNext();
                    if (thirdInsn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode invokeInsn = (MethodInsnNode) thirdInsn;
                        unboxesReturnValue = invokeInsn.owner.equals(boxedType.getInternalName())
                                && Type.getReturnType(invokeInsn.desc).equals(returnType);
                        if (unboxesReturnValue) {
                            result.instructions.remove(invokeInsn);
                        }
                    }
                }

                if (!unboxesReturnValue) {
                    // the weave method uses the boxed type or java.lang.Object - box the original's return value
                    String boxMethodDesc = Type.getMethodDescriptor(boxedType, returnType);
                    MethodInsnNode boxInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, boxedType.getInternalName(),
                            "valueOf", boxMethodDesc, false);
                    result.instructions.insert(originalInsn, boxInsn);
                }
                result.instructions.insert(originalInsn, endOfCallOriginal);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                // if there's a redundant cast to the return type, we can remove the it - otherwise we need to keep it
                if (nextInsn.getOpcode() == Opcodes.CHECKCAST) {
                    result.instructions.insert(nextInsn, endOfCallOriginal);
                    TypeInsnNode castInsn = (TypeInsnNode) nextInsn;
                    if (Type.getObjectType(castInsn.desc).equals(returnType)) {
                        result.instructions.remove(castInsn);
                    }
                } else {
                    result.instructions.insert(originalInsn, endOfCallOriginal);
                }
                break;
            default:
                return; // unsupported return type (e.g. Type.METHOD)
        }

        result.instructions.remove(originalInsn);
        this.success = true;
        this.result = result;
    }

    private static MethodInsnNode findOriginalIndex(InsnList instructions) {
        int size = instructions.size();
        for (int i = 0; i < size; i++) {
            AbstractInsnNode insn = instructions.get(i);
            if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (WeaveUtils.isOriginalMethodInvocation(methodInsn.owner, methodInsn.name, methodInsn.desc)) {
                    return methodInsn;
                }
            }
        }
        return null;
    }

    private static final Map<Type, Type> PRIMITIVE_TO_OBJECT_TYPE = Collections.unmodifiableMap(new HashMap<Type, Type>() {
        private static final long serialVersionUID = 1L;

        {
            put(Type.BOOLEAN_TYPE, Type.getType(Boolean.class));
            put(Type.BYTE_TYPE, Type.getType(Byte.class));
            put(Type.CHAR_TYPE, Type.getType(Character.class));
            put(Type.DOUBLE_TYPE, Type.getType(Double.class));
            put(Type.FLOAT_TYPE, Type.getType(Float.class));
            put(Type.INT_TYPE, Type.getType(Integer.class));
            put(Type.LONG_TYPE, Type.getType(Long.class));
            put(Type.SHORT_TYPE, Type.getType(Short.class));
        }
    });

    /**
     * Whether the replacement was successful or not. Replacements are unsuccessful because the return type of
     * callOriginal() is not the same as the return type of the original method.
     *
     * @return <code>true</code> if the replacement was successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the processed method, or <code>null</code> if the replacement was unsusccessful or unnecessary.
     *
     * @return the processed method, or <code>null</code> if the replacement was unsusccessful or unnecessary
     * @see #isSuccess()
     */
    public MethodNode getResult() {
        return result;
    }

    /**
     * A {@link LabelNode} that was inserted just <i>before</i> the original method instructions.
     *
     * @return {@link LabelNode} that was inserted just <i>before</i> the original method instructions
     */
    public LabelNode getStartOfOriginalMethodLabelNode() {
        return this.startOfCallOriginal;
    }

    /**
     * A {@link LabelNode} that was inserted just <i>after</i> the original method instructions.
     *
     * @return {@link LabelNode} that was inserted just <i>after</i> the original method instructions
     */
    public LabelNode getEndOfOriginalMethodLabelNode() {
        return this.endOfCallOriginal;
    }
}
