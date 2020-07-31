/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.io.IOException;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.newrelic.weave.utils.ClassCache;
import com.newrelic.weave.utils.ClassLoaderFinder;
import com.newrelic.weave.utils.WeaveUtils;

public class ReferenceTest {
    public final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    private static boolean containsReferences(Set<Reference> references, String... referenceNames) {
        for (String referenceName : referenceNames) {
            if (!containsReference(references, referenceName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsReference(Set<Reference> references, String referenceName) {
        for (Reference ref : references) {
            if (ref.className.equals(referenceName)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testRegularClass() throws IOException {
        ClassNode node = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                ABaseClass.class.getName(), classloader));
        Set<Reference> references = Reference.create(node);
        Assert.assertTrue(containsReferences(references, "com/newrelic/weave/weavepackage/ReferenceTest$AnInterface",
                "com/newrelic/weave/weavepackage/ReferenceTest$ASuperClass"));
    }

    @Test
    public void testInterface() throws IOException {
        ClassNode node = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                AnInterface.class.getName(), classloader));
        Set<Reference> references = Reference.create(node);
        Assert.assertTrue(containsReferences(references, "java/lang/Object",
                "com/newrelic/weave/weavepackage/ReferenceTest$AnotherInterface"));
    }

    @Test
    public void testHumanReadableFlags() {
        int flags = Opcodes.ACC_PUBLIC + Opcodes.ACC_PRIVATE + Opcodes.ACC_PROTECTED + Opcodes.ACC_STATIC
                + Opcodes.ACC_FINAL + Opcodes.ACC_SYNCHRONIZED + Opcodes.ACC_BRIDGE + Opcodes.ACC_NATIVE
                + Opcodes.ACC_INTERFACE + Opcodes.ACC_ABSTRACT + Opcodes.ACC_STRICT + Opcodes.ACC_SYNTHETIC
                + Opcodes.ACC_ANNOTATION + Opcodes.ACC_ENUM;
        Assert.assertEquals(
                "public,private,protected,static,final,(synchronized|super),(bridge|volatile),native,interface,abstract,strict,synthetic,annotation,enum",
                WeaveUtils.humanReadableAccessFlags(flags));
    }

    @Test
    public void testFinalModifiers() throws IOException {
        ClassNode baseNode = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                ABaseClass.class.getName(), classloader));
        ClassNode superNode = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                ASuperClass.class.getName(), classloader));
        Reference superReference = null;
        Set<Reference> references = Reference.create(baseNode);
        for (Reference ref : references) {
            if (ref.className.equals(superNode.name)) {
                if (null == superReference) {
                    superReference = ref;
                } else {
                    Assert.assertTrue(superReference.merge(ref));
                }
            }
        }
        Assert.assertNotNull("baseNode should have created super reference", superReference);
        ClassCache cache = new ClassCache(new ClassLoaderFinder(Thread.currentThread().getContextClassLoader()));
        Assert.assertEquals(0, superReference.validateClassNode(cache, superNode).size());
        superNode.access = superNode.access | Opcodes.ACC_FINAL; // now the super class is final
        // Reference check must prevent us from extending a final class at runtime
        Assert.assertEquals(1, superReference.validateClassNode(cache, superNode).size());
    }

    /////////////// supertype testing
    public static class ASuperClass {
    }

    public static interface AnotherInterface {
        public void foo();
    }

    public static interface AnInterface extends AnotherInterface {
        public void interfaceMethod();
    }

    public static class ABaseClass extends ASuperClass implements AnInterface {
        public void interfaceMethod() {
        }

        public void foo() {
        }
    }
    ///////////////
}