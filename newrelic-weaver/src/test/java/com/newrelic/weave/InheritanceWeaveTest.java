/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.WeaveUtils;

/**
 * Inheritance-related weave tests.
 */
public class InheritanceWeaveTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave parentWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$Parent",
                "com.newrelic.weave.InheritanceWeaveTest$WeaveParent", "com.newrelic.weave.InheritanceWeaveTest$Parent");
        assertNotNull(parentWeave.getMatch().getExtension());

        ClassWeave childWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$Parent",
                "com.newrelic.weave.InheritanceWeaveTest$WeaveParent", "com.newrelic.weave.InheritanceWeaveTest$Child");
        assertNotNull(childWeave.getMatch().getExtension());
        // Target classes may broaden the permissions of overridden methods.
        // We should weave them without changing the access.
        assertEquals("weaveparent", new Child().parentProtectedMethod());
        MethodNode childMethod = WeaveUtils.getMethodNode(childWeave.getComposite(), "parentProtectedMethod", "()Ljava/lang/String;");
        assertTrue("Child.parentProtectedMethod must be public.", (childMethod.access & Opcodes.ACC_PUBLIC) != 0);

        ClassWeave siblingWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$Parent",
                "com.newrelic.weave.InheritanceWeaveTest$WeaveParent",
                "com.newrelic.weave.InheritanceWeaveTest$Sibling");
        assertNotNull(siblingWeave.getMatch().getExtension());

        ClassWeave grandchildWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$Parent",
                "com.newrelic.weave.InheritanceWeaveTest$WeaveParent",
                "com.newrelic.weave.InheritanceWeaveTest$Grandchild");
        assertNotNull(grandchildWeave.getMatch().getExtension());

        // in a non-test enviornment, both SuperOriginal and SuperImpl would get weaved because they're both base matches of SuperOriginal
        // we're testing that in SuperWeave.originalMethod, so we weave both matches manually for this test
        ClassWeave superOriginal = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$SuperOriginal",
                "com.newrelic.weave.InheritanceWeaveTest$SuperWeave",
                "com.newrelic.weave.InheritanceWeaveTest$SuperOriginal", true);
        assertNull(superOriginal.getMatch().getExtension());

        ClassWeave superImpl = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InheritanceWeaveTest$SuperOriginal",
                "com.newrelic.weave.InheritanceWeaveTest$SuperWeave",
                "com.newrelic.weave.InheritanceWeaveTest$SuperImpl");
        assertNull(superImpl.getMatch().getExtension());
    }

    @Test
    public void testBaseClassWeave() throws IOException {
        Child child = new Child();
        Sibling sibling = new Sibling();

        // basic weaving of parent final method + new field in WeaveParent
        assertEquals("Parent.finalMethod field(weave):WeaveParent.field parentField:Parent.field", child.finalMethod());

        // WeaveParent and Child both contain "field" - assert WeaveParent doesn't overwrite Child
        assertEquals(
                "Parent.finalMethod field(weave):WeaveParent.field parentField:Parent.field field(child):Child.field",
                child.appendField());

        // WeaveParent and Child both contain "childMethod" - assert WeaveParent doesn't overwrite Child
        assertEquals("Child.childMethod", child.childMethod());

        // WeaveParent and Child both contain "childMethod" - assert Child doesn't overwrite WeaveParent
        assertEquals("WeaveParent.childMethod", child.useWeaveParentChildMethod());

        // weaving of child concreteMethod that does not call super's impl
        assertEquals("Child.concreteMethod and WeaveParent.field", child.concreteMethod());

        // weaving of sibling concreteMethod that *calls* super's impl
        assertEquals("Parent.concreteMethod and WeaveParent.field / Sibling.concreteMethod and WeaveParent.field",
                sibling.concreteMethod());

        // use abstract method from child in WeaveParent
        assertEquals("Child.abstractMethod / Child.abstractMethod", child.useAbstractMethod());

        // use abstract method from sibling in WeaveParent (which actually overrides useAbstractMethod() and calls
        // super())
        assertEquals(
                "Sibling.abstractMethod / Sibling.useAbstractMethod / Sibling.abstractMethod / Sibling.abstractMethod",
                sibling.useAbstractMethod());

        // make sure constructor in WeaveParent is called the right number of times
        assertEquals(2, child.getNumWeaveConstructorCalls());
        assertEquals(2, sibling.getNumWeaveConstructorCalls());

        // make sure calls to super() work in weird ways
        Grandchild grandchild = new Grandchild();
        assertEquals("Grandchild.abstractMethod / Grandchild.useAbstractMethod / Child.childMethod",
                grandchild.useAbstractMethod());
    }

    public abstract static class Parent {
        protected String parentField = "Parent.field";

        public final String finalMethod() {
            return "Parent.finalMethod";
        }

        public String concreteMethod() {
            return "Parent.concreteMethod";
        }

        public abstract String abstractMethod();

        public String useAbstractMethod() {
            return abstractMethod();
        }

        public String useWeaveParentChildMethod() {
            return null; // used to assert WeaveParent.childMethod and Child.childMethod don't interfere with each other
        }

        public int getNumWeaveConstructorCalls() {
            return -1; // used to assert # of constructor calls from WeaveParent
        }

        protected String parentProtectedMethod() {
            return "parent";
        }
    }

    public static class Child extends Parent {
        private String field = "Child.field";

        @Override
        public String concreteMethod() {
            return "Child.concreteMethod";
        }

        public String childMethod() {
            return "Child.childMethod";
        }

        public String appendField() {
            return finalMethod() + " field(child):" + field;
        }

        @Override
        public String abstractMethod() {
            return "Child.abstractMethod";
        }

        @Override
        public String parentProtectedMethod(){
            return "child";
        }
    }

    public static class Grandchild extends Child {
        @Override
        public String childMethod() {
            return "Grandchild.childMethod";
        }

        @Override
        public String abstractMethod() {
            return "Grandchild.abstractMethod";
        }

        @Override
        public String useAbstractMethod() {
            return "Grandchild.useAbstractMethod / " + super.childMethod();
        }
    }

    public static class Sibling extends Parent {
        @Override
        public String concreteMethod() {
            return super.concreteMethod() + " / Sibling.concreteMethod";
        }

        @Override
        public String abstractMethod() {
            return "Sibling.abstractMethod";
        }

        @Override
        public String useAbstractMethod() {
            return "Sibling.useAbstractMethod / " + super.useAbstractMethod();
        }
    }

    public abstract static class WeaveParent {
        protected String parentField;
        private int numWeaveConstructorCalls;
        private String field = "WeaveParent.field";

        public WeaveParent() {
            numWeaveConstructorCalls++;
        }

        public final String finalMethod() {
            return Weaver.callOriginal() + " field(weave):" + field + " parentField:" + parentField;
        }

        public String concreteMethod() {
            return Weaver.callOriginal() + " and " + field;
        }

        private String childMethod() {
            return "WeaveParent.childMethod"; // this is a new method and should not interfere with Child.childMethod
        }

        public String useWeaveParentChildMethod() {
            return childMethod();
        }

        public abstract String abstractMethod();

        public String useAbstractMethod() {
            return abstractMethod() + " / " + Weaver.callOriginal();
        }

        public int getNumWeaveConstructorCalls() {
            return numWeaveConstructorCalls;
        }

        protected String parentProtectedMethod(){
            return "weaveparent";
        }
    }

    @Test
    public void testWeaveSuperMethods() throws IOException {
        SuperImpl impl = new SuperImpl();
        assertEquals("weaved abstractMethod", impl.abstractMethod());
        assertEquals("weaved throwsMethod", impl.throwsMethod());
        assertEquals("weaved originalMethod and called concreteMethod", impl.originalMethod());
    }

    public abstract static class SuperParent {
        public abstract String abstractMethod();

        public String throwsMethod() {
            throw new RuntimeException();
        }

        public String concreteMethod() {
            // this can't be weaved in a base match on SuperMethodOriginal, however it can be called by making the weave
            // class extend SuperMethodParent and declaring it abstract
            return "concreteMethod";
        }
    }

    public abstract static class SuperOriginal extends SuperParent {
        public String originalMethod() {
            return "originalMethod";
        }
    }

    public static class SuperImpl extends SuperOriginal {
        public String abstractMethod() {
            return "abstractMethod";
        }

        public String throwsMethod() {
            return "throwsMethod";
        }
    }

    public abstract static class SuperWeave extends SuperParent {
        public String abstractMethod() {
            return "weaved " + Weaver.callOriginal();
        }

        public String throwsMethod() {
            return "weaved " + Weaver.callOriginal();
        }

        public abstract String concreteMethod();

        public String originalMethod() {
            return "weaved " + Weaver.callOriginal() + " and called " + concreteMethod();
        }
    }
}
