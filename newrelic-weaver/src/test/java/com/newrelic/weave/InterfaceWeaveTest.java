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

import java.io.IOException;
import java.io.Serializable;

import org.junit.BeforeClass;
import org.junit.Test;

import com.newrelic.api.agent.weaver.Weaver;

/**
 * Tests weaving interfaces.
 */
public class InterfaceWeaveTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        ClassWeave implementationWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$Interface", "com.newrelic.weave.InterfaceWeaveTest$Weave",
                "com.newrelic.weave.InterfaceWeaveTest$Implementation");
        assertNotNull(implementationWeave.getMatch().getExtension());

        ClassWeave childWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$Interface", "com.newrelic.weave.InterfaceWeaveTest$Weave",
                "com.newrelic.weave.InterfaceWeaveTest$ChildImplementation");
        assertNotNull(childWeave.getMatch().getExtension());

        ClassWeave anotherWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$Interface", "com.newrelic.weave.InterfaceWeaveTest$Weave",
                "com.newrelic.weave.InterfaceWeaveTest$AnotherImplementation");
        assertNotNull(anotherWeave.getMatch().getExtension()); // returns extension

        ClassWeave itfWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestInterface",
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestWeave",
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestImplementation");
        assertNotNull(itfWeave.getMatch().getExtension());

        ClassWeave itfChildWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestInterface",
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestWeave",
                "com.newrelic.weave.InterfaceWeaveTest$ConstructorTestImplementationChild");
        assertNotNull(itfChildWeave.getMatch().getExtension());

        ClassWeave extendedWeave = WeaveTestUtils.weaveAndAddToContextClassloader(
                "com.newrelic.weave.InterfaceWeaveTest$ExtendedInterface",
                "com.newrelic.weave.InterfaceWeaveTest$ExtendedInterfaceWeave",
                "com.newrelic.weave.InterfaceWeaveTest$ExtendedInterfaceImplementation");
        assertNull(extendedWeave.getMatch().getExtension());
    }

    @Test
    public void test() {
        Implementation impl = new Implementation();

        // basic weaving
        assertEquals("Weave.interfaceMethod / Implementation.interfaceMethod", impl.interfaceMethod());

        // make sure new fields don't clash
        assertEquals("Weave.field / Implementation.field", impl.interfaceMethodUsesFields());

        impl.setImplementationField("Implementation.new");
        assertEquals("Weave.field / Implementation.new", impl.interfaceMethodUsesFields());

        impl.setWeaveField("Weave.new");
        assertEquals("Weave.new / Implementation.new", impl.interfaceMethodUsesFields());

        // make sure new methods don't clash
        assertEquals("Weave.privateMethod / Implementation.privateMethod",
                impl.interfaceMethodCallsImplementationMethods());
    }

    @Test
    public void testChild() {
        // make sure interface matching works correctly with subclasses
        ChildImplementation child = new ChildImplementation();

        // basic weaving
        assertEquals("Weave.interfaceMethod / ChildImplementation.interfaceMethod", child.interfaceMethod());

        // superclass weaving - note that the weaver weaves all methods in the inheritence chain
        // see InheritanceWeaveTest javadoc
        assertEquals("Weave.field / from super(Weave.field / Implementation.field)", child.interfaceMethodUsesFields());
    }

    @Test
    public void testAnother() {
        // run the same tests with a completely new implementation to make sure new fields/methods are working correctly
        AnotherImplementation another = new AnotherImplementation("AnotherImplementation.field");

        // basic weaving
        assertEquals("Weave.interfaceMethod / AnotherImplementation.interfaceMethod", another.interfaceMethod());

        // make sure new fields don't clash
        assertEquals("Weave.field / AnotherImplementation.field", another.interfaceMethodUsesFields());

        another.setImplementationField("AnotherImplementation.new");
        assertEquals("Weave.field / AnotherImplementation.new", another.interfaceMethodUsesFields());

        another.setWeaveField("Weave.new");
        assertEquals("Weave.new / AnotherImplementation.new", another.interfaceMethodUsesFields());

        // make sure new methods don't clash
        assertEquals("Weave.privateMethod / AnotherImplementation.privateMethod",
                another.interfaceMethodCallsImplementationMethods());
    }

    public interface Interface extends Serializable {
        String interfaceMethod();

        String interfaceMethodUsesFields();

        void setWeaveField(String val); // to test accessing weave field

        String interfaceMethodCallsImplementationMethods();
    }

    public static class Implementation implements Interface {

        private String field = "Implementation.field";

        public String interfaceMethod() {
            return "Implementation.interfaceMethod";
        }

        public String interfaceMethodUsesFields() {
            return field;
        }

        public String interfaceMethodCallsImplementationMethods() {
            return privateMethod();
        }

        private String privateMethod() {
            return "Implementation.privateMethod";
        }

        public void setImplementationField(String val) {
            field = val;
        }

        public void setWeaveField(String val) {
            return;
        }
    }

    public static class ChildImplementation extends Implementation {
        public String interfaceMethod() {
            return "ChildImplementation.interfaceMethod";
        }

        public String interfaceMethodUsesFields() {
            return "from super(" + super.interfaceMethodUsesFields() + ")";
        }
    }

    public static class AnotherImplementation implements Interface {

        private String field;

        public AnotherImplementation(String field) {
            this.field = field;
        }

        public String interfaceMethod() {
            return "AnotherImplementation.interfaceMethod";
        }

        public String interfaceMethodUsesFields() {
            return field;
        }

        public String interfaceMethodCallsImplementationMethods() {
            return privateMethod();
        }

        private String privateMethod() {
            return "AnotherImplementation.privateMethod";
        }

        public void setImplementationField(String val) {
            field = val;
        }

        public void setWeaveField(String val) {
            return;
        }
    }

    public static class Weave {
        private String field = "Weave.field";

        public String interfaceMethod() {
            return "Weave.interfaceMethod / " + Weaver.callOriginal();
        }

        public String interfaceMethodUsesFields() {
            return field + " / " + Weaver.callOriginal();
        }

        public String interfaceMethodCallsImplementationMethods() {
            return privateMethod() + " / " + Weaver.callOriginal();
        }

        private String privateMethod() {
            return "Weave.privateMethod";
        }

        private void setImplementationField(String val) {
        }

        public void setWeaveField(String val) {
            field = val;
        }
    }

    @Test
    public void testConstructors() {
        // default constructors from weave class match *all* constructors in target
        ConstructorTestImplementation implementation = new ConstructorTestImplementation(100);
        assertEquals(1, implementation.getNumCalls());

        implementation = new ConstructorTestImplementation("100");
        assertEquals(2, implementation.getNumCalls());

        ConstructorTestImplementationChild child = new ConstructorTestImplementationChild(100);
        assertEquals(2, child.getNumCalls());

        child = new ConstructorTestImplementationChild("100");
        assertEquals(3, child.getNumCalls());
    }

    public interface ConstructorTestInterface {
        int getNumCalls(); // this is here to allow access to ConstructorTestWeave.numCalls for test purposes
    }

    public class ConstructorTestImplementation {
        public ConstructorTestImplementation(int someInt) {
        }

        public ConstructorTestImplementation(String someString) {
            this(Integer.parseInt(someString));
        }

        public int getNumCalls() {
            return 0;
        }
    }

    public class ConstructorTestImplementationChild extends ConstructorTestImplementation {
        public ConstructorTestImplementationChild(int someInt) {
            super(someInt);
        }

        public ConstructorTestImplementationChild(String someString) {
            super(someString);
        }
    }

    public static class ConstructorTestWeave {
        private int numCalls;

        public ConstructorTestWeave() {
            this.numCalls++;
        }

        public int getNumCalls() {
            return numCalls;
        }
    }

    @Test
    public void testExtendedInterface() {
        ExtendedInterface extendedInterface = new ExtendedInterfaceImplementation();
        assertEquals("Weave.interfaceMethod / Implementation.interfaceMethod", extendedInterface.interfaceMethod());
        assertEquals("extendedInterfaceMethod weaved Weave.interfaceMethod / Implementation.interfaceMethod",
                extendedInterface.extendedInterfaceMethod());
    }

    public interface ExtendedInterface extends Interface {
        String extendedInterfaceMethod();
    }

    public static class ExtendedInterfaceImplementation extends Implementation implements ExtendedInterface {
        public String extendedInterfaceMethod() {
            return "extendedInterfaceMethod";
        }
    }

    public abstract static class ExtendedInterfaceWeave implements Interface, Serializable {
        public String extendedInterfaceMethod() {
            return Weaver.callOriginal() + " weaved " + interfaceMethod();
        }

        // we cannot weave this here because there is no way to guarantee it will be weaved -
        // because the implementation of interfaceMethod() is in Implementation.class which does not implement
        // ExtendedInterface, we can only guarantee that the methods defined in a ExtendedInterface can be weaved
        // this is in direct contradiction to weaving supertype methods, which we *can* support but only if the
        // inherited implementation is abstract (or only throws an exception)
        public abstract String interfaceMethod();
    }
}
