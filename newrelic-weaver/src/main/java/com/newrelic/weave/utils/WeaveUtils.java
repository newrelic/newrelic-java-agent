/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveIntoAllMethods;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.MethodKey;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Weave utility methods.
 */
public final class WeaveUtils {
    /**
     * Version of ASM API used in the weaver.
     */
    public static final int ASM_API_LEVEL = Opcodes.ASM9;
    public static final int CLASS_FILE_VERSION_OFFSET = 44;

    /**
     * No op remapper used when inlining methods.
     */
    public static final Remapper NO_OP_REMAPPER = new Remapper() {
    };

    /**
     * Constructor method name.
     */
    public static final String INIT_NAME = "<init>";

    /**
     * Constructor method desc.
     */
    public static final String INIT_DESC = "()V";

    /**
     * Static class initializer method name.
     */
    public static final String CLASS_INIT_NAME = "<clinit>";

    /**
     * Weaver internal ASM name.
     */
    public static final Type WEAVER_TYPE = Type.getType(Weaver.class);

    /**
     * Weaver callOriginal() method.
     */
    public static final Method CALL_ORIGINAL_METHOD = new Method("callOriginal", Type.getType(Object.class),
            new Type[0]);

    /**
     * Weaver T getClassAnnotation(Class<T> annotationClass) method.
     */
    public static final Method CLASS_ANNOTATION_GETTER = new Method("getClassAnnotation", Type.getType(Annotation.class),
            new Type[] { Type.getType(Class.class) });

    /**
     * Weaver T getMethodAnnotation(Class<T> annotationClass) method.
     */
    public static final Method METHOD_ANNOTATION_GETTER = new Method("getMethodAnnotation", Type.getType(Annotation.class),
            new Type[] { Type.getType(Class.class) });

    /**
     * The name of the annotation classes attribute on the {@link WeaveWithAnnotation} annotation.
     */
    public static final String ANNOTATION_CLASSES_ATTRIBUTE_KEY = "annotationClasses";

    /**
     * The name of the MatchType attribute on the {@link WeaveWithAnnotation} annotation.
     */
    public static final String ANNOTATION_MATCH_TYPE_ATTRIBUTE_KEY = "type";

    /**
     * {@link WeaveIntoAllMethods} annotation type
     */
    public static final Type WEAVE_ALL_METHODS_TYPE = Type.getType(WeaveIntoAllMethods.class);

    /**
     * {@link WeaveWithAnnotation} annotation type
     */
    public static final Type WEAVE_WITH_ANNOTATION_TYPE = Type.getType(WeaveWithAnnotation.class);

    /**
     * Static class initializer method.
     */
    public static final Method CLASS_INIT_METHOD = new Method(CLASS_INIT_NAME, INIT_DESC);

    /**
     * Default constructor method.
     */
    public static final Method DEFAULT_CONSTRUCTOR = new Method(INIT_NAME, INIT_DESC);

    /**
     * Object internal ASM name.
     */
    public static final String JAVA_LANG_OBJECT_NAME = Type.getInternalName(Object.class);

    /**
     * Used to represent the bootstrap classloader.
     */
    public static final ClassLoader BOOTSTRAP_PLACEHOLDER = new ClassLoader(null) {
    };

    /**
     * Used to find synthetic accessor methods generated by the compiler. These are generated when inner classes
     * implicitly access parent class members.
     */
    public static final Pattern SYNTHETIC_ACCESSOR_PATTERN = Pattern.compile("^access\\$[0-9]+$");

    /**
     * We repalce Weaver.callOriginal() with an invoke of the weave method. We add this prefix to the weave class name
     * so the method owner+name will not collide with the original owner+name. This allows weaved code to make recursive
     * calls behave as expected.
     */
    public static final String INLINER_PREFIX = "INLINER_";

    /**
     * At runtime, this will contain the max class major version that the jvm can read.
     * <p/>
     * java 1.8 = 52<br/>
     * java 1.7 = 51<br/>
     * java 1.6 = 50<br/>
     */
    public static final int RUNTIME_MAX_SUPPORTED_CLASS_VERSION = getRuntimeMaxSupportedClassVersion();

    /**
     * This is the major class version that represents Java 1.6
     */
    public static final int JAVA_6_CLASS_VERSION = 50;

    public static final Set<MethodKey> METHODS_WE_NEVER_INSTRUMENT = ImmutableSet.of(new MethodKey("equals",
                    "(Ljava/lang/Object;)Z"), new MethodKey("toString", "()Ljava/lang/String;"), new MethodKey("finalize", "()V"),
            new MethodKey("hashCode", "()I"), new MethodKey("clone", "()Ljava/lang/Object;"));

