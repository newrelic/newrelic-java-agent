/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.asm;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.reflect.ClassReflection;
import com.newrelic.weave.utils.SynchronizedFieldNode;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ClassStructure {

    /**
     * This flag indicates the method names of the class should be collected.
     */
    public static final int METHODS = 0x0001;

    /**
     * This flag indicates the fields of the class should be collected.
     */
    public static final int FIELDS = 0x0002;

    /**
     * This flag indicates the class annotations on the class should be collected.
     */
    public static final int CLASS_ANNOTATIONS = 0x0004;

    /**
     * This flag indicates the method annotations on all of the methods should be collected. It implies the
     * {@link ClassStructure#METHODS} flag.
     */
    public static final int METHOD_ANNOTATIONS = 0x0008;

    /**
     * This flag indicates that all details about the class should be collected.
     */
    public static final int ALL = METHODS + FIELDS + CLASS_ANNOTATIONS + METHOD_ANNOTATIONS;

    private Map<Method, MethodDetails> methods;
    private Map<String, FieldNode> fields;
    private final Type type;
    protected final int access;
    protected final String superName;
    protected final String[] interfaces;
    protected Map<String, AnnotationDetails> classAnnotations;

    private ClassStructure(String className, int access, String superName, String[] interfaceNames) {
        type = Type.getObjectType(className);
        this.access = access;
        this.superName = superName;
        this.interfaces = interfaceNames;
    }

    public int getAccess() {
        return access;
    }

    /**
     * Returns the super class internal name, or null if there is no super class.
     */
    public String getSuperName() {
        return superName;
    }

    public Type getType() {
        return type;
    }

    /**
     * Returns the class' methods (if the {@link #CLASS_ANNOTATIONS} flag was set).
     */
    public Set<Method> getMethods() {
        return methods.keySet();
    }

    public Map<String, FieldNode> getFields() {
        return fields;
    }

    /**
     * Returns the annotations for a method (if the {@link #METHOD_ANNOTATIONS} flag was set). The key is the annotation
     * descriptor.
     * 
     * @see Type#getDescriptor(Class)
     */
    public Map<String, AnnotationDetails> getMethodAnnotations(Method method) {
        MethodDetails methodDetails = methods.get(method);
        if (methodDetails == null) {
            return Collections.emptyMap();
        }
        return methodDetails.annotations;
    }

    public String[] getInterfaces() {
        return interfaces;
    }

    /**
     * Returns the annotations for the class (if the {@link #CLASS_ANNOTATIONS} flag was set). The key is the annotation
     * descriptor.
     * 
     * @see Type#getDescriptor(Class)
     */
    public Map<String, AnnotationDetails> getClassAnnotations() {
        return classAnnotations;
    }

    @Override
    public String toString() {
        return type.getClassName();
    }

    public static ClassStructure getClassStructure(URL url) throws IOException {
        return getClassStructure(url, METHODS);
    }

    public static ClassStructure getClassStructure(URL url, int flags) throws IOException {
        return getClassStructure(Utils.getClassReaderFromResource(url.getPath(), url), flags);
    }

    public static ClassStructure getClassStructure(ClassReader cr, int flags) throws IOException {

        ClassStructure structure = new ClassStructure(cr.getClassName(), cr.getAccess(), cr.getSuperName(),
                cr.getInterfaces());

        ClassVisitor cv = structure.createClassVisitor(flags);
        if (cv != null) {
            cr.accept(cv, ClassReader.SKIP_CODE);
        }

        structure.methods = structure.methods == null ? Collections.<Method, MethodDetails>emptyMap()
                : Collections.unmodifiableMap(structure.methods);

        structure.classAnnotations = structure.classAnnotations == null ? Collections.<String, AnnotationDetails>emptyMap()
                : Collections.unmodifiableMap(structure.classAnnotations);

        structure.fields = structure.fields == null ? Collections.<String, FieldNode>emptyMap()
                : Collections.unmodifiableMap(structure.fields);

        return structure;
    }

    private ClassVisitor createClassVisitor(final int flags) {
        ClassVisitor cv = null;
        if (isMethodFlagSet(flags)) {
            cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                        String[] exceptions) {
                    if (null == methods) {
                        methods = new HashMap<>();
                    }
                    boolean isStatic = (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
                    Method method = new Method(name, desc);
                    if ((flags & METHOD_ANNOTATIONS) == METHOD_ANNOTATIONS) {
                        final MethodDetails details = new MethodDetails(new HashMap<String, AnnotationDetails>(),
                                isStatic);
                        methods.put(method, details);
                        return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, super.visitMethod(access, name, desc,
                                signature, exceptions)) {

                            @Override
                            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                                AnnotationDetails annotation = new AnnotationDetails(super.visitAnnotation(desc,
                                        visible), desc);
                                details.annotations.put(desc, annotation);
                                return annotation;
                            }

                        };
                    } else {
                        methods.put(method, isStatic ? EMPTY_METHOD_DEFAULTS_STATIC : EMPTY_METHOD_DEFAULTS_MEMBER);
                        return super.visitMethod(access, name, desc, signature, exceptions);
                    }
                }
            };
        }

        if ((flags & CLASS_ANNOTATIONS) == CLASS_ANNOTATIONS) {
            cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (null == classAnnotations) {
                        classAnnotations = new HashMap<>();
                    }
                    AnnotationDetails annotation = new AnnotationDetails(super.visitAnnotation(desc, visible), desc);
                    classAnnotations.put(desc, annotation);
                    return annotation;
                }
            };
        }

        if (isFieldFlagSet(flags)) {
            cv = new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

                @Override
                public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                    FieldNode field = new SynchronizedFieldNode(access, name, desc, signature, value);
                    if (fields == null) {
                        fields = new HashMap<>();
                    }
                    fields.put(name, field);
                    return super.visitField(access, name, desc, signature, value);
                }
            };
        }

        return cv;
    }

    public static ClassStructure getClassStructure(final Class<?> clazz) {
        return getClassStructure(clazz, METHODS);
    }

    public static ClassStructure getClassStructure(final Class<?> clazz, final int flags) {
        int access = 0;
        int modifiers = clazz.getModifiers();
        String superName = null;
        if (clazz.isAnnotation()) {
            access |= Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE;
            // the access modifiers for class visibility are wonky for Annotations
            if (!Modifier.isPrivate(modifiers)) {
                access |= Opcodes.ACC_PUBLIC;
            }
            superName = "java/lang/Object";
        } else if (clazz.isInterface()) {
            access |= Opcodes.ACC_INTERFACE;
            superName = "java/lang/Object";
        } else if (clazz.isEnum()) {
            access |= Opcodes.ACC_ENUM | Opcodes.ACC_SUPER;
        } else {
            access |= Opcodes.ACC_SUPER;
        }

        if (Modifier.isAbstract(modifiers)) {
            access |= Opcodes.ACC_ABSTRACT;
        }

        if (!clazz.isAnnotation()) {
            if (Modifier.isPublic(modifiers)) {
                access |= Opcodes.ACC_PUBLIC;
            } else if (Modifier.isPrivate(modifiers)) {
                access |= Opcodes.ACC_PRIVATE;
            } else if (Modifier.isProtected(modifiers)) {
                access |= Opcodes.ACC_PROTECTED;
            }
        }

        if (Modifier.isFinal(modifiers)) {
            access |= Opcodes.ACC_FINAL;
        }

        if (clazz.getSuperclass() != null) {
            superName = Type.getType(clazz.getSuperclass()).getInternalName();
        }

        // int ACC_SYNTHETIC = 0x1000; // class, field, method
        // int ACC_ANNOTATION = 0x2000; // class
        // int ACC_ENUM = 0x4000; // class(?) field inner

        String[] interfaces = new String[clazz.getInterfaces().length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaces[i] = Type.getType(clazz.getInterfaces()[i]).getInternalName();
        }

        final ClassStructure structure = new ClassStructure(Type.getType(clazz).getInternalName(), access, superName,
                interfaces);

        if ((flags & CLASS_ANNOTATIONS) == CLASS_ANNOTATIONS) {
            Annotation[] annotations = clazz.getAnnotations();
            if (annotations.length > 0) {
                structure.classAnnotations = new HashMap<>();
                for (Annotation annotation : annotations) {
                    AnnotationDetails annotationDetails = getAnnotationDetails(annotation);
                    structure.classAnnotations.put(annotationDetails.desc, annotationDetails);
                }
            }
        }

        if (structure.classAnnotations == null) {
            structure.classAnnotations = Collections.emptyMap();
        }

        if (isFieldFlagSet(flags)) {
            structure.fields = new HashMap<>();
            Field[] declaredFields = ClassReflection.getDeclaredFields(clazz);
            for (Field f : declaredFields) {
                FieldNode field = new SynchronizedFieldNode(0, f.getName(), Type.getDescriptor(f.getDeclaringClass()), null, null);
                structure.fields.put(f.getName(), field);
            }
        } else {
            structure.fields = ImmutableMap.of();
        }

        if (isMethodFlagSet(flags)) {
            structure.methods = new HashMap<>();
            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() {
                        java.lang.reflect.Method[] methods = ClassReflection.getDeclaredMethods(clazz);
                        for (java.lang.reflect.Method m : methods) {
                            structure.methods.put(Method.getMethod(m), getMethodDetails(m, flags,
                                    Modifier.isStatic(m.getModifiers())));
                        }

                        return null;
                    }
                });
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINEST, "Error getting methods of " + clazz.getName(), ex);
            }

            try {
                AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() {
                        Constructor<?>[] constructors = ClassReflection.getDeclaredConstructors(clazz);
                        for (Constructor<?> c : constructors) {
                            structure.methods.put(Method.getMethod(c), getMethodDetails(c, flags, false));
                        }
                        return null;
                    }

                });

            } catch (Exception ex) {
                Agent.LOG.log(Level.FINEST, "Error getting constructors of " + clazz.getName(), ex);
            }
        }

        return structure;
    }

    private static boolean isMethodFlagSet(final int flags) {
        return (flags & (METHODS + METHOD_ANNOTATIONS)) > 0;
    }

    private static boolean isFieldFlagSet(final int flags) {
        return (flags & (FIELDS)) > 0;
    }

    private static MethodDetails getMethodDetails(AccessibleObject method, int flags, boolean isStatic) {
        if ((flags & METHOD_ANNOTATIONS) == METHOD_ANNOTATIONS) {
            final MethodDetails details = new MethodDetails(new HashMap<String, AnnotationDetails>(), isStatic);
            for (Annotation annotation : method.getAnnotations()) {
                AnnotationDetails annotationDetails = getAnnotationDetails(annotation);
                details.annotations.put(annotationDetails.desc, annotationDetails);
            }
            return details;
        } else {
            return isStatic ? EMPTY_METHOD_DEFAULTS_STATIC : EMPTY_METHOD_DEFAULTS_MEMBER;
        }
    }

    private static AnnotationDetails getAnnotationDetails(Annotation annotation) {
        Class<? extends Annotation> annotationType = annotation.annotationType();
        String annotationDesc = Type.getDescriptor(annotationType);
        AnnotationDetails node = new AnnotationDetails(null, annotationDesc);

        for (java.lang.reflect.Method annotationMethod : annotationType.getDeclaredMethods()) {
            try {
                Object value = annotationMethod.invoke(annotation);
                node.getOrCreateAttributes().put(annotationMethod.getName(), value);
            } catch (Exception e) {
                Agent.LOG.log(Level.FINEST, "Error getting annotation value for " + annotationMethod.getName(), e);
            }
        }
        return node;
    }

    private static final MethodDetails EMPTY_METHOD_DEFAULTS_MEMBER = new MethodDetails(
            ImmutableMap.<String, AnnotationDetails>of(), false);

    private static final MethodDetails EMPTY_METHOD_DEFAULTS_STATIC = new MethodDetails(
            ImmutableMap.<String, AnnotationDetails>of(), true);

    private static class MethodDetails {
        final Map<String, AnnotationDetails> annotations;
        final boolean isStatic;

        public MethodDetails(Map<String, AnnotationDetails> annotations, boolean isStatic) {
            this.annotations = annotations;
            this.isStatic = isStatic;
        }
    }
}
