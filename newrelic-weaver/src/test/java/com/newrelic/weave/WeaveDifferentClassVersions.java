/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import java.util.Collections;

import org.apache.struts.config.ForwardConfig;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolationType;

/**
 * Tests for how we handle class files which have a different version than the weave class file.
 */
public class WeaveDifferentClassVersions {
    final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    @Test
    public void weaveJavaTooOld() throws Exception {
        // this is a java 1.4 class
        final String classname = "org.apache.struts.config.ForwardConfig";
        byte[] bytes = WeaveUtils.getClassBytesFromClassLoaderResource(classname, classloader);
        Assert.assertNotNull(bytes);
        ClassNode target = WeaveUtils.convertToClassNode(bytes);
        Assert.assertTrue(target.version < 49);

        ClassWeave weave = WeaveTestUtils.weaveAndAddToContextClassloader(classname, WeaveClass.class.getName());
        ForwardConfig wovenInstance = new ForwardConfig();
        Assert.assertEquals(wovenInstance.getName(), new WeaveClass().getName());
        Assert.assertEquals(WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION, weave.getComposite().version);
    }

    @Test
    public void weaveJavaTooNew() throws Exception {
        byte[] bytes = WeaveUtils.getClassBytesFromClassLoaderResource(OriginalClass.class.getName(), classloader);
        Assert.assertNotNull(bytes);
        ClassNode originalNode = WeaveUtils.convertToClassNode(bytes);

        ClassNode weaveNode = WeaveUtils.convertToClassNode(WeaveTestUtils.getClassBytes(WeaveClass.class.getName()));
        weaveNode.version = 999;
        Assert.assertTrue(weaveNode.version > WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION);

        ClassMatch match = ClassMatch.match(originalNode, weaveNode, false, Collections.<String>emptySet(),
                Collections.<String>emptySet(), WeaveTestUtils.createContextCache());
        Assert.assertEquals(1, match.getViolations().size());
        Assert.assertTrue(match.getViolations().iterator().next().getType() == WeaveViolationType.INCOMPATIBLE_BYTECODE_VERSION);
    }

    public static class WeaveClass {
        public String getName() {
            // this isn't possible in 1.4 bytecode
            Class<?> clazz = ClassLoader.class;
            clazz.getClassLoader();

            return "weaved";
        }
    }

    public static class OriginalClass {
        public String getName() {
            return "original";
        }
    }
}
