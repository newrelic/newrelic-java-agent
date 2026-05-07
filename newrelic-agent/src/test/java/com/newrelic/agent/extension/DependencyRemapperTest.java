/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.Caffeine;
import com.newrelic.agent.deps.caffeine2.com.github.benmanes.caffeine.cache.CacheLoader;
import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.agent.util.asm.Utils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.IOException;

public class DependencyRemapperTest {

    @Test
    public void testClass() throws BenignClassReadException, IOException {
        ClassReader cr = Utils.readClass(RewriteTest.class);

        DependencyRemapper remapper = ExtensionRewriter.REMAPPER;
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassRemapper(cw, remapper);

        cr.accept(cv, ClassReader.SKIP_FRAMES);

        Assert.assertFalse(remapper.getRemappings().isEmpty());
        Assert.assertEquals(remapper.getRemappings().toString(),
                "com/newrelic/agent/deps/org/objectweb/asm/MethodVisitor", remapper.getRemappings().get(
                        "org/objectweb/asm/MethodVisitor"));
        Assert.assertEquals(remapper.getRemappings().toString(),
                "com/newrelic/agent/deps/org/objectweb/asm/ClassVisitor", remapper.getRemappings().get(
                        "org/objectweb/asm/ClassVisitor"));
        Assert.assertEquals(remapper.getRemappings().toString(),
                "com/newrelic/agent/deps/com/google/common/collect/ImmutableSet", remapper.getRemappings().get(
                        "com/google/common/collect/ImmutableSet"));
    }

    @Test
    public void google() {

        DependencyRemapper remapper = ExtensionRewriter.REMAPPER;
        Assert.assertEquals(remapper.getRemappings().toString(),
                "com/newrelic/agent/deps/com/google/common/collect/ImmutableSet",
                remapper.mapType(Type.getInternalName(ImmutableSet.class)));
    }

    @Test
    public void asm() {

        DependencyRemapper remapper = ExtensionRewriter.REMAPPER;
        Assert.assertEquals(remapper.getRemappings().toString(),
                "com/newrelic/agent/deps/org/objectweb/asm/MethodVisitor",
                remapper.mapType(Type.getInternalName(MethodVisitor.class)));
    }

    private static final class RewriteTest extends ClassVisitor {

        public RewriteTest(int api) {
            super(api);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            // Make sure we pick up this Caffeine reference.
            Caffeine<Object, Object> newBuilder = Caffeine.newBuilder();
            // Make sure we pick up this Guava reference.
            ImmutableSet<Object> set = ImmutableSet.of();
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

    }
}
