/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.google.common.collect.ObjectArrays;
import com.newrelic.api.agent.weaver.WeaveAllConstructors;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.EOFException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import static com.newrelic.weave.violation.WeaveViolationType.CLASS_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_EXTENDS_ILLEGAL_SUPERCLASS;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_IMPLEMENTS_ILLEGAL_INTERFACE;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_NESTED_NONSTATIC_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLASS_WEAVE_IS_INTERFACE;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_FIELD_ACCESS_VIOLATION;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.CLINIT_METHOD_ACCESS_VIOLATION;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_FINAL_ASSIGNMENT;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_FINAL_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_PRIVATE_BASE_CLASS_MATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_SERIALVERSIONUID_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_STATIC_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.FIELD_TYPE_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_ILLEGAL_CALL_ORIGINAL;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_NEW_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED;
import static com.newrelic.weave.violation.WeaveViolationType.INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_ACCESS_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NATIVE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_INVOKE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_NEW_NON_PRIVATE_UNSUPPORTED;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_RETURNTYPE_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_STATIC_MISMATCH;
import static com.newrelic.weave.violation.WeaveViolationType.METHOD_THROWS_MISMATCH;

/**
 * Tests for ClassMatch.
 */
public class ClassMatchTest {

    /**
     * Test field-related mismatches.
     */
    @Test
    public void testFieldMismatches() throws IOException {
        String internalName = Type.getInternalName(FieldMismatchWeave.class);
        WeaveViolation[] expected = { new WeaveViolation(FIELD_TYPE_MISMATCH, internalName, "stringOriginalIntWeave"),
                new WeaveViolation(FIELD_FINAL_MISMATCH, internalName, "finalOriginalNonfinalWeave"),
                new WeaveViolation(FIELD_FINAL_ASSIGNMENT, internalName, "finalOriginalFinalWeaveNoCallOriginal"),
                new WeaveViolation(FIELD_STATIC_MISMATCH, internalName, "staticOriginalMemberWeave"),
                new WeaveViolation(FIELD_ACCESS_MISMATCH, internalName, "privateOriginalPublicWeave"),
                new WeaveViolation(FIELD_ACCESS_MISMATCH, internalName, "protectedOriginalPublicWeave"),
                new WeaveViolation(FIELD_ACCESS_MISMATCH, internalName, "packageOriginalPublicWeave"),
                new WeaveViolation(FIELD_SERIALVERSIONUID_UNSUPPORTED, internalName, "serialVersionUID"), };
        WeaveTestUtils.expectViolations(FieldMismatchOriginal.class, FieldMismatchWeave.class, false, expected);

        // base class matches should not access private members in original class
        expected = ObjectArrays.concat(new WeaveViolation(FIELD_PRIVATE_BASE_CLASS_MATCH, internalName,
                "privateOriginalPublicWeave"), expected);
        WeaveTestUtils.expectViolations(FieldMismatchOriginal.class, FieldMismatchWeave.class, true, expected);
    }

    public static class FieldMismatchOriginal {
        // type mismatch
        String stringOriginalIntWeave;
        String stringOriginalStringWeave;

        // static mismatch
        static int staticOriginalMemberWeave;
        static int staticOriginalStaticWeave;

        // final mismatch
        final int finalOriginalNonfinalWeave = 1;
        final int finalOriginalFinalWeaveCallOriginal = 1;
        final int finalOriginalFinalWeaveNoCallOriginal = 1;

        // access mismatch
        private int privateOriginalPublicWeave;
        protected int protectedOriginalPublicWeave;
        int packageOriginalPublicWeave;
        public int publicOriginalPrivateWeave;
        public int publicOriginalPublicWeave;
    }

    public static class FieldMismatchWeave {
        // type mismatch
        int stringOriginalIntWeave;
        String stringOriginalStringWeave;

        // static mismatch
        int staticOriginalMemberWeave;
        static int staticOriginalStaticWeave;

        // final mismatch
        int finalOriginalNonfinalWeave = 1;
        final int finalOriginalFinalWeaveCallOriginal = Weaver.callOriginal();
        final int finalOriginalFinalWeaveNoCallOriginal = 1;

