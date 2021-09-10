/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.profile;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

public class MethodLineNumberMatcher {

    public static String getMethodDescription(Class<?> currentClass, String mMethodName, int lineNumber) {
        try {
            if (currentClass != null && mMethodName != null && lineNumber > 0) {
                ClassReader cr = getClassReader(currentClass);
                LineNumberClassVisitor cv = new LineNumberClassVisitor(mMethodName, lineNumber);
                cr.accept(cv, ClassReader.SKIP_FRAMES);
                return cv.getActualMethodDesc();
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINEST, "Unable to grab method info using line numbers", e);
        }
        return null;
    }

    private static ClassReader getClassReader(Class<?> currentClass) {
        ClassLoader loader = currentClass.getClassLoader() == null ? AgentBridge.getAgent().getClass().getClassLoader()
                : currentClass.getClassLoader();
        String resource = currentClass.getName().replace('.', '/') + ".class";

        InputStream is = null;
        ClassReader cr;
        try {
            is = loader.getResourceAsStream(resource);
            cr = new ClassReader(is);
        } catch (IOException e) {
            throw new RuntimeException("unable to access resource: " + resource, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                }
            }
        }
        return cr;
    }

    public static class LineNumberClassVisitor extends ClassVisitor {

        private final String methodName;
        private final int lineNumber;
        // this field set if we find the line number in a method
        private String actualMethodDesc;

        public LineNumberClassVisitor(ClassVisitor cv, String mName, int lNumber) {
            super(WeaveUtils.ASM_API_LEVEL, cv);
            methodName = mName;
            lineNumber = lNumber;
            actualMethodDesc = null;
        }

        public LineNumberClassVisitor(String mName, int lNumber) {
            super(WeaveUtils.ASM_API_LEVEL);
            methodName = mName;
            lineNumber = lNumber;
            actualMethodDesc = null;
        }

        @Override
        public MethodVisitor visitMethod(int access, final String pMethodName, final String methodDesc,
                String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, pMethodName, methodDesc, signature, exceptions);
            if (methodName.equals(pMethodName)) {
                mv = new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        super.visitLineNumber(line, start);
                        if (lineNumber == line) {
                            actualMethodDesc = methodDesc;
                        }
                    }
                };
            }
            return mv;
        }

        public boolean foundMethod() {
            return actualMethodDesc != null;
        }

        public String getActualMethodDesc() {
            return actualMethodDesc;
        }
    }

}
