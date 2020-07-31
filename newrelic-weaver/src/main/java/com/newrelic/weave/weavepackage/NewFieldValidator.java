/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import java.util.Collection;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.weave.ClassMatch;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;

/**
 * Utility class that validates {@link NewField} annotation for a specified {@link ClassMatch}.
 */
public final class NewFieldValidator {
    private static final String NEW_FIELD_DESC = Type.getType(NewField.class).getDescriptor();

    private NewFieldValidator() {
    }

    /**
     * Validate that all new fields in by the specified {@link ClassMatch} have a {@link NewField} annotation on them,
     * and that all other fields do not.
     * @param match {@link ClassMatch} containing fields that should be new or matched
     * @param violations collection to add any violations to
     */
    public static void validate(ClassMatch match, Collection<WeaveViolation> violations) {

        // make sure all new fields have the @NewField annotation on them
        for (String newFieldName : match.getNewFields()) {
            if (!hasNewFieldAnnotation(match, newFieldName)) {
                violations.add(new WeaveViolation(WeaveViolationType.EXPECTED_NEW_FIELD_ANNOTATION,
                        match.getWeave().name, newFieldName));
            }
        }

        // make sure all matched fields do NOT have the @NewField annotation on them
        for (String matchedFieldName : match.getMatchedFields()) {
            if (hasNewFieldAnnotation(match, matchedFieldName)) {
                violations.add(new WeaveViolation(WeaveViolationType.UNEXPECTED_NEW_FIELD_ANNOTATION,
                        match.getWeave().name, matchedFieldName));
            }
        }
    }

    private static boolean hasNewFieldAnnotation(ClassMatch match, String fieldName) {
        FieldNode fieldNode = WeaveUtils.findRequiredMatch(match.getWeave().fields, fieldName);
        if(fieldNode.visibleAnnotations != null) {
            for (AnnotationNode annotationNode : fieldNode.visibleAnnotations) {
                if (annotationNode.desc.equals(NEW_FIELD_DESC)) {
                    return true;
                }
            }
        }
        return false;
    }
}
