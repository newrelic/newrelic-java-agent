/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.TypeAnnotationNode;

import java.util.ArrayList;

/**
 * This class exists only to work around the fact that FieldNode instances are NOT thread safe. If multiple threads
 * call {@link #accept(ClassVisitor)} at the same time there is a race condition that exists that has been shown to
 * cause VerifyErrors and other serious bytecode-related exceptions.
 *
 * By synchronizing on the {@link #accept(ClassVisitor)} method we are ensuring that any usage of this FieldNode in the
 * agent can be accessed by multiple threads. One important thing to note is that this ties us pretty strongly to an ASM
 * version due to the fact that we had to override constructors in the original asm class. If these constructors change
 * or other accept() methods are added we will need to update accordingly.
 *
 * NOTE: If you are upgrading from org.ow2.asm:asm / org.ow2.asm:asm-tree make sure to double
 * check that this class hasn't changed between versions.
 */
public class SynchronizedFieldNode extends FieldNode {

    public SynchronizedFieldNode(int access, String name, String desc, String signature, Object value) {
        super(WeaveUtils.ASM_API_LEVEL, access, name, desc, signature, value);
    }

    public SynchronizedFieldNode(int api, int access, String name, String desc, String signature, Object value) {
        super(api, access, name, desc, signature, value);
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

    /**
     * This method is synchronized in order to prevent issues with bytecode modification that
     * occur when multiple threads call accept on the same FieldNode instance.
     */
    @Override
    public synchronized void accept(ClassVisitor cv) {
        super.accept(cv);
    }

}
