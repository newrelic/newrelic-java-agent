/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.util.regex.Pattern;

public class GeneratedClassDetector implements ClassMatchVisitorFactory {

    private static final Pattern PROXY_REGEX = Pattern.compile("(.*)proxy(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CGLIB_REGEX = Pattern.compile("(.*)cglib(.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERATED_REGEX = Pattern.compile("(.*)generated(.*)", Pattern.CASE_INSENSITIVE);

    /**
     * Return true if the class name is likely the name of generated class. Package private for testability.
     */
    static boolean isGenerated(String className) {

        if (className == null) {
            return false;
        }

        if (className.contains("$$")) {
            return true;
        }

        return (GENERATED_REGEX.matcher(className).find() || CGLIB_REGEX.matcher(className).find() ||
                PROXY_REGEX.matcher(className).find());
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined, final ClassReader reader,
            ClassVisitor cv, final InstrumentationContext context) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public void visitSource(String source, String debug) {
                super.visitSource(source, debug);
                context.setSourceAttribute(true);
                if ("<generated>".equals(source)) {
                    context.setGenerated(true);
                }
            }

            /**
             * Some generated code doesn't set the source, so let's try to detect that. (The ClassReader doesn't even
             * call visitSource() if the source attribute isn't defined in the class.) We believe that source is also
             * missing when code is obfuscated, and because of that we do these name checks.
             */
            @Override
            public void visitEnd() {
                super.visitEnd();
                if (!context.hasSourceAttribute()) {
                    String className = reader.getClassName();

                    if (isGenerated(className)) {
                        context.setGenerated(true);
                    }
                }
            }
        };
    }

}
