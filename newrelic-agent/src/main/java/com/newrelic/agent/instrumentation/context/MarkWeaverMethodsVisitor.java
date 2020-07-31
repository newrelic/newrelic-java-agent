/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.instrumentation.WeavedMethod;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.Collection;

public class MarkWeaverMethodsVisitor extends ClassVisitor {
    private final InstrumentationContext context;

    public MarkWeaverMethodsVisitor(ClassVisitor cv, InstrumentationContext context) {
        super(WeaveUtils.ASM_API_LEVEL, cv);
        this.context = context;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        Collection<String> instrumentationTitles = context.getMergeInstrumentationPackages(new Method(name, desc));
        if (instrumentationTitles != null && !instrumentationTitles.isEmpty()) {
            AnnotationVisitor weavedAnnotation = mv.visitAnnotation(Type.getDescriptor(WeavedMethod.class), true);
            AnnotationVisitor visitArray = weavedAnnotation.visitArray("source");
            for (String title : instrumentationTitles) {
                visitArray.visit("", title);
            }
            visitArray.visitEnd();
            weavedAnnotation.visitEnd();
        }

        return mv;
    }

}