        // access mismatch
        public int privateOriginalPublicWeave;
        public int protectedOriginalPublicWeave;
        public int packageOriginalPublicWeave;
        private int publicOriginalPrivateWeave;
        public int publicOriginalPublicWeave;

        // cannot define serialVersionUID
        public static long serialVersionUID;
    }

    /**
     * Test method mismatches.
     */
    @Test
    public void testMethodMismatches() throws IOException {
        String internalName = Type.getInternalName(MethodAccessWeave.class);
        WeaveViolation[] expected = {
                new WeaveViolation(METHOD_NATIVE_UNSUPPORTED, internalName, new Method("nativeOriginal", "()V")),
                new WeaveViolation(METHOD_NATIVE_UNSUPPORTED, internalName, new Method("nativeWeave", "()V")),
                new WeaveViolation(METHOD_STATIC_MISMATCH, internalName, new Method("staticOriginalMemberWeave", "()V")),
                new WeaveViolation(METHOD_ACCESS_MISMATCH, internalName,
                        new Method("privateOriginalPublicWeave", "()V")),
                new WeaveViolation(METHOD_ACCESS_MISMATCH, internalName, new Method("protectedOriginalPublicWeave",
                        "()V")),
                new WeaveViolation(METHOD_ACCESS_MISMATCH, internalName,
                        new Method("packageOriginalPublicWeave", "()V")),
                new WeaveViolation(METHOD_ACCESS_MISMATCH, internalName,
                        new Method("publicOriginalPrivateWeave", "()V")),
                new WeaveViolation(METHOD_RETURNTYPE_MISMATCH, internalName, new Method("returnsInt", "()J")),
                new WeaveViolation(METHOD_THROWS_MISMATCH, internalName, new Method("staticThrowsWeave", "()V")),
                new WeaveViolation(METHOD_THROWS_MISMATCH, internalName, new Method("privateThrowsWeave", "()V")),
                new WeaveViolation(METHOD_THROWS_MISMATCH, internalName, new Method("protectedThrowsWeave", "()V")),
                new WeaveViolation(METHOD_THROWS_MISMATCH, internalName, new Method("publicThrowsWeave", "()I")),
                new WeaveViolation(METHOD_THROWS_MISMATCH, internalName, new Method("publicNoThrowsWeave", "()V")) };
        WeaveTestUtils.expectViolations(MethodAccessOriginal.class, MethodAccessWeave.class, true, expected);
    }

    public abstract static class MethodAccessOriginal {
        // native unsupported
        native void nativeOriginal();

        abstract void nativeWeave();

        native void unweavedNative(); // this is OK

        // static mismatch
        static void staticOriginalMemberWeave() {
        }

        static void staticOriginalStaticWeave() {
        }

        // access mismatch
        private void privateOriginalPublicWeave() {
        }

        protected void protectedOriginalPublicWeave() {
        }

        void packageOriginalPublicWeave() {
        }

        public void publicOriginalPrivateWeave() {
        }

        public void publicOriginalPublicWeave() {
        }

        // return type mismatch
        public int returnsInt() {
            return 1;
        }

        // throws mismatch
        static void staticThrowsWeave() throws IOException {
        }

        private void privateThrowsWeave() throws CustomRuntimeException {
        }

        protected void protectedThrowsWeave() throws IOException, NumberFormatException {
        }

        public int publicThrowsWeave() throws Exception {
            return 100;
        }

        public void publicNoThrowsWeave() {
        }

        public void publicCorrectThrowsWeave() throws InterruptedException {
        }

        public void publicThrowsSubsetWeave() throws InterruptedException, IOException {
        }
    }

    private class CustomRuntimeException extends RuntimeException {

    }

    public abstract static class MethodAccessWeave {
        // native unsupported
        abstract void nativeOriginal();

        native void nativeWeave();

        // static mismatch
        void staticOriginalMemberWeave() {
        }

        static void staticOriginalStaticWeave() {
        }

