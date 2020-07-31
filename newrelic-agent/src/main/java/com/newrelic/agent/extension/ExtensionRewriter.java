/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.util.asm.PatchedClassWriter;
import com.newrelic.weave.utils.Streams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class ExtensionRewriter {

    static final DependencyRemapper REMAPPER = new DependencyRemapper(ImmutableSet.of("org/objectweb/asm/",
            "com/google/", "org/apache/commons/"));

    private ExtensionRewriter() {
    }

    /**
     * Given a jar file, return the bytes for a new jar in which all classes have been modified so that references to
     * dependencies that we repackage in the agent are rewritten to reference the repackaged classes. If no such
     * references are found, a null is returned.
     *
     * @return the bytes for a new jar file, or null if no new jar is needed
     */
    public static byte[] rewrite(JarFile jar, ClassLoader classLoader) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        JarOutputStream jarOut = new JarOutputStream(out);
        boolean modified = false;

        try {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                JarEntry entry = e.nextElement();

                JarEntry newEntry = new JarEntry(entry.getName());

                InputStream inputStream = jar.getInputStream(entry);
                try {
                    if (entry.getName().endsWith(".class")) {
                        ClassReader cr = new ClassReader(inputStream);
                        ClassWriter writer = new PatchedClassWriter(ClassWriter.COMPUTE_FRAMES, classLoader);

                        ClassVisitor cv = new ClassRemapper(writer, REMAPPER);

                        cr.accept(cv, ClassReader.SKIP_FRAMES);

                        if (!REMAPPER.getRemappings().isEmpty()) {
                            modified = true;
                        }

                        inputStream.close();
                        inputStream = new ByteArrayInputStream(writer.toByteArray());
                    }
                    jarOut.putNextEntry(newEntry);
                    Streams.copy(inputStream, jarOut, inputStream.available());
                } finally {
                    jarOut.closeEntry();
                    inputStream.close();
                }
            }
        } finally {
            jarOut.close();
            jar.close();
            out.close();
        }

        return modified ? out.toByteArray() : null;
    }
}
