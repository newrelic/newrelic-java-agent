/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.violation;

import com.google.common.base.MoreObjects;

/**
 * Raised at match time when weave code tries to access original code in an illegal way. For example, if the weave code
 * tries to access a private field.
 */
public class ReferenceViolation extends WeaveViolation {
    private final String weaveClass;
    private final String originalClass;
    private final String violationMessage;
    private final WeaveViolationType type;

    /**
     * Create a {@link ReferenceViolation} of a {@link WeaveViolationType} for the specified weave class, original class,
     * and custom message.
     * @param type violation type
     * @param weaveClass weave class name
     * @param originalClass original class name
     * @param violationMessage message describing the violation
     */
    public ReferenceViolation(WeaveViolationType type, String weaveClass, String originalClass, String violationMessage) {
        super(type, weaveClass);
        this.type = type;
        this.weaveClass = weaveClass;
        this.originalClass = originalClass;
        this.violationMessage = violationMessage;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("type", type).add("weaveClass", weaveClass).add(
                "originalClass", originalClass).add("violationMessage", violationMessage).toString();
    }
}