        // access mismatch
        public void privateOriginalPublicWeave() {
        }

        public void protectedOriginalPublicWeave() {
        }

        public void packageOriginalPublicWeave() {
        }

        private void publicOriginalPrivateWeave() {
        }

        public void publicOriginalPublicWeave() {
        }

        // return type mismatch
        public long returnsInt() {
            return 1;
        }

        // throws mismatch
        static void staticThrowsWeave() throws IllegalArgumentException {
        }

        private void privateThrowsWeave() throws IOException, CustomRuntimeException {
        }

        protected void protectedThrowsWeave() throws EOFException {
        }

        public int publicThrowsWeave() throws RuntimeException {
            return 100;
        }

        public void publicNoThrowsWeave() throws OutOfMemoryError {
        }

        public void publicCorrectThrowsWeave() throws InterruptedException {
        }

        public void publicThrowsSubsetWeave() throws InterruptedException {
        }

    }

    /**
     * Test that nested weaved interfaces don't throw an NPE.
     */
    @Test
    public void testNestedInterface() throws IOException {
        WeaveTestUtils.expectViolations(ChainImpl.class, Interceptor.class, false, new WeaveViolation(
                CLASS_WEAVE_IS_INTERFACE, Type.getInternalName(Interceptor.class)));
        WeaveTestUtils.expectViolations(ChainImpl.class, Interceptor.Chain.class, false, new WeaveViolation(
                CLASS_WEAVE_IS_INTERFACE, Type.getInternalName(Interceptor.Chain.class)));
    }

    public interface Interceptor {
        String intercept(Chain chain);

        interface Chain {
            String request();
        }
    }

    public static class ChainImpl implements Interceptor.Chain {
        private String value;

        public ChainImpl(String value) {
            this.value = value;
        }

        public String request() {
            return value;
        }
    }

    /**
     * Test class-level matching, e.g. class modifiers, superclasses, inheritance, etc.
     */
    @Test
    public void testClassLevelMatching() throws IOException {
        // successful matches
        WeaveTestUtils.expectViolations(PublicClass.class, PublicClass.class, false);
        WeaveTestUtils.expectViolations(ProtectedClass.class, ProtectedClass.class, false);
        WeaveTestUtils.expectViolations(PackageClass.class, PackageClass.class, false);
        WeaveTestUtils.expectViolations(PrivateClass.class, PrivateClass.class, false);

        // we don't (yet) check protected/private modifiers on inner classes
        // top-level class defs are always either public or package-private
        WeaveTestUtils.expectViolations(PublicClass.class, ProtectedClass.class, false);
        WeaveTestUtils.expectViolations(PackageClass.class, PrivateClass.class, false);

        // access mismatches
        String packageClassName = Type.getInternalName(PackageClass.class);
        String privateClassName = Type.getInternalName(PrivateClass.class);

        WeaveTestUtils.expectViolations(PublicClass.class, PackageClass.class, false, new WeaveViolation(
                CLASS_ACCESS_MISMATCH, packageClassName));

        WeaveTestUtils.expectViolations(PublicClass.class, PrivateClass.class, false, new WeaveViolation(
                CLASS_ACCESS_MISMATCH, privateClassName));

        WeaveTestUtils.expectViolations(ProtectedClass.class, PrivateClass.class, false, new WeaveViolation(
                CLASS_ACCESS_MISMATCH, privateClassName));

        WeaveTestUtils.expectViolations(ProtectedClass.class, PackageClass.class, false, new WeaveViolation(
                CLASS_ACCESS_MISMATCH, packageClassName));

        // weave classes must extend java.lang.Object or the original's superclass
        WeaveTestUtils.expectViolations(PropertiesClass.class, ChildClass.class, false);

        WeaveTestUtils.expectViolations(PublicClass.class, ChildClass.class, false, new WeaveViolation(
                CLASS_EXTENDS_ILLEGAL_SUPERCLASS, Type.getInternalName(ChildClass.class)));

        // weave classes can only implement interfaces that the original implements
        WeaveTestUtils.expectViolations(SerializableClass.class, ImplementsClass.class, false);

        WeaveTestUtils.expectViolations(PublicClass.class, ImplementsClass.class, false, new WeaveViolation(
                CLASS_IMPLEMENTS_ILLEGAL_INTERFACE, Type.getInternalName(ImplementsClass.class)));

        // weave classes cannot be non-static inner classes
        String nonStaticInnerClassName = Type.getInternalName(NonStaticInnerClass.class);
        WeaveTestUtils.expectViolations(PublicClass.class, NonStaticInnerClass.class, false, new WeaveViolation(
                CLASS_NESTED_NONSTATIC_UNSUPPORTED, nonStaticInnerClassName), new WeaveViolation(
                INIT_NEW_UNSUPPORTED, nonStaticInnerClassName, new Method(WeaveUtils.INIT_NAME,
                "(Lcom/newrelic/weave/ClassMatchTest;)V")));

        // weave class cannot be an interface
        WeaveTestUtils.expectViolations(PropertiesClass.class, InterfaceWeaveIsIllegal.class, false,
                new WeaveViolation(WeaveViolationType.CLASS_WEAVE_IS_INTERFACE,
                        Type.getInternalName(InterfaceWeaveIsIllegal.class)));
    }

