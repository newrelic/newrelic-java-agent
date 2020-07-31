/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.weave.ClassMatch;
import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.violation.WeaveViolation;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.newrelic.weave.violation.WeaveViolationType.EXPECTED_NEW_FIELD_ANNOTATION;
import static com.newrelic.weave.violation.WeaveViolationType.UNEXPECTED_NEW_FIELD_ANNOTATION;

/**
 * NewFieldValidatorTest.java
 */
public class NewFieldValidatorTest {

    @Test
    public void testExactMatchFields() throws IOException {
        ClassMatch match = WeaveTestUtils.match(Original.class, Weave.class, false);
        List<WeaveViolation> violations = new ArrayList<>();
        NewFieldValidator.validate(match, violations);

        String internalName = Type.getInternalName(Weave.class);
        WeaveTestUtils.expectViolations(violations, new WeaveViolation(UNEXPECTED_NEW_FIELD_ANNOTATION, internalName,
                "invalidMatchedField"), new WeaveViolation(EXPECTED_NEW_FIELD_ANNOTATION, internalName,
                "invalidNewField"));
    }

    public static class Original {
        public String validMatchedField;
        public String invalidMatchedField;
    }

    public static class Weave {
        @NewField
        public String validNewField;

        public String invalidNewField; // no annotation, this is invalid!

        public String validMatchedField; // no annotation, valid!

        @NewField
        public String invalidMatchedField; // has annotation, invalid!
    }
}
