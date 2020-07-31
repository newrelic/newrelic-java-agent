/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Information about a class, generally used during validation of weave packages.
 */
public class ClassInformation {

    /**
     * Create a {@link ClassInformation} for the specified class byte[]
     * @param classBytes binary class representation
     * @return {@link ClassInformation} for the specified class byte[]
     */
    public static ClassInformation fromClassBytes(byte[] classBytes) {
        if (classBytes == null) {
            return null;
        }

        ClassInformationExtractor extractor = new ClassInformationExtractor();
        new ClassReader(classBytes).accept(extractor, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES
                | ClassReader.SKIP_DEBUG);
        return extractor.result;
    }

    /**
     * Create a {@link ClassInformation} for the specified ASM {@link ClassNode}
     * @param classNode ASM {@link ClassNode} class representation
     * @return {@link ClassInformation} for the specified {@link ClassNode}
     */
    public static ClassInformation fromClassNode(ClassNode classNode) {
        if (classNode == null) {
            return null;
        }

        ClassInformationExtractor extractor = new ClassInformationExtractor();
        classNode.accept(extractor);
        return extractor.result;
    }

    /**
     * {@link ClassVisitor} that extracts {@link ClassInformation}.
     */
    private static final class ClassInformationExtractor extends ClassVisitor {
        /**
         * {@link ClassInformation} information about the class that was visited.
         */
        final ClassInformation result = new ClassInformation();