    public static class PublicClass {
        private PublicClass() {
        }
    }

    protected static class ProtectedClass {
        private ProtectedClass() {
        }
    }

    static class PackageClass {
        private PackageClass() {
        }
    }

    private static class PrivateClass {
        private PrivateClass() {
        }
    }

    public static class PropertiesClass extends java.util.Properties {

    }

    public static class ChildClass extends java.util.Properties {
        private ChildClass() {
        }
    }

    public static class SerializableClass implements Serializable {

    }

    public static class ImplementsClass implements Serializable {
        private ImplementsClass() {
        }
    }

    public class NonStaticInnerClass {
        private NonStaticInnerClass() {
        }
    }

    public interface InterfaceWeaveIsIllegal {
    }

    /**
     * Test new field validations
     *
     * @throws IOException
     */
    @Test
    public void testNewFieldValidations() throws IOException {
        String internalName = Type.getInternalName(NewFieldsWeave.class);
        WeaveViolation[] expected = {};
        WeaveTestUtils.expectViolations(NewFieldsOriginal.class, NewFieldsWeave.class, false, expected);
    }

    public static class NewFieldsOriginal {
        private static int matchedField;
    }

    public static class NewFieldsWeave {
        public int newPublicField; // new public fields not allowed at this time
    }

    /**
     * Test new method validations
     */
    @Test
    public void testNewMethodValidations() throws IOException {
        String internalName = Type.getInternalName(NewMethodsWeave.class);
        WeaveViolation[] expected = {
                new WeaveViolation(INIT_NEW_UNSUPPORTED, internalName, new Method(WeaveUtils.INIT_NAME, "(I)V")),
                new WeaveViolation(METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED, internalName, new Method(
                        "newMethodCallsOriginal", "()V")),
                new WeaveViolation(METHOD_NEW_NON_PRIVATE_UNSUPPORTED, internalName, new Method(
                        "invalidNewPublicMethod", "()V")),
                new WeaveViolation(METHOD_NEW_INVOKE_UNSUPPORTED, internalName, new Method("newMethodCallsNewMethod",
                        "()V")), };
        WeaveTestUtils.expectViolations(NewMethodsOriginal.class, NewMethodsWeave.class, false, expected);
    }

    public static class NewMethodsOriginal {
    }

    public static class NewMethodsWeave {
        private NewMethodsWeave(int someInt) {
            // cannot add new constructors
        }

        private void validNewMethod() {
        }

        public void invalidNewPublicMethod() {
            // new methods must be private
        }

        private void newMethodCallsOriginal() {
            Weaver.callOriginal(); // new methods cannot call original
        }

        private void newMethodCallsNewMethod() {
            validNewMethod(); // new methods cannot call each other
        }
    }

