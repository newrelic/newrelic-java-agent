/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import java.io.IOException;

import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;

/**
 * Inheritance tests for ClassMatch
 */
public class ClassMatchInheritanceTest {

    @Test
    public void testWeaveAbstractMethodExactMatch() throws IOException {
        String internalName = Type.getInternalName(ExactWeave.class);
        WeaveTestUtils.expectViolations(ExactOriginal.class, ExactWeave.class, false, new WeaveViolation(
                WeaveViolationType.METHOD_EXACT_ABSTRACT_WEAVE, internalName, new Method("abstractMethod", "()V")));
    }

    public abstract static class ExactOriginal {
        public abstract void abstractMethod();
    }

    public static class ExactWeave {
        public void abstractMethod() {
            // can't weave an abstract method in an exact match because it doesn't have an implementation to weave into
        }
    }

    @Test
    public void testWeaveConcreteSuperclassMethod() throws IOException {
        String internalName = Type.getInternalName(SuperWeave.class);
        WeaveTestUtils.expectViolations(SuperOriginal.class, SuperWeave.class, true, new WeaveViolation(
                WeaveViolationType.METHOD_BASE_CONCRETE_WEAVE, internalName, new Method("concreteMethod", "()V")));
    }

    public abstract static class SuperParent {
        public abstract void abstractMethod();

        public void throwsMethod() {
            throw new RuntimeException();
        }

        public void concreteMethod() {
        }

        public void anotherConcreteMethod() {
        }
    }

    public abstract static class SuperOriginal extends SuperParent {
        public void anotherConcreteMethod() {
        }
    }

    public static class SuperWeave {
        public void abstractMethod() {
            // we can weave this because it's abstract in SuperParent and guaranteed to be implemented in subclasses
        }

        public void throwsMethod() {
            // we can weave this because it's impl in SuperParent can only throw, so is guaranteed to be implemented in
            // subclasses if appropriate
        }

        public void concreteMethod() {
            // we can't weave this because it's implemented in SuperParent and may not be implemented in concrete
            // subclasses
        }

        public void anotherConcreteMethod() {
            // we can weave this because it's implemented in SuperOriginal
        }
    }

    @Test
    public void testWeaveIndirectInterfaceMethodInterfaceMatch() throws IOException {
        String internalName = Type.getInternalName(IndirectInterfaceWeave.class);
        WeaveTestUtils.expectViolations(Direct.class, IndirectInterfaceWeave.class, true, new WeaveViolation(
                WeaveViolationType.METHOD_INDIRECT_INTERFACE_WEAVE, internalName, new Method("indirectMethod", "()V")));
    }

    public interface Indirect {
        void indirectMethod();
    }

    public interface Direct extends Indirect {
        void directMethod();
    }

    public static class IndirectInterfaceWeave {
        public void directMethod() {
        }

        public void indirectMethod() {
            // can't weave an indirect method because we don't know that it will exist in child classes at runtime
        }
    }

    @Test
    public void testWeaveIndirectInterfaceMethodBaseMatch() throws IOException {
        String internalName = Type.getInternalName(IndirectBaseWeave.class);
        WeaveTestUtils.expectViolations(Direct.class, IndirectBaseWeave.class, true, new WeaveViolation(
                WeaveViolationType.METHOD_INDIRECT_INTERFACE_WEAVE, internalName, new Method("indirectMethod", "()V")));
    }

    public static class IndirectImpl implements Indirect {
        public void indirectMethod() {
        }
    }

    public static class DirectImpl extends IndirectImpl implements Direct {
        public void directMethod() {
        }
    }

    public static class IndirectBaseWeave {
        public void directMethod() {
        }

        public void indirectMethod() {
            // can't weave an indirect method because we don't know that it will exist in child classes at runtime
            // DirectImpl is an example
        }
    }

}
