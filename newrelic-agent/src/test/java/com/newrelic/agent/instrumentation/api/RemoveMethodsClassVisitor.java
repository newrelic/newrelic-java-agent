/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.api;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.util.HashSet;
import java.util.Set;

/**
 * Visit a class to remove methods.
 */
public class RemoveMethodsClassVisitor extends ClassVisitor {

    private final Set<Method> methodsToRemove;

    /**
     * This class removes methods declared in the class.
     *
     * @param cv the class visitor delegate
     * @param methodsToRemove the methods to remove
     */
    public RemoveMethodsClassVisitor(ClassVisitor cv, Set<Method> methodsToRemove) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
        this.methodsToRemove = new HashSet<>(methodsToRemove);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        if (methodsToRemove.remove(new Method(name, desc))) {
            return null;
        }
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

}