    /**
     * Test constructor validations
     */
    @Test
    public void testConstructorValidations() throws IOException {
        String internalName = Type.getInternalName(ConstructorWeave.class);
        WeaveViolation[] expected = {
                new WeaveViolation(INIT_NEW_UNSUPPORTED, internalName, new Method(WeaveUtils.INIT_NAME, "(I)V")),
                new WeaveViolation(METHOD_NEW_NON_PRIVATE_UNSUPPORTED, internalName, new Method(WeaveUtils.INIT_NAME,
                        "(I)V")),
                new WeaveViolation(INIT_ILLEGAL_CALL_ORIGINAL, internalName, new Method(WeaveUtils.INIT_NAME, "()V")), };
        WeaveTestUtils.expectViolations(CosntructorOriginal.class, ConstructorWeave.class, false, expected);

        // interface matches only allow default constructor
        expected = ObjectArrays.concat(new WeaveViolation(INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED, internalName,
                new Method(WeaveUtils.INIT_NAME, "(I)V")), expected);
        WeaveTestUtils.expectViolations(CosntructorInterface.class, ConstructorWeave.class, false, expected);
    }

    public static class CosntructorOriginal {
    }

    public static class ConstructorWeave {
        public ConstructorWeave() {
            this(4); // cannot call other constructors from weave
            Weaver.callOriginal(); // cannot call original from weave constructor
        }

        public ConstructorWeave(int someInt) {
            // cannot add new constructors to weave
        }
    }

    public interface CosntructorInterface {
    }

    /**
     * Test that call original is only invoked at most once in weaved methods
     */
    @Test
    public void testWeaveCallOriginalNoMoreThanOnce() throws IOException {
        String internalName = Type.getInternalName(TestCallOriginalWeave.class);
        WeaveTestUtils.expectViolations(TestCallOriginalClass.class, TestCallOriginalWeave.class, false,
                new WeaveViolation(METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE, internalName, new Method("twice", "()V")),
                new WeaveViolation(METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE, internalName, new Method("phantomTwice",
                        "()V")));
    }

    public static class TestCallOriginalClass {
        public TestCallOriginalClass(boolean callFirst) {
        }

        public void none() {
        }

        public void once() {
        }

        public void twice() {
        }

        public void phantomTwice() {
        }
    }

    public static class TestCallOriginalWeave {
        private boolean callFirst;

        public TestCallOriginalWeave(boolean callFirst) {
            this.callFirst = callFirst;
        }

        public void none() {
        }

        public void once() {
            Weaver.callOriginal();
        }

        public void twice() {
            Weaver.callOriginal();
            Weaver.callOriginal();
        }

        public void phantomTwice() {
            // though callOriginal will be executed once at runtime here, we only allow one invoke per weave method
            // our implementation currently makes this assumption and it simplifies things
            // also, I think someone could always factor these kinds of cases into a single call
            // so I don't think we're disallowing anything conceptually with this restriction
            // we can revisit this case later and add support if necessary
            if (callFirst) {
                Weaver.callOriginal();
            } else {
                Weaver.callOriginal();
            }
        }
    }

    /**
     * Test that implied return type of callOriginal invocations is the same as the original method's return type
     */
    @Test
    public void testCallOriginalIllegalReturnType() throws Exception {
        String internalName = Type.getInternalName(ReturnTypeWeave.class);
        WeaveTestUtils.expectViolations(ReturnTypeOriginal.class, ReturnTypeWeave.class, false, new WeaveViolation(
                        METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE, internalName, new Method("voidMethod", "()V")),
                new WeaveViolation(METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE, internalName, new Method("booleanMethod",
                        "()Z")));
    }

    public static class ReturnTypeOriginal {
        public void voidMethod() {
        }

        public boolean booleanMethod() {
            return true;
        }

        public int intMethod() {
            return 1;
        }

        public double doubleMethod() {
            return 1.0;
        }

        public Object objectMethod() {
            return new Object();
        }

        public String stringMethod() {
            return "string";
        }
    }

    public static class ReturnTypeWeave {
        public void voidMethod() {
            Object val = Weaver.callOriginal(); // violation - original returns void, not Object
        }

