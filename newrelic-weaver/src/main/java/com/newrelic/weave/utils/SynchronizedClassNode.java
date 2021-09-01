/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;

/**
 * This class exists only to work around the fact that ClassNode instances are NOT thread safe. If multiple threads call
 * {@link #accept(ClassVisitor)} at the same time there is a race condition that exists that has been shown to cause
 * VerifyErrors and other serious bytecode-related exceptions.
 *
 * By synchronizing on the {@link #accept(ClassVisitor)} method we are ensuring that any usage of this ClassNode in the
 * agent can be accessed by multiple threads. One important thing to note is that this ties us pretty strongly to an
 * ASM version due to the fact that we had to copy/paste/modify a few of the methods in this class in order to generate
 * thread-safe versions of other *Node classes such as MethodNode and FieldNode.
 *
 * NOTE: If you are upgrading from org.ow2.asm:asm / org.ow2.asm:asm-tree make sure to double
 * check that this class hasn't changed between versions.
 */
public class SynchronizedClassNode extends ClassNode {

    public SynchronizedClassNode(int asmApiLevel) {
        super(asmApiLevel);
    }

    public SynchronizedClassNode() {
        super(WeaveUtils.ASM_API_LEVEL);
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
        module = new SynchronizedModuleNode(name, access, version);
        return module;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        AnnotationNode an = new SynchronizedAnnotationNode(desc);
        if (visible) {
            if (visibleAnnotations == null) {
                visibleAnnotations = new ArrayList<>(1);
            }
            visibleAnnotations.add(an);
        } else {
            if (invisibleAnnotations == null) {
                invisibleAnnotations = new ArrayList<>(1);
            }
            invisibleAnnotations.add(an);
        }
        return an;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
        TypeAnnotationNode an = new SynchronizedTypeAnnotationNode(typeRef, typePath, desc);
        if (visible) {
            if (visibleTypeAnnotations == null) {
                visibleTypeAnnotations = new ArrayList<>(1);
            }
            visibleTypeAnnotations.add(an);
        } else {
            if (invisibleTypeAnnotations == null) {
                invisibleTypeAnnotations = new ArrayList<>(1);
            }
            invisibleTypeAnnotations.add(an);
        }
        return an;
    }

    @Override
    public void visitInnerClass(final String name, final String outerName, final String innerName, final int access) {
        InnerClassNode icn = new SynchronizedInnerClassNode(name, outerName, innerName, access);
        innerClasses.add(icn);
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature,
            final Object value) {
        FieldNode fn = new SynchronizedFieldNode(access, name, desc, signature, value);
        fields.add(fn);
        return fn;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature,
            final String[] exceptions) {
        MethodNode mn = new SynchronizedMethodNode(access, name, desc, signature, exceptions);
        methods.add(mn);
        return mn;
    }

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same ClassNode instance.
     */
    @Override
    public synchronized void accept(final ClassVisitor cv) {
        super.accept(cv);
    }

}
