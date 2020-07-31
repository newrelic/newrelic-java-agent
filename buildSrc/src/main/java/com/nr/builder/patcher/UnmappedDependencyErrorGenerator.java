/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.builder.patcher;

import com.nr.builder.Patcher;
import com.nr.builder.PatcherViolationException;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This patcher always runs. However, since it's only used for generating errors,
 * it only verifies that:
 * <ol>
 *     <li>"include" prefixes <i>must</i> be relocated (they come from dependencies)</li>
 *     <li>"exclude" prefixes need not be relocated.</li>
 *     <li>Any other packages will throw errors.</li>
 * </ol>
 */
public class UnmappedDependencyErrorGenerator implements Patcher {
    private static final Set<String> allowedPackages = new HashSet<>();

    static {
        // this represents classes that are relocated and agent classes.
        allowedPackages.add("com/newrelic/");

        // these are packages that come from the JRE.
        allowedPackages.add("java/");
        allowedPackages.add("javax/");
        allowedPackages.add("org/xml/sax");
        allowedPackages.add("org/w3c");
        allowedPackages.add("com/sun");
        allowedPackages.add("sun/misc");
        allowedPackages.add("org/ietf");
        allowedPackages.add("sun/reflect");

        // this comes from was_public and gets actually drawn in from WebSphere.
        allowedPackages.add("com/ibm");
    }

    @Override
    public ClassVisitor getVerificationVisitor(ClassVisitor next, AtomicBoolean shouldTransform) {
        final Set<String> unmappedClasses = new TreeSet<>(Comparator.naturalOrder());

        final UnmappedDependencyAccumulator accumulator = new UnmappedDependencyAccumulator(unmappedClasses, allowedPackages);
        return new ClassRemapper(next, accumulator) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                accumulator.shouldProcess(name.startsWith("com/newrelic/"));
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                if (!unmappedClasses.isEmpty()) {
                    String builder = "Unmapped dependencies found in " + className
                            + "\n\t\t"
                            + String.join("\n\t\t", unmappedClasses);

                    throw new PatcherViolationException(builder);
                }

            }
        };
    }

    @Override
    public ClassVisitor getRewritingVisitor(ClassVisitor next) {
        return next;
    }

    public static class UnmappedDependencyAccumulator extends Remapper {
        private boolean process;
        private final Set<String> unmappedClassNames;
        private final Set<String> allowedPackages;

        UnmappedDependencyAccumulator(final Set<String> unmappedClassNames, final Set<String> allowedPackages) {
            this.unmappedClassNames = unmappedClassNames;
            this.allowedPackages = allowedPackages;
        }

        void shouldProcess(boolean value) {
            process = value;
        }

        @Override
        public String map(String key) {
            if (!process) {
                return key;
            }

            // exclude prefixes: Prefixes that _need not_ be relocated.
            if (allowedPackages.stream().anyMatch(key::startsWith)) {
                return key;
            }

            unmappedClassNames.add(key);
            return key;
        }

    }
}