        public boolean booleanMethod() {
            String val = Weaver.callOriginal(); // violation - original returns boolean, not String
            return false;
        }

        public int intMethod() {
            Object val = Weaver.callOriginal(); // original returns int, but Object is OK
            return 1;
        }

        public double doubleMethod() {
            Double val = Weaver.callOriginal(); // original returns double, but Double is OK
            return 1.0;
        }

        public Object objectMethod() {
            // you might think this should be a violation - original returns Object, not String
            // however, since original returns Object, it's possible for the user to cast to whatever they want
            // e.g. String s = Weaver.callOriginal() has the same bytecode as String s = (String) Weaver.callOriginal()
            // as such, we can only validate call original's return type iff the method returns void or a primitive
            String val = Weaver.callOriginal();
            return new Object();
        }

        public String stringMethod() {
            Object val = Weaver.callOriginal(); // original returns String, but Object is a superclass
            return "string";
        }
    }

    /**
     * Test nested class validations
     */
    @Test
    public void testWeavingNestedClassesViolations() throws Exception {

        // outer class should not cause violations
        WeaveTestUtils.expectViolations(OuterClassFields.class, OuterClassFieldsWeaved.class, false);

        // check inner non-static class violations

        // find the parent this (e.g. this$0) field (the synthetic field name changes depending on the compiler)
        String parentThisFieldName = WeaveTestUtils.findParentThisFieldName(OuterClassFieldsWeaved.InnerClass.class);
        // Assert.assertNotNull(parentThisFieldName);

        String innerName = Type.getInternalName(OuterClassFieldsWeaved.InnerClass.class);
        WeaveTestUtils.expectViolations(OuterClassFields.InnerClass.class, OuterClassFieldsWeaved.InnerClass.class,
                false, new WeaveViolation(CLASS_NESTED_NONSTATIC_UNSUPPORTED, innerName), new WeaveViolation(
                        FIELD_TYPE_MISMATCH, innerName, parentThisFieldName), new WeaveViolation(
                        FIELD_FINAL_ASSIGNMENT, innerName, parentThisFieldName), new WeaveViolation(
                        INIT_NEW_UNSUPPORTED, innerName, new Method(WeaveUtils.INIT_NAME,
                        "(Lcom/newrelic/weave/ClassMatchTest$OuterClassFieldsWeaved;)V")), new WeaveViolation(
                        METHOD_NEW_NON_PRIVATE_UNSUPPORTED, innerName, new Method(WeaveUtils.INIT_NAME,
                        "(Lcom/newrelic/weave/ClassMatchTest$OuterClassFieldsWeaved;)V")));

        // check nested static class violations
        WeaveTestUtils.expectViolations(OuterClassFields.NestedStaticClass.class,
                OuterClassFieldsWeaved.NestedStaticClass.class, false, new WeaveViolation(
                        CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED,
                        Type.getInternalName(OuterClassFieldsWeaved.NestedStaticClass.class),
                        new Method("returnInt", "()I")));
    }

    public static class OuterClassFields {
        private static int outerField = 6;

        public int returnFive() {
            return 5;
        }

        public class InnerClass {
        }

        public static class NestedStaticClass {
            private String nestedClassField = "nestedClassField";

            public int returnInt() {
                nestedClassField.toString();
                return 5;
            }
        }
    }

    public static class OuterClassFieldsWeaved {
        private static int outerField;  // this will cause a CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED error
        private static int newOuterField = 100; // we allow access to this

        public int returnFive() {
            return 6;
        }

        public class InnerClass {
        }

        public static class NestedStaticClass {
            public int returnInt() {
                return outerField + newOuterField;
            }
        }
    }

    /**
     * Test valid class initializer
     */
    @Test
    public void testValidClassInit() throws IOException {
        // successful match
        WeaveTestUtils.expectViolations(ValidClassInitOriginal.class, ValidClassInitWeave.class, false);
    }

