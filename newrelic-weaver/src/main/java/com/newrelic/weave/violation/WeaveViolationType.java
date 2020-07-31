/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.violation;

/**
 * A violation of the weave API. Used to validate @Weave classes against original classes.
 */
public enum WeaveViolationType {

    CLASS_WEAVE_IS_INTERFACE("@Weave classes can not be interfaces"),
    CLASS_ACCESS_MISMATCH("Class access levels do not match"),
    CLASS_NESTED_NONSTATIC_UNSUPPORTED("Non-static nested classes are not supported in @Weave classes"),
    CLASS_EXTENDS_ILLEGAL_SUPERCLASS("@Weave classes must extend java.lang.Object or the same superclass as the original class"),
    CLASS_IMPLEMENTS_ILLEGAL_INTERFACE("@Weave classes cannot implement interfaces that the original class does not implement"),
    CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED("Nested classes cannot implicitly access outer fields/methods in @Weave classes"),
    CLASS_MISSING_REQUIRED_ANNOTATIONS("Class does not contain at least one of the required class annotations"),
    ENUM_NEW_FIELD("Enums cannot add new fields"),
    FIELD_TYPE_MISMATCH("Cannot match fields with the same name but different types"),
    FIELD_FINAL_MISMATCH("Cannot match a non-final field with a final field"),
    FIELD_FINAL_ASSIGNMENT("Cannot assign a value other than Weaver.callOriginal() to a matched final field"),
    FIELD_STATIC_MISMATCH("Cannot match a non-static field with a static field"),
    FIELD_ACCESS_MISMATCH("Field access levels do not match"),
    FIELD_PRIVATE_BASE_CLASS_MATCH("Cannot match a private field when using base class matching"),
    FIELD_SERIALVERSIONUID_UNSUPPORTED("Cannot define a serialVersionUID field"),
    METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE("@Weave methods may only invoke Weaver.callOriginal() once"),
    METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE("Return type of Weaver.callOriginal() does not match original return type"),
    METHOD_EXACT_ABSTRACT_WEAVE("Exact matches cannot weave abstract methods"),
    METHOD_BASE_CONCRETE_WEAVE("Base matches cannot weave concrete parent methods"),
    METHOD_INDIRECT_INTERFACE_WEAVE("Cannot weave indirect interface methods"),
    METHOD_MISSING_REQUIRED_ANNOTATIONS("Method does not contain at least one of the required method annotations"),
    METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED("New @Weave methods may not invoke Weaver.callOriginal()"),
    METHOD_NEW_INVOKE_UNSUPPORTED("Cannot invoke a new method from another new method"),
    METHOD_NEW_ABSTRACT_UNSUPPORTED("Cannot define a new abstract method"),
    METHOD_NEW_NON_PRIVATE_UNSUPPORTED("New methods must be declared private"),
    METHOD_NATIVE_UNSUPPORTED("Native method weaving is unsupported"),
    METHOD_STATIC_MISMATCH("Cannot match a non-static method with a static method"),
    METHOD_ACCESS_MISMATCH("Method access levels do not match"),
    METHOD_THROWS_MISMATCH("Method throws do not match. Ensure that the weave method only throws exceptions that exist in the original method signature"),
    METHOD_SYNTHETIC_WEAVE_ILLEGAL("Cannot weave a synthetic method"),
    METHOD_RETURNTYPE_MISMATCH("Matched methods must have the same return type"),
    NON_VOID_NO_PARAMETERS_WEAVE_ALL_METHODS("@WeaveIntoAllMethods can only be applied to a method with no parameters and void return type"),
    NON_STATIC_WEAVE_INTO_ALL_METHODS("@WeaveIntoAllMethods must be static"),
    INIT_NEW_UNSUPPORTED("Cannot add new constructors in a @Weave class"),
    INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED("Only no-argument constructors are allowed in @Weave classes that match interfaces"),
    INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED("Only one constructor is allowed in @Weave classes when @WeaveAllConstructors is present"),
    INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED("Cannot apply @WeaveAllConstructors to constructor with arguments"),
    INIT_ILLEGAL_CALL_ORIGINAL("Only matched members can be initialized with Weaver.callOriginal()"),
    CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED("Cannot modify the value of a matched static field"),
    CLINIT_FIELD_ACCESS_VIOLATION("Cannot access a matched private or protected static field during weave class initialization"),
    CLINIT_METHOD_ACCESS_VIOLATION("Cannot call a matched private or protected static method during weave class initialization"),
    MISSING_ORIGINAL_BYTECODE("Could not find original bytecode"),
    BOOTSTRAP_CLASS("Cannot weave a bootstrap class without java.lang.instrument.Instrumentation"),
    INVALID_REFERENCE("Code in the weave packages references a class in a way that does not match the original code"),
    LANGUAGE_ADAPTER_VIOLATION("Non-Java violation."),
    UNEXPECTED_NEW_FIELD_ANNOTATION("Field is marked with @NewField, but is matched under the current ClassLoader"),
    EXPECTED_NEW_FIELD_ANNOTATION("Field is not marked with @NewField, but is not matched under the current ClassLoader"),
    ILLEGAL_CLASS_NAME("Cannot define a new class with the same name as an original class"),
    INCOMPATIBLE_BYTECODE_VERSION("The major version of the weave bytecode is higher than the jvm supports."),
    SKIP_IF_PRESENT("Encountered illegal original class."),
    MULTIPLE_WEAVE_ALL_METHODS("Encountered more than one method with @WeaveIntoAllMethods");

    private final String message;

    WeaveViolationType(String message) {
        this.message = message;
    }

    /**
     * Description of the violation.
     *
     * @return description of the violation
     */
    public String getMessage() {
        return message;
    }
}