        public ClassInformationExtractor() {
            super(WeaveUtils.ASM_API_LEVEL);
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
            final CollectAnnotationsVisitor collectAnnotationsVisitor = new CollectAnnotationsVisitor(this.api);
            return new AnnotationVisitor(this.api, collectAnnotationsVisitor) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    AnnotationNode annotationNode = collectAnnotationsVisitor.getAnnotationNode();
                    if (annotationNode != null) {
                        annotationNode.desc = desc;
                        result.classAnnotationNodes.add(annotationNode);
                        result.classAnnotationNames.add(Type.getType(annotationNode.desc).getClassName());
                    }
                }
            };
        }

        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            result.className = name;
            if (null != superName) {
                result.superName = superName;
            }
            if (null != interfaces) {
                Collections.addAll(result.interfaceNames, interfaces);
            }
        }

        public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
            result.fields.add(new MemberInformation(name, desc, access));
            return null;
        }

        public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, desc, name, signature, exceptions);
            return new MethodVisitor(this.api, methodVisitor) {
                final Set<AnnotationNode> annotations = new HashSet<>();

                @Override
                public AnnotationVisitor visitAnnotation(final String desc, boolean visible) {
                    final CollectAnnotationsVisitor collectAnnotationsVisitor = new CollectAnnotationsVisitor(this.api);
                    return new AnnotationVisitor(this.api, collectAnnotationsVisitor) {
                        @Override
                        public void visitEnd() {
                            super.visitEnd();

                            AnnotationNode annotationNode = collectAnnotationsVisitor.getAnnotationNode();
                            if (annotationNode != null) {
                                annotationNode.desc = desc;
                                annotations.add(annotationNode);
                                result.methodAnnotationNames.add(Type.getType(annotationNode.desc).getClassName());
                            }
                        }
                    };
                }

                @Override
                public void visitEnd() {
                    result.methods.add(new MemberInformation(name, desc, access, annotations));
                }
            };
        }
    }

    static class CollectAnnotationsVisitor extends org.objectweb.asm.AnnotationVisitor {
        final List<Object> values = new ArrayList<>();
        AnnotationNode result;

        public CollectAnnotationsVisitor(int api) {
            super(api);
        }

        @Override
        public void visit(String name, Object value) {
            values.add(name);
            values.add(value);
            super.visit(name, value);
        }

        @Override
        public void visitEnum(String name, String desc, String value) {
            values.add(name);
            values.add(value);
            super.visitEnum(name, desc, value);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, final String desc) {
            final CollectAnnotationsVisitor collectAnnotationsVisitor = new CollectAnnotationsVisitor(this.api);
            return new AnnotationVisitor(this.api, collectAnnotationsVisitor) {
                @Override
                public void visitEnd() {
                    super.visitEnd();
                    AnnotationNode annotationNode = collectAnnotationsVisitor.getAnnotationNode();
                    if (annotationNode != null) {
                        annotationNode.desc = desc;
                        values.add(annotationNode);
                    }
                }
            };
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            values.add(name);
            final CollectAnnotationsVisitor collectAnnotationsVisitor = new CollectAnnotationsVisitor(this.api);

            return new AnnotationVisitor(this.api) {
                @Override
                public void visitEnd() {
                    super.visitEnd();

                    AnnotationNode annotationNode = collectAnnotationsVisitor.getAnnotationNode();
                    if (annotationNode != null) {
                        values.add(annotationNode);
                    }
                }
            };
        }

        @Override
        public void visitEnd() {
            result = new AnnotationNode("");
            result.values = values;
            super.visitEnd();
        }

        public AnnotationNode getAnnotationNode() {
            return result;
        }
    }

    /**
     * Class name.
     */
    public String className;

    /**
     * Super class name.
     */
    public String superName;

    /**
     * Interfaces implemented <i>directly</i> by this class. Use {@link #getAllInterfaces(ClassInformationFinder)} to
     * find all inherited interfaces.
     */
    public final Set<String> interfaceNames = new HashSet<>();

    /**
     * Fields declared <i>directly</i> in this class. Use {@link #getAllFields(ClassInformationFinder)} to find all
     * inherited fields.
     */
    public final Set<MemberInformation> fields = new HashSet<>();

    /**
     * Methods declared <i>directly</i> in this class. Use {@link #getAllMethods(ClassInformationFinder)} to find all
     * inherited methods.
     */
    public final Set<MemberInformation> methods = new HashSet<>();

    /**
     * Class annotations.
     */
    public final Set<AnnotationNode> classAnnotationNodes = new HashSet<>();

    public final Set<String> classAnnotationNames = new HashSet<>();

    public final Set<String> methodAnnotationNames = new HashSet<>();

    /**
     * Find all fields, including inherited fields, using the specified {@link ClassInformationFinder}.
     *
     * @param finder {@link ClassInformationFinder} to use to resolve the type hierarchy
     * @return set containing {@link MemberInformation} for all fields
     * @throws IOException
     */
    public Set<MemberInformation> getAllFields(ClassInformationFinder finder) throws IOException {
        Set<MemberInformation> result = new HashSet<>(fields);

        if (superName != null) {
            ClassInformation info = finder.getClassInformation(superName);
            if (null != info) {
                result.addAll(info.getAllFields(finder));
            }
        }

        for (String interfaceName : interfaceNames) {
            ClassInformation info = finder.getClassInformation(interfaceName);
            if (null != info) {
                result.addAll(info.getAllFields(finder));
            }
        }

        return result;
    }

    /**
     * Find all methods, including inherited methods, using the specified {@link ClassInformationFinder}.
     *
     * @param finder {@link ClassInformationFinder} to use to resolve the type hierarchy
     * @return set containing {@link MemberInformation} for all methods
     * @throws IOException
     */
    public Set<MemberInformation> getAllMethods(ClassInformationFinder finder) throws IOException {
        Set<MemberInformation> result = new HashSet<>(methods);

        if (superName != null) {
            ClassInformation info = finder.getClassInformation(superName);
            if (null != info) {
                result.addAll(info.getAllMethods(finder));
            }
        }

        for (String interfaceName : interfaceNames) {
            ClassInformation info = finder.getClassInformation(interfaceName);
            if (null != info) {
                result.addAll(info.getAllMethods(finder));
            }
        }

        return result;
    }

    /**
     * Find all interfaces, including inherited interfaces, using the specified {@link ClassInformationFinder}.
     * 
     * @param finder {@link ClassInformationFinder} to use to resolve the type hierarchy
     * @return set of all interface names
     * @throws IOException
     */
    public Set<String> getAllInterfaces(ClassInformationFinder finder) throws IOException {
        Set<String> result = new HashSet<>(interfaceNames);

        if (superName != null) {
            ClassInformation info = finder.getClassInformation(superName);
            if (null != info) {
                result.addAll(info.getAllInterfaces(finder));
            }
        }

        for (String interfaceName : interfaceNames) {
            ClassInformation info = finder.getClassInformation(interfaceName);
            if (null != info) {
                result.addAll(info.getAllInterfaces(finder));
            }
        }

        return result;
    }

    /**
     * Find all supertype class names using the specified {@link ClassInformationFinder}.
     * 
     * @param finder {@link ClassInformationFinder} to use to resolve the type hierarchy
     * @return list of all supertype class names
     * @throws IOException
     */
    public List<String> getAllSuperNames(ClassInformationFinder finder) throws IOException {
        List<String> result = new LinkedList<>();

        String currentSuper = superName;
        while (currentSuper != null) {
            result.add(currentSuper);
            ClassInformation info = finder.getClassInformation(currentSuper);
            if (null == info) {
                break;
            } else {
                currentSuper = info.superName;
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ClassInformation that = (ClassInformation) o;

        if (!className.equals(that.className))
            return false;
        if (superName != null ? !superName.equals(that.superName) : that.superName != null)
            return false;
        if (!interfaceNames.equals(that.interfaceNames))
            return false;
        if (!fields.equals(that.fields))
            return false;
        return methods.equals(that.methods);

    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + (superName != null ? superName.hashCode() : 0);
        result = 31 * result + interfaceNames.hashCode();
        result = 31 * result + fields.hashCode();
        result = 31 * result + methods.hashCode();
        return result;
    }

    /**
     * Name, descriptor, and access information about a field or method.
     */
    public static class MemberInformation {
        public final String name;
        public final String desc;
        public final int access;
        public final Set<AnnotationNode> annotations;

        public MemberInformation(String name, String desc, int access) {
            this.name = name;
            this.desc = desc;
            this.access = access;
            this.annotations = new HashSet<>();
        }

        public MemberInformation(String name, String desc, int access, Set<AnnotationNode> annotations) {
            this.name = name;
            this.desc = desc;
            this.access = access;
            this.annotations = annotations;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            MemberInformation method = (MemberInformation) o;

            if (access != method.access)
                return false;
            if (!name.equals(method.name))
                return false;
            return desc.equals(method.desc);

        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + desc.hashCode();
            result = 31 * result + access;
            return result;
        }
    }
}