    public static class ValidClassInitOriginal {
        public static double d = 1.0;
        public static int i = 1;
        public static int[] iarr = new int[] { i };
        public static String s = "string";
        public static Object obj = new Object();
        public static Object[] objarr = new Object[] { new Object() };
        public static Properties props = new Properties();
        public static Properties[] propsarr = new Properties[] { new Properties() };

        public static void incrementI() {
            i++;
        }
    }

    public static class ValidClassInitWeave {
        // original static fields
        public static double d = Weaver.callOriginal();
        public static int i = Weaver.callOriginal();
        public static int[] iarr = Weaver.callOriginal();
        public static String s = Weaver.callOriginal();
        public static Object obj = Weaver.callOriginal();
        public static Object[] objarr = Weaver.callOriginal();
        public static Properties props = Weaver.callOriginal();
        public static Properties[] propsarr = Weaver.callOriginal();

        // new static fields
        private static double dNew = 2.0;
        private static int iNew = 2;
        private static int[] iarrNew = new int[] { iNew };
        private static String sNew = "string";
        private static Object objNew = new Object();
        private static Object[] objarrNew = new Object[] { new Object() };
        private static Properties propsNew = newProperties();
        private static Properties[] propsarrNew;

        // method used to assign a static field
        private static Properties newProperties() {
            return new Properties();
        }

        // a static initializer that doesn't access matched private members is also allowed
        static {
            propsarrNew = new Properties[] { new Properties() };
            ValidClassInitOriginal.incrementI();
        }
    }

    /**
     * Test invalid class initializers
     */
    @Test
    public void testInvalidClassInit() throws IOException {
        // cannot assign a matched static field to anything other thatn Weaver.callOriginal()
        // (this is because there may be no <clinit> on original, and we cannot add one without modifying structure)
        WeaveTestUtils.expectViolations(InvalidClassInitOriginal.class, ModifyMatchedFieldClassInitWeave.class, false,
                new WeaveViolation(CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED,
                        Type.getInternalName(ModifyMatchedFieldClassInitWeave.class), WeaveUtils.CLASS_INIT_METHOD));

        // cannot use Weaver.callOriginal() to assign a new field
        WeaveTestUtils.expectViolations(InvalidClassInitOriginal.class, AssignNewWithCallOriginalClassInitWeave.class,
                false, new WeaveViolation(INIT_ILLEGAL_CALL_ORIGINAL,
                        Type.getInternalName(AssignNewWithCallOriginalClassInitWeave.class),
                        WeaveUtils.CLASS_INIT_METHOD));

        // cannot use matched private/protected/package-private fields to assign a new field
        String fieldClassName = Type.getInternalName(AccessPrivateFieldClassInitWeave.class);
        WeaveTestUtils.expectViolations(InvalidClassInitOriginal.class, AccessPrivateFieldClassInitWeave.class, false,
                new WeaveViolation(CLINIT_FIELD_ACCESS_VIOLATION, fieldClassName, "matchedPrivateField"),
                new WeaveViolation(CLINIT_FIELD_ACCESS_VIOLATION, fieldClassName, "matchedProtectedField"),
                new WeaveViolation(CLINIT_FIELD_ACCESS_VIOLATION, fieldClassName, "matchedPackageField"));

        // cannot use matched private/protected methods to assign a new field
        String methodClassName = Type.getInternalName(AccessPrivateMethodClassInitWeave.class);
        String methodDescriptor = Type.getMethodDescriptor(Type.getType(String.class));
        WeaveTestUtils.expectViolations(InvalidClassInitOriginal.class, AccessPrivateMethodClassInitWeave.class, false,
                new WeaveViolation(CLINIT_METHOD_ACCESS_VIOLATION, methodClassName, new Method("matchedPrivateMethod", methodDescriptor)),
                new WeaveViolation(CLINIT_METHOD_ACCESS_VIOLATION, methodClassName, new Method("matchedProtectedMethod", methodDescriptor)),
                new WeaveViolation(CLINIT_METHOD_ACCESS_VIOLATION, methodClassName, new Method("matchedPackageMethod", methodDescriptor)));
    }

    public static class InvalidClassInitOriginal {
        // used in ModifyMatchedFeildClassInitWeave
        public static String assignedFieldDisallowed;
        public static String callOriginalFieldAllowed;

