/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.google.common.collect.ImmutableMap;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class with factory methods to create {@link ClassVisitor} implementations for renaming classes, methods, and
 * fields.
 */
public final class ReferenceUtils {

    private ReferenceUtils() {
    }

    /**
     * Create a {@link ClassVisitor} which will rename all class references in a class file exception for annotations.
     *
     * @param oldToNew map of old to new internal class names - every key in this map will be renamed to its value
     * @param delegate delegate {@link ClassVisitor}
     * @return {@link ClassVisitor} that will rename class references when visited
     */
    public static ClassVisitor getRenamingVisitor(final Map<String, String> oldToNew, ClassVisitor delegate) {
        return new ClassRenamingVisitor(oldToNew, delegate);
    }

    private static class ClassRenamingVisitor extends ClassVisitor {

        final Map<String, String> oldToNewInternalNames;
        final Map<String, String> oldToNewDescs;

        private ClassRenamingVisitor(Map<String, String> oldToNew, ClassVisitor delegatee) {
            super(WeaveUtils.ASM_API_LEVEL, delegatee);
            if (null == oldToNew) {
                oldToNew = new HashMap<>(0);
            }
            Map<String, String> internalNameMap = new HashMap<>(oldToNew.size());
            Map<String, String> descMap = new HashMap<>(oldToNew.size());
            for (String key : oldToNew.keySet()) {
                String internalNameKey = WeaveUtils.getClassInternalName(key);
                String internalNameValue = WeaveUtils.getClassInternalName(oldToNew.get(key));
                internalNameMap.put(internalNameKey, internalNameValue);
                String descKey = "L" + internalNameKey + ";";
                String descValue = "L" + internalNameValue + ";";
                descMap.put(descKey, descValue);
            }
            this.oldToNewInternalNames = ImmutableMap.<String, String> builder().putAll(internalNameMap).build();
            this.oldToNewDescs = ImmutableMap.<String, String> builder().putAll(descMap).build();
        }

        private String updateName(String oldName) {
            final String newName = oldToNewInternalNames.get(oldName);
            return null == newName ? oldName : newName;
        }

        private String updateDesc(String desc) {
            if (null == desc) {
                return null;
            }
            for (Map.Entry<String, String> entry : oldToNewDescs.entrySet()) {
                desc = desc.replace(entry.getKey(), entry.getValue());
            }
            return desc;
        }

        private String updateSignature(String signature) {
            if (null == signature) {
                return null;
            }
            // generics can nest other generics so we have to replace all names
            for (Map.Entry<String, String> entry : oldToNewInternalNames.entrySet()) {
                signature = signature.replace(entry.getKey(), entry.getValue());
            }
            return signature;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            name = updateName(name);
            superName = updateName(superName);
            if (interfaces != null) {
                for (int i = 0; i < interfaces.length; ++i) {
                    interfaces[i] = updateName(interfaces[i]);
                }
            }
            signature = updateSignature(signature);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            desc = updateDesc(desc);
            signature = updateSignature(signature);
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            desc = updateDesc(desc);
            signature = updateSignature(signature);
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ((Opcodes.ACC_ABSTRACT & access) == 0) {
                mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        desc = updateDesc(desc);
                        owner = updateName(owner);
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        desc = updateDesc(desc);
                        owner = updateName(owner);
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        desc = updateDesc(desc);
                        owner = updateName(owner);
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }

                    @Override
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
                            int index) {
                        desc = updateDesc(desc);
                        signature = updateSignature(signature);
                        super.visitLocalVariable(name, desc, signature, start, end, index);
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String desc, int dims) {
                        desc = updateDesc(desc);
                        super.visitMultiANewArrayInsn(desc, dims);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (type.startsWith("[")) {
                            // array
                            type = updateDesc(type);
                        } else {
                            // regular class name
                            type = updateName(type);
                        }
                        super.visitTypeInsn(opcode, type);
                    }

                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (cst instanceof Type) {
                            String newInternalClassName = updateName(((Type) cst).getInternalName());
                            super.visitLdcInsn(Type.getType("L" + newInternalClassName + ";"));
                            return;
                        }
                        super.visitLdcInsn(cst);
                    }
                };
            }
            return mv;
        }
    }

    /**
     * Create a {@link ClassVisitor} which will rename all references to a specific field on a class.
     *
     * @param delegate delegate visitor
     * @param className field's internal class name
     * @param oldFieldName old field name
     * @param newFieldName new field name
     * @return {@link ClassVisitor} that will rename field references when visited
     */
    public static ClassVisitor getFieldRenamingVisitor(ClassVisitor delegate, String className, String oldFieldName,
            String newFieldName) {
        return new FieldRenamingVisitor(delegate, className, oldFieldName, newFieldName);
    }

    private static class FieldRenamingVisitor extends ClassVisitor {
        private final String className;
        private final String oldFieldName;
        private final String newFieldName;
        private String visitingClassName;

        private FieldRenamingVisitor(ClassVisitor delegatee, String className, String oldFieldName,
                String newFieldName) {
            super(WeaveUtils.ASM_API_LEVEL, delegatee);
            this.className = className;
            this.oldFieldName = oldFieldName;
            this.newFieldName = newFieldName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            visitingClassName = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        private String updateFieldName(String fieldName) {
            if (oldFieldName.equals(fieldName)) {
                return newFieldName;
            } else {
                return fieldName;
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            if (className.equals(visitingClassName)) {
                name = updateFieldName(name);
            }
            return super.visitField(access, name, desc, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ((Opcodes.ACC_ABSTRACT & access) == 0) {
                mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        if (className.equals(owner)) {
                            name = updateFieldName(name);
                        }
                        super.visitFieldInsn(opcode, owner, name, desc);
                    }
                };
            }
            return mv;
        }
    }

    /**
     * Create a {@link ClassVisitor} which will rename all references to a specific method on a class.
     *
     * @param delegate delegate visitor
     * @param className method's internal class name
     * @param oldMethodName old method name
     * @param oldMethodDesc old method descriptor
     * @param newMethodName new method name
     * @return {@link ClassVisitor} that will rename method references when visited
     */
    public static ClassVisitor getMethodRenamingVisitor(ClassVisitor delegate, String className, String oldMethodName,
            String oldMethodDesc, String newMethodName) {
        return new MethodRenamingVisitor(delegate, className, oldMethodName, oldMethodDesc, newMethodName);
    }

    private static class MethodRenamingVisitor extends ClassVisitor {
        private final String className;
        private final String oldMethodName;
        private final String oldMethodDesc;
        private final String newMethodName;
        private String visitingClassName;

        private MethodRenamingVisitor(ClassVisitor delegatee, String className, String oldMethodName,
                String oldMethodDesc, String newMethodName) {
            super(WeaveUtils.ASM_API_LEVEL, delegatee);
            this.className = className;
            this.oldMethodName = oldMethodName;
            this.oldMethodDesc = oldMethodDesc;
            this.newMethodName = newMethodName;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            visitingClassName = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (className.equals(visitingClassName) && oldMethodName.equals(name) && oldMethodDesc.equals(desc)) {
                name = newMethodName;
            }
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            if ((Opcodes.ACC_ABSTRACT & access) == 0) {
                mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (className.equals(owner) && oldMethodName.equals(name) && oldMethodDesc.equals(desc)) {
                            name = newMethodName;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }

                    @Override
                    @SuppressWarnings("deprecation")
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (className.equals(owner) && oldMethodName.equals(name) && oldMethodDesc.equals(desc)) {
                            name = newMethodName;
                        }
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }
                };
            }
            return mv;
        }
    }

}