    /**
     * java.lang.Object was compiled with the max supported class version of the jvm we are running in.
     *
     * @return the class major version of java.lang.Object
     */
    private static int getRuntimeMaxSupportedClassVersion() {
        try {
            double jvmSpecVersion = Double.parseDouble(System.getProperty("java.specification.version"));
            if (jvmSpecVersion >= 9) {
                return (int) jvmSpecVersion + CLASS_FILE_VERSION_OFFSET;
            } else if (jvmSpecVersion >= 1.8) {
                return 52;
            } else if (jvmSpecVersion == 1.7) {
                return 51;
            } else if (jvmSpecVersion == 1.6) {
                return 50;
            } else if (jvmSpecVersion == 1.5) {
                return 49;
            } else if (jvmSpecVersion == 1.4) {
                return 48;
            } else if (jvmSpecVersion == 1.3) {
                return 47;
            } else if (jvmSpecVersion == 1.2) {
                return 46;
            } else if (jvmSpecVersion <= 1.1) {
                return 45;
            }
        } catch (Throwable t) {
        }
        return 0;
    }

    private WeaveUtils() {
    }

    /**
     * Find a method by name and desc in the specified collection.
     *
     * @param methodNodes collection to search
     * @param queryNode   MethodNode with name and desc to search for
     * @return matched method or <code>null</code> if there was no match
     */
    public static MethodNode findMatch(Collection<MethodNode> methodNodes, MethodNode queryNode) {
        return findMatch(methodNodes, queryNode.name, queryNode.desc);
    }

    /**
     * Find a method by name and desc in the specified collection.
     *
     * @param methodNodes collection to search
     * @param queryMethod Method to search for
     * @return matched method or <code>null</code> if there was no match
     */
    public static MethodNode findMatch(Collection<MethodNode> methodNodes, Method queryMethod) {
        return findMatch(methodNodes, queryMethod.getName(), queryMethod.getDescriptor());
    }

    /**
     * Find a method by name and desc in the specified collection.
     *
     * @param methodNodes collection to search
     * @param name        method name to search for
     * @param desc        method desc to search for
     * @return matched method or <code>null</code> if there was no match
     */
    public static MethodNode findMatch(Collection<MethodNode> methodNodes, String name, String desc) {
        for (MethodNode searchNode : methodNodes) {
            if (searchNode.name.equals(name) && searchNode.desc.equals(desc)) {
                return searchNode;
            }
        }
        return null;
    }

    /**
     * Find a field by name and desc in the specified collection.
     *
     * @param fieldNodes collection to search
     * @param name       name to search for
     * @return matched field or <code>null</code> if there was no match
     */
    public static FieldNode findMatch(Collection<FieldNode> fieldNodes, String name) {
        for (FieldNode fieldNode : fieldNodes) {
            if (fieldNode.name.equals(name)) {
                return fieldNode;
            }
        }
        return null;
    }

    /**
     * Find a required field by name and desc in the specified collection.
     *
     * @param fieldNodes collection to search
     * @param name       name to search for
     * @return matched field
     * @throws IllegalArgumentException if there was no match
     */
    public static FieldNode findRequiredMatch(Collection<FieldNode> fieldNodes, String name) {
        FieldNode result = findMatch(fieldNodes, name);
        if (result == null) {
            throw new IllegalArgumentException("Could not find required field name: " + name
                    + " in specified collection.");
        }
        return result;
    }

    /**
     * Tests whether the specified method invocation is to code>Weaver.callOriginal()</code>.
     *
     * @param owner method owner
     * @param name  method name
     * @param desc  method desc
     * @return <code>true</code> if owner, name, and desc correspond to <code>Weaver.callOriginal()</code>
     */
    public static boolean isOriginalMethodInvocation(String owner, String name, String desc) {
        return owner.equals(WEAVER_TYPE.getInternalName()) && name.equals(CALL_ORIGINAL_METHOD.getName())
                && desc.equals(CALL_ORIGINAL_METHOD.getDescriptor());
    }

    /**
     * Tests whether the specified method invocation is to code>Weaver.getClassAnnotation()</code>.
     *
     * @param owner method owner
     * @param name  method name
     * @param desc  method desc
     * @return <code>true</code> if owner, name, and desc correspond to <code>Weaver.getClassAnnotation()</code>
     */
    public static boolean isClassAnnotationGetter(String owner, String name, String desc) {
        return owner.equals(WEAVER_TYPE.getInternalName()) && name.equals(CLASS_ANNOTATION_GETTER.getName())
                && desc.equals(CLASS_ANNOTATION_GETTER.getDescriptor());
    }