        // used in AccessPrivateFieldClassInitWeave
        public static String matchedPublicField = "public";
        private static String matchedPrivateField = "private";
        protected static String matchedProtectedField = "protected";
        static String matchedPackageField = "package-private";

        // used in AccessPrivateMethodClassInitWeave
        public static String matchedPublicMethod() {
            return "publicMethod";
        }

        private static String matchedPrivateMethod() {
            return "privateMethod";
        }

        protected static String matchedProtectedMethod() {
            return "protectedMethod";
        }

        static String matchedPackageMethod() {
            return "packageMethod";
        }
    }

    public static class ModifyMatchedFieldClassInitWeave {
        public static String assignedFieldDisallowed = "";
        public static String callOriginalFieldAllowed = Weaver.callOriginal();
    }

    public static class AssignNewWithCallOriginalClassInitWeave {
        private static String newFieldCallOriginalDisallowed = Weaver.callOriginal();
        private static String newFieldAssignmentAllowed = "";
    }

    public static class AccessPrivateFieldClassInitWeave {
        public static String matchedPublicField = Weaver.callOriginal();
        private static String newFieldAssignedwithPublicField = matchedPublicField; // OK

        private static String matchedPrivateField = Weaver.callOriginal();
        private static String newFieldAssignedwithPrivateField = matchedPrivateField; // not OK

        protected static String matchedProtectedField = Weaver.callOriginal();
        private static String newProtectedField = matchedProtectedField; // not OK

        static String matchedPackageField = Weaver.callOriginal();
        private static String newPackageField = matchedPackageField; // not OK
    }

    public static class AccessPrivateMethodClassInitWeave {

        public static String matchedPublicMethod() {
            return Weaver.callOriginal();
        }

        private static String newPublicField = matchedPublicMethod(); // OK

        private static String matchedPrivateMethod() {
            return Weaver.callOriginal();
        }

        private static String newPrivateField = matchedPrivateMethod(); // not OK

        protected static String matchedProtectedMethod() {
            return Weaver.callOriginal();
        }

        private static String newProtectedField = matchedProtectedMethod(); // not OK

        static String matchedPackageMethod() {
            return Weaver.callOriginal();
        }

        private static String newPackageField = matchedPackageMethod(); // not OK
    }

    @Test
    public void testWeaveAllNoOtherConstructors() throws IOException {
        boolean isBaseMatch = true;
        WeaveTestUtils.expectViolations(Ctor.class, CtorWeave.class, isBaseMatch, new WeaveViolation(
                INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED, Type.getInternalName(CtorWeave.class)));
    }

    public static class Ctor {
        public Ctor() {
        }

        public Ctor(int i) {
        }
    }

    public static class CtorWeave {

        @WeaveAllConstructors
        public CtorWeave() {
        }

        public CtorWeave(int i) {
        }
    }

    @Test
    public void testWeaveArgsConstructor() throws IOException {
        WeaveTestUtils.expectViolations(Ctor.class, CtorWeaveWithArgs.class, true, new WeaveViolation(
                INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED, Type.getInternalName(CtorWeaveWithArgs.class)));
    }

    public static class CtorWeaveWithArgs {

        @WeaveAllConstructors
        public CtorWeaveWithArgs(int i) {
            /*
             * @WeaveAllConstructors not allowed in constructors with parameters.
             *
             * We make this a violation to prevent use of constructor arguments that may not be present in other
             * constructors.
             */
        }
    }

    @Test
    public void testMulitpleWeaveAllConstructors() throws IOException {
        WeaveTestUtils.expectViolations(Ctor.class, MultipleWeaveAllCtors.class, true, new WeaveViolation(
                INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED, Type.getInternalName(MultipleWeaveAllCtors.class)));
    }

    public static class MultipleWeaveAllCtors {

        @WeaveAllConstructors
        public MultipleWeaveAllCtors() {
        }

        @WeaveAllConstructors
        public MultipleWeaveAllCtors(int i) {
        }
    }

}
