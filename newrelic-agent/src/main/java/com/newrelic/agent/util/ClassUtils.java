/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ClassUtils {

    /**
     * Finds the super definition of a method, giving preference to interfaces. For example, if you subclass ArrayList
     * and override get(int) and call findSuperDefinition with that method, it will return the get(int) method from the
     * List interface, not the definition from ArrayList.
     *
     * @param method
     */
    public static Method findSuperDefinition(Method method) {
        return findSuperDefinition(method.getDeclaringClass(), method);
    }

    private static Method findSuperDefinition(Class<?> clazz, Method method) {
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaces) {
            try {
                return interfaceClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (Exception e) {
            }
            method = findSuperDefinition(interfaceClass, method);
        }
        Class<?> parentClass = clazz.getSuperclass();
        if (parentClass != null) {
            try {
                method = parentClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
            }
            return findSuperDefinition(parentClass, method);
        }
        return method;
    }

    /**
     * Find all of the class references in the given class bytes.
     *
     * @param classBytes
     */
    public static Set<String> getClassReferences(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);

        final Set<String> classNames = new HashSet<>();
        ClassVisitor cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                addType(Type.getObjectType(superName));
                for (String iFace : interfaces) {
                    addType(Type.getObjectType(iFace));
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                addType(Type.getType(desc));
                return null;
            }

            private void addType(Type type) {
                if (type == null) {
                    return;
                } else if (type.getSort() == Type.ARRAY) {
                    addType(type.getElementType());
                } else if (type.getSort() == Type.OBJECT) {
                    classNames.add(type.getInternalName());
                }

            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                addMethodClasses(name, desc);

                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {

                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        addType(Type.getType(desc));
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        addMethodClasses(name, desc);
                    }

                    @Override
                    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end,
                            int index) {
                        addType(Type.getType(desc));
                    }

                };
            }

            private void addMethodClasses(String name, String desc) {
                org.objectweb.asm.commons.Method method = new org.objectweb.asm.commons.Method(name, desc);
                for (Type t : method.getArgumentTypes()) {
                    addType(t);
                }

            }

        };

        cr.accept(cv, ClassReader.SKIP_FRAMES);

        return classNames;
    }
}