    /**
     * Tests whether the specified method invocation is to <code>Weaver.getMethodAnnotation()</code>.
     *
     * @param owner method owner
     * @param name  method name
     * @param desc  method desc
     * @return <code>true</code> if owner, name, and desc correspond to <code>Weaver.getMethodAnnotation()</code>
     */
    public static boolean isMethodAnnotationGetter(String owner, String name, String desc) {
        return owner.equals(WEAVER_TYPE.getInternalName()) && name.equals(METHOD_ANNOTATION_GETTER.getName())
                && desc.equals(METHOD_ANNOTATION_GETTER.getDescriptor());
    }

    /**
     * Tests whether the specified class node represents a nonstatic inner class.
     *
     * @param classNode class to test
     * @return <code>true</code> if the specified class node represents a nonstatic inner class
     */
    public static boolean isNonstaticInnerClass(ClassNode classNode) {
        if (classNode.innerClasses != null) {
            for (InnerClassNode innerClassNode : classNode.innerClasses) {
                if ((innerClassNode.access & Opcodes.ACC_STATIC) == 0 && innerClassNode.name.equals(classNode.name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Tests whether the specified class node represents a anonymous inner class.
     *
     * @param innerClassNode class to test
     * @return <code>true</code> if the specified class node represents a nonstatic inner class
     */
    public static boolean isAnonymousInnerClass(InnerClassNode innerClassNode) {
        return innerClassNode.innerName == null;
    }

    /**
     * Tests whether the method name represents a synthetic accessor methods generated by the compiler. These are
     * generated when inner classes implicitly access parent class members.
     *
     * @param methodName method to test
     * @return <code>true</code> if the name represents a synthetic accessor methods generated by the compiler
     */
    public static boolean isSyntheticAccessor(String methodName) {
        return SYNTHETIC_ACCESSOR_PATTERN.matcher(methodName).matches();
    }

    /**
     * Find the outer class type for the nested class.
     *
     * @param nestedClassNode class node representing a nested class
     * @return outer class type or <code>null</code> if it is not a nested class
     */
    public static Type getOuterClassType(ClassNode nestedClassNode) {
        if (nestedClassNode.innerClasses != null) {
            for (InnerClassNode innerClassNode : nestedClassNode.innerClasses) {
                if (innerClassNode.name.equals(nestedClassNode.name)) {
                    return Type.getType("L" + innerClassNode.outerName + ";");
                }
            }
        }
        return null;
    }

    /**
     * Test if the specified method represents an "empty" (usually generated) constructor.
     *
     * @return <code>true</code> if the specified method represents an "empty" constructor
     */
    public static boolean isEmptyConstructor(MethodNode method) {
        if (!method.name.equals(INIT_NAME) || !method.desc.equals("()V") || method.visibleAnnotations != null
                || method.invisibleAnnotations != null) {
            return false;
        }

        final int init = 0;
        final int aload0 = 1;
        final int invokespecial = 2;
        int state = init;
        InsnList instructions = method.instructions;
        int size = instructions.size();
        for (int i = 0; i < size; i++) {
            AbstractInsnNode node = instructions.get(i);
            switch (node.getOpcode()) {
                case Opcodes.ALOAD:
                    if (state != init) {
                        return false;
                    }
                    state = aload0;
                    break;
                case Opcodes.INVOKESPECIAL:
                    if (state != aload0) {
                        return false;
                    }
                    state = invokespecial;
                    break;
                case Opcodes.RETURN:
                    return state == invokespecial;
                default:
                    if (node.getType() == AbstractInsnNode.LINE || node.getType() == AbstractInsnNode.LABEL
                            || node.getType() == AbstractInsnNode.FRAME) {
                        break;
                    }
                    return false;
            }
        }
        return false;
    }

    /**
     * Create a new SynchronizedMethodNode with the access, name, desc, signature, and exceptions of the specified
     * MethodNode.
     *
     * @param source MethodNode to copy access, name, desc, signature, and exceptions from
     * @return new SynchronizedMethodNode with the access, name, desc, signature, and exceptions of the specified
     * MethodNode
     */
    public static MethodNode newMethodNode(MethodNode source) {
        return new SynchronizedMethodNode(ASM_API_LEVEL, source.access, source.name, source.desc, source.signature,
                source.exceptions.toArray(new String[source.exceptions.size()]));
    }

    /**
     * Returns a copy of the specified ClassNode, stripping out all JSR/RET instructions.
     *
     * @param source ClassNode to copy
     */
    public static ClassNode copyWithoutJSRInstructions(ClassNode source) {
        ClassNode result = new SynchronizedClassNode(ASM_API_LEVEL);
        source.accept(new ClassVisitor(ASM_API_LEVEL, result) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
            }
        });
        return result;
    }

    /**
     * Returns a copy of the specified MethodNode.
     *
     * @param source MethodNode to copy
     */
    public static MethodNode copy(MethodNode source) {
        MethodNode result = newMethodNode(source);
        source.accept(result);
        return result;
    }

    /**
     * Returns a copy of the specified ClassNode with all references to the "oldName" parameter renamed to the value
     * of the "newName" parameter.
     * <p>
     * This method is best used when a template ClassNode is available and you want to make a new copy of it every time
     * it needs to be used so it can have a unique name.
     *
     * @param source ClassNode to copy and rename
     */
    public static ClassNode copyAndRename(ClassNode source, final String oldName, final String newName) {
        ClassNode result = new SynchronizedClassNode(ASM_API_LEVEL);
        source.accept(new ClassVisitor(ASM_API_LEVEL, result) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                // Rename the Class
                super.visit(version, access, name.equals(oldName) ? newName : name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                    String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodVisitor(ASM_API_LEVEL, methodVisitor) {
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        // Rename any Field instructions
                        super.visitFieldInsn(opcode, owner.equals(oldName) ? newName : owner, name, desc);
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        // Rename any Method instructions
                        super.visitMethodInsn(opcode, owner.equals(oldName) ? newName : owner, name, desc, itf);
                    }

                    @Override
                    public void visitLdcInsn(Object cst) {
                        if (cst instanceof Type) {
                            if (oldName.equals(((Type) cst).getInternalName())) {
                                super.visitLdcInsn(Type.getType("L" + newName + ";"));
                                return;
                            }
                        }
                        super.visitLdcInsn(cst);
                    }

                    @Override
                    public void visitTypeInsn(int opcode, String type) {
                        if (oldName.equals(type)) {
                            super.visitTypeInsn(opcode, newName);
                            return;
                        }
                        super.visitTypeInsn(opcode, type);
                    }
                };
            }
        });

        return result;
    }

    /**
     * Read an array of class bytes into an ASM ClassNode
     *
     * @param classBytes classBytes from a .class file
     * @return ASM ClassNode representation of the
     */
    public static ClassNode convertToClassNode(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode result = new SynchronizedClassNode(ASM_API_LEVEL);
        reader.accept(result, ClassReader.SKIP_FRAMES);
        return result;
    }

    //make an analyzer

    private static void printMethodAnalysis(String owner, MethodNode method) throws AnalyzerException {
        System.out.println("Analyzing method: " + method.name);
        BasicInterpreter interpreter = new BasicInterpreter();
        Analyzer<BasicValue> a = new Analyzer<>(interpreter);
        Frame<BasicValue>[] frames = a.analyze(owner, method);
        Map<AbstractInsnNode, Integer> rtStacks = new HashMap<>();
        for (int j = 0; j < method.instructions.size(); ++j) {
            AbstractInsnNode insn = method.instructions.get(j);
            Frame<BasicValue> frame = frames[j];
            if (frame != null) {
                System.out.println("Locals: " + stringLocals(frame) + " stack: " + stringStack(frame) + " " + stringifyInstruction(insn));
            }
        }
    }

    private static String stringLocals(Frame<BasicValue> frame) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frame.getLocals(); i++) {
            BasicValue value = frame.getLocal(i);
            sb.append(value);
            sb.append(" . ");
        }
        return sb.toString();
    }

    private static String stringStack(Frame<BasicValue> frame) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < frame.getStackSize(); i++) {
            BasicValue value = frame.getStack(i);
            sb.append(value);
            sb.append(" . ");
        }
        return sb.toString();
    }

    //print class node
    public static void printClassFrames(byte [] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        reader.accept(cn, 0);
        for (MethodNode methodNode : cn.methods) {
            try {
                printMethodAnalysis(cn.name, methodNode);
            } catch (AnalyzerException e) {
                e.printStackTrace();
            }
        }
    }

    //TEMPORARY

    public static void getInstructionAtBCI(int bci, byte [] classBytes) {
        int bciMargin = 3;
        ClassReader reader = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        reader.accept(cn, 0);
        for (MethodNode methodNode : cn.methods) {
            int currentBytecodeIndex = 0;
            for (AbstractInsnNode instruction : methodNode.instructions) {
                if (instruction.getType() != AbstractInsnNode.LABEL &&
                        instruction.getType() != AbstractInsnNode.LINE &&
                        instruction.getType() != AbstractInsnNode.FRAME) {

                    if (currentBytecodeIndex > bci - bciMargin || currentBytecodeIndex < bci + bciMargin) {
                        if (currentBytecodeIndex == bci) {
                            System.out.print("Target Instruction >>>>>");
                        }
                        System.out.println("bci: " + currentBytecodeIndex + " " + stringifyInstruction(instruction));
                    }
                    currentBytecodeIndex++;
                }

            }
            System.out.println("Finished instrumenting method: " + methodNode.name);
        }
    }

    public static void createReadableClassFileFromClassNode(ClassNode cn, String originalName, String targetName, String destDir) {
        if (targetName == null || originalName.contains(targetName)) {
            System.out.println("Weaved composite ClassLoader");
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cn.accept(cw);
            byte[] classBytes = cw.toByteArray();
            createReadableClassFileFromByteArray(classBytes, originalName, null, destDir);
        }
    }

    public static void createReadableClassFileFromByteArray(byte[] classBytes, String originalName, String targetName, String destDir){
        if (targetName == null || originalName.contains(targetName)) {
            final File MY_DIRECTORY = new File(destDir);
            try {
                File newFile = File.createTempFile(originalName.replace('/', '_'), ".new", MY_DIRECTORY);
                PrintWriter pw = new PrintWriter(newFile);
                ClassReader cr = new ClassReader(classBytes);
                org.objectweb.asm.util.TraceClassVisitor mv = new org.objectweb.asm.util.TraceClassVisitor(pw);
                cr.accept(mv, ClassReader.EXPAND_FRAMES);
                pw.flush();
                System.out.println("Wrote " + newFile.getAbsolutePath());
            } catch (IOException e) {
                System.out.println("Error writing ClassLoader files");
            }
        }
    }

    /**
     * Utility method to print human-readable bytecode instructions of a MethodNode.
     *
     * @param mn the node to print
     */
    public static void printAllInstructions(MethodNode mn) {
        for (AbstractInsnNode insn : mn.instructions) {
            System.out.println(stringifyInstruction(insn));
        }
    }

    public static String stringifyInstruction(AbstractInsnNode node) {
        Textifier p = new Textifier(WeaveUtils.ASM_API_LEVEL) {
            @Override
            protected void appendLabel(Label l) {
                if (labelNames == null) {
                    labelNames = new HashMap<>();
                }
                String name = labelNames.get(l);
                if (name == null) {
                    name = l.toString();
                    labelNames.put(l, name);
                }
                stringBuilder.append(name);
            }
        };
        TraceMethodVisitor mv = new TraceMethodVisitor(p);
        node.accept(mv);
        StringWriter sw = new StringWriter();
        p.print(new PrintWriter(sw));
        return sw.toString().replace('\n', ' ');
    }

    /**
     * Converts an ASM {@link ClassNode} to a byte array.
     *
     * @param classNode       class node to convert
     * @param classInfoFinder the classloader used to create the {@link PatchedClassWriter}
     * @return byte array representing the specified class node
     */
    public static byte[] convertToClassBytes(ClassNode classNode, ClassInformationFinder classInfoFinder) {
        ClassWriter cw = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, classInfoFinder);
        classNode.accept(cw);
        return cw.toByteArray();
    }

    /**
     * Retrieve the class bytes from the specified {@link ClassFinder}, or <code>null</code> if the class could not be
     * found.
     *
     * @param classname internal class name
     * @param finder    {@link ClassFinder} implementation to get bytes from
     * @return class bytes, or <code>null</code> if the class could not be found
     * @throws IOException
     */
    public static byte[] getClassBytesFromClassFinder(String classname, ClassFinder finder) throws IOException {
        URL location = finder.findResource(classname);
        if (null == location) {
            return null; // no such resource
        }

        InputStream is = location.openStream();
        return Streams.read(is, true);
    }

    /**
     * Read a ClassLoader's resource into a byte array.
     *
     * @param classname   Internal or Fully qualified name of the class
     * @param classloader the classloader to read the resource from
     * @return the resource bytes (class bytes) or null if no resource was found.
     * @throws IOException
     */
    public static byte[] getClassBytesFromClassLoaderResource(String classname, ClassLoader classloader)
            throws IOException {
        ClassFinder cf = new ClassLoaderFinder(classloader);
        return WeaveUtils.getClassBytesFromClassFinder(classname, cf);
    }

    /**
     * Check if the access flags indicate a private or protected access level.
     *
     * @param access ASM access flags
     * @return <code>true</code> if flags indicate a private or protected access level
     */
    public static boolean isPrivateOrProtected(int access) {
        return (access & Opcodes.ACC_PROTECTED) != 0 || (access & Opcodes.ACC_PRIVATE) != 0;
    }

    /**
     * Creates a new {@link LabelNode} which can be safely inserted into a {@link MethodNode} instruction list.
     *
     * @return a new LabelNode.
     */
    public static LabelNode makeLabelNode() {
        LabelNode labelNode = new LabelNode();
        labelNode.getLabel().info = labelNode;
        return labelNode;
    }

    /**
     * Pull a method node out of a class node, or null if there is no such method in the class
     *
     * @return method node matching the specified name and descriptor, or <code>null</code> if it does not exist
     */
    public static MethodNode getMethodNode(ClassNode classnode, String methodName, String methodDesc) {
        for (MethodNode method : classnode.methods) {
            if (method.name.equals(methodName) && method.desc.equals(methodDesc)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Returns true if both access params have the same value of all given opcode flags.
     *
     * @param access1 access flags.
     * @param access2 access flags.
     * @param opcodes flags to check. See {@link org.objectweb.asm.Opcodes}
     * @return True if access1 and access2 have the same value for all opcodes.
     * @see Opcodes
     */
    public static boolean flagsMatch(int access1, int access2, int... opcodes) {
        for (int opcode : opcodes) {
            if (((opcode & access1) != 0) != ((opcode & access2) != 0)) {
                return false;
            }
        }
        return true;
    }

    // @formatter:off
    private static final Map<Integer, String> HUMAN_READABLE_OPCODES = ImmutableMap.<Integer, String>builder()
            .put(Opcodes.ACC_PUBLIC, "public")
            .put(Opcodes.ACC_PRIVATE, "private")
            .put(Opcodes.ACC_PROTECTED, "protected")
            .put(Opcodes.ACC_STATIC, "static")
            .put(Opcodes.ACC_FINAL, "final")
            .put(Opcodes.ACC_SYNCHRONIZED, "(synchronized|super)")
            .put(Opcodes.ACC_BRIDGE, "(bridge|volatile)")
            .put(Opcodes.ACC_NATIVE, "native")
            .put(Opcodes.ACC_INTERFACE, "interface")
            .put(Opcodes.ACC_ABSTRACT, "abstract")
            .put(Opcodes.ACC_STRICT, "strict")
            .put(Opcodes.ACC_SYNTHETIC, "synthetic")
            .put(Opcodes.ACC_ANNOTATION, "annotation")
            .put(Opcodes.ACC_ENUM, "enum")
            .build();
    // @formatter:on

    /**
     * Passes the sum of the opcodes array to {@link WeaveUtils#humanReadableAccessFlags(int)}
     */
    public static String humanReadableAccessFlags(int[] opcodes) {
        int sum = 0;
        for (int opcode : opcodes) {
            sum += opcode;
        }
        return humanReadableAccessFlags(sum);
    }

    /**
     * Returns a human-readable comma-separated description of the asm access flags.<br/>
     * Any ints that don't map to an asm opcode flag will retain their int value.
     *
     * @param opcodes ASM access flags to check
     */
    public static String humanReadableAccessFlags(int opcodes) {
        String humanReadableFlags = "";
        for (int supportedOpcode : HUMAN_READABLE_OPCODES.keySet()) {
            if (flagsMatch(supportedOpcode, opcodes, supportedOpcode)) {
                if (humanReadableFlags.length() > 0) {
                    humanReadableFlags += ",";
                }
                humanReadableFlags += HUMAN_READABLE_OPCODES.get(supportedOpcode);
                opcodes = opcodes ^ supportedOpcode; // remove the suported flag from the opcodes
            }
        }
        if (opcodes != 0) {
            // unknown flags. Just append the remaining int value
            if (humanReadableFlags.length() > 0) {
                humanReadableFlags += ",";
            }
            humanReadableFlags += "UnknownRemaingFlags=" + opcodes;
        }
        return humanReadableFlags;
    }

    /**
     * Returns true if the class being read by the {@link ClassReader} is marked with a {@link Weave} annotation.
     */
    public static boolean isWeavedClass(ClassReader reader) {
        final boolean[] weaved = new boolean[] { false };
        reader.accept(new ClassVisitor(WeaveUtils.ASM_API_LEVEL) {

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (Type.getDescriptor(Weave.class).equals(desc)) {
                    weaved[0] = true;
                }
                return null;
            }

        }, ClassReader.SKIP_CODE);
        return weaved[0];
    }

    /**
     * Update class versions to the max supported runtime class version.
     */
    public static void updateClassVersion(ClassNode node) {
        if (node.version < RUNTIME_MAX_SUPPORTED_CLASS_VERSION) {
            node.version = RUNTIME_MAX_SUPPORTED_CLASS_VERSION;
        }
    }

    /**
     * java.lang.Class -> java/lang/Class.class
     */
    public static String getClassResourceName(String binaryName) {
        if (binaryName.endsWith(".class")) {
            return binaryName;
        } else {
            return binaryName.replace('.', '/') + ".class";
        }
    }

    /**
     * java.lang.Class -> java/lang/Class
     */
    public static String getClassInternalName(String binaryName) {
        return binaryName.replaceFirst("\\.class$", "").replace('.', '/');
    }

    /**
     * java/lang/Class.class -> java.lang.Class
     */
    public static String getClassBinaryName(String resourceName) {
        return resourceName.replaceFirst("\\.class$", "").replace('/', '.');
    }

    public static boolean isConstructor(String methodName) {
        return INIT_NAME.equals(methodName);
    }

    public static boolean isMain(int access, String methodName, String methodDesc) {
        return (access & Opcodes.ACC_PUBLIC) == 1 && (access & Opcodes.ACC_STATIC) == 8 && "main".equals(methodName) &&
                "([Ljava/lang/String;)V".equals(methodDesc);
    }

    public static boolean isStaticInitializer(String methodName) {
        return CLASS_INIT_NAME.equals(methodName);
    }

    public static MethodInsnNode getUnboxingInstruction(Type returnType) {
        if (Type.INT_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "()I", false);
        } else if (Type.BOOLEAN_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
        } else if (Type.CHAR_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C", false);
        } else if (Type.BYTE_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "()B", false);
        } else if (Type.SHORT_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "()S", false);
        } else if (Type.FLOAT_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Float.class), "floatValue", "()F", false);
        } else if (Type.LONG_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "()J", false);
        } else if (Type.DOUBLE_TYPE.equals(returnType)) {
            return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "()D", false);
        }

        return null;
    }

    public static String getClassInternalName(Type type) {
        if (Type.INT_TYPE.equals(type)) {
            return Type.getInternalName(Integer.class);
        } else if (Type.BOOLEAN_TYPE.equals(type)) {
            return Type.getInternalName(Boolean.class);
        } else if (Type.CHAR_TYPE.equals(type)) {
            return Type.getInternalName(Character.class);
        } else if (Type.BYTE_TYPE.equals(type)) {
            return Type.getInternalName(Byte.class);
        } else if (Type.SHORT_TYPE.equals(type)) {
            return Type.getInternalName(Short.class);
        } else if (Type.FLOAT_TYPE.equals(type)) {
            return Type.getInternalName(Float.class);
        } else if (Type.LONG_TYPE.equals(type)) {
            return Type.getInternalName(Long.class);
        } else if (Type.DOUBLE_TYPE.equals(type)) {
            return Type.getInternalName(Double.class);
        } else {
            return type.getInternalName();
        }
    }

    public static int getReturnOpcodeForReturnType(Type returnType) {
        if (Type.INT_TYPE.equals(returnType)) {
            return Opcodes.IRETURN;
        } else if (Type.BOOLEAN_TYPE.equals(returnType)) {
            return Opcodes.IRETURN;
        } else if (Type.CHAR_TYPE.equals(returnType)) {
            return Opcodes.IRETURN;
        } else if (Type.BYTE_TYPE.equals(returnType)) {
            return Opcodes.IRETURN;
        } else if (Type.SHORT_TYPE.equals(returnType)) {
            return Opcodes.IRETURN;
        } else if (Type.FLOAT_TYPE.equals(returnType)) {
            return Opcodes.FRETURN;
        } else if (Type.LONG_TYPE.equals(returnType)) {
            return Opcodes.LRETURN;
        } else if (Type.DOUBLE_TYPE.equals(returnType)) {
            return Opcodes.DRETURN;
        } else if (Type.VOID_TYPE.equals(returnType)) {
            return Opcodes.RETURN;
        }

        return Opcodes.ARETURN;
    }

    public static AbstractInsnNode getCheckCastInstruction(String classInternalName) {
        if (Type.getInternalName(Object.class).equals(classInternalName)) {
            // Don't need a CHECKCAST instruction
            return null;
        }
        return new TypeInsnNode(Opcodes.CHECKCAST, classInternalName);
    }

    /**
     * Returns a list of all the LabelNode instructions.
     *
     * @param instructions the instructions list to look through
     * @return List of LabelNode instructions found
     */
    public static List<LabelNode> findLabels(InsnList instructions) {
        List<LabelNode> labels = new ArrayList<>();
        AbstractInsnNode instructionNode = instructions.getFirst();
        while (instructionNode != null) {
            if (instructionNode instanceof LabelNode) {
                labels.add((LabelNode) instructionNode);
            }
            instructionNode = instructionNode.getNext();
        }
        return labels;
    }

    /**
     * Returns a list of all annotations found on the provided method
     *
     * @param method the method to look at for annotations
     * @return all annotations present on the provided method
     */
    public static List<AnnotationNode> getMethodAnnotations(MethodNode method) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (method.invisibleAnnotations != null) {
            annotations.addAll(method.invisibleAnnotations);
        }
        if (method.visibleAnnotations != null) {
            annotations.addAll(method.visibleAnnotations);
        }
        return annotations;
    }

    /**
     * Returns a list of all annotations found on the provided class
     *
     * @param classNode the class to look at for annotations
     * @return all annotations present on the provided class
     */
    public static List<AnnotationNode> getClassAnnotations(ClassNode classNode) {
        List<AnnotationNode> annotations = new ArrayList<>();
        if (classNode.invisibleAnnotations != null) {
            annotations.addAll(classNode.invisibleAnnotations);
        }
        if (classNode.visibleAnnotations != null) {
            annotations.addAll(classNode.visibleAnnotations);
        }
        return annotations;
    }

    /**
     * Returns a list of all annotations found on all methods of the provided class
     *
     * @param classNode the class to look at for annotations
     * @return all annotations present on all methods of the provided class
     */
    public static List<AnnotationNode> getMethodAnnotations(ClassNode classNode) {
        List<AnnotationNode> annotations = new ArrayList<>();
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.invisibleAnnotations != null) {
                annotations.addAll(methodNode.invisibleAnnotations);
            }
            if (methodNode.visibleAnnotations != null) {
                annotations.addAll(methodNode.visibleAnnotations);
            }
        }
        return annotations;
    }

    /**
     * Searches for {@link WeaveWithAnnotation#annotationClasses()} in a method's annotations.
     *
     * @param annotations annotations to search through.
     */
    public static Set<String> getMethodRequiredAnnotations(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return Collections.emptySet();
        }

        Set<String> result = new HashSet<>();
        for (AnnotationNode visibleAnnotation : annotations) {
            if (WEAVE_WITH_ANNOTATION_TYPE.getDescriptor().equals(visibleAnnotation.desc)) {
                List<Object> values = visibleAnnotation.values;
                for (int i = 0; i < values.size(); i += 2) {
                    if (values.get(i).equals(WeaveUtils.ANNOTATION_CLASSES_ATTRIBUTE_KEY)) {
                        // The value of this attribute is a String[] which ASM converts into an ArrayList<String>
                        result.addAll((List<String>) (values.get(i + 1)));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Searches for {@link WeaveWithAnnotation#type()} in a classes annotations to see if we have an Interface match type.
     *
     * @param classNode the class to search
     * @return true if a a {@link WeaveWithAnnotation} annotation is found on the class that uses an Interface match type.
     */
    public static boolean isWeaveWithAnnotationInterfaceMatch(ClassNode classNode) {
        List<AnnotationNode> classAnnotationNodes = WeaveUtils.getClassAnnotations(classNode);
        for (AnnotationNode classAnnotationNode : classAnnotationNodes) {
            if (WEAVE_WITH_ANNOTATION_TYPE.getDescriptor().equals(classAnnotationNode.desc)) {
                List<Object> values = classAnnotationNode.values;
                for (int i = 0; i < values.size(); i += 2) {
                    if (values.get(i).equals(WeaveUtils.ANNOTATION_MATCH_TYPE_ATTRIBUTE_KEY)) {
                        // The value of this attribute is a String
                        if (MatchType.Interface.name().equals(((String[]) values.get(i + 1))[1])) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Searches through the provided method's annotation to see if it uses the {@link WeaveIntoAllMethods} annotation.
     *
     * @param methodNode the method to look for annotations on
     * @return true if the provided method has a {@link WeaveIntoAllMethods} annotation, false otherwise
     */
    public static boolean hasWeaveIntoAllMethodsAnnotation(MethodNode methodNode) {
        List<AnnotationNode> methodAnnotations = WeaveUtils.getMethodAnnotations(methodNode);
        for (AnnotationNode methodAnnotation : methodAnnotations) {
            if (WEAVE_ALL_METHODS_TYPE.getDescriptor().equals(methodAnnotation.desc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if method has at least one of the provided required method annotations.
     *
     * @param originalMethod                     the method to check
     * @param requiredMethodAnnotationClassNames the Set of required annotation class names
     * @return true if method has at least one of the required method annotations, false otherwise.
     */
    public static boolean hasRequiredAnnotations(MethodNode originalMethod, Set<String> requiredMethodAnnotationClassNames) {
        List<AnnotationNode> methodAnnotations = WeaveUtils.getMethodAnnotations(originalMethod);

        Set<String> methodAnnotationClassNames = new HashSet<>();
        for (AnnotationNode methodAnnotation : methodAnnotations) {
            methodAnnotationClassNames.add(Type.getType(methodAnnotation.desc).getClassName());
        }

        return hasRequiredAnnotations(methodAnnotationClassNames, requiredMethodAnnotationClassNames);
    }

    public static boolean hasRequiredAnnotations(Set<String> methodAnnotationClassNames, Set<String> requiredMethodAnnotationClassNames) {
        // Remove all class names from "methodAnnotationClassNames" that aren't in "requiredMethodAnnotationClassNames"
        methodAnnotationClassNames.retainAll(requiredMethodAnnotationClassNames);

        // If "methodAnnotationClassNames" is not empty it means we have at least one of the required annotations
        return !methodAnnotationClassNames.isEmpty();
    }

    public static boolean isMethodWeNeverInstrument(MethodNode originalMethod) {
        return METHODS_WE_NEVER_INSTRUMENT.contains(new MethodKey(originalMethod.name, originalMethod.desc));
    }

}


