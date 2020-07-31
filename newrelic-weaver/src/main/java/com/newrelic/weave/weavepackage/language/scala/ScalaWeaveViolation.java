/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language.scala;

import com.google.common.base.MoreObjects;
import com.newrelic.weave.violation.WeaveViolation;
import com.newrelic.weave.violation.WeaveViolationType;

public class ScalaWeaveViolation extends WeaveViolation {
    private static final WeaveViolationType TYPE = WeaveViolationType.LANGUAGE_ADAPTER_VIOLATION;

    private final String weaveClass;
    private final String violationMessage;
    private final ScalaWeaveViolationType violationType;

    public ScalaWeaveViolation(String weaveClass, ScalaWeaveViolationType violationType) {
        this(weaveClass, violationType, null);
    }

    public ScalaWeaveViolation(String weaveClass, ScalaWeaveViolationType violationType, String violationMessage) {
        super(TYPE, weaveClass);
        this.weaveClass = weaveClass;
        this.violationMessage = violationMessage;
        this.violationType = violationType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("violationType", violationType).add("weaveClass",
                weaveClass).add("violationType", violationType).add("violationMessage", violationMessage).toString();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScalaWeaveViolation other = (ScalaWeaveViolation) o;
        return (violationType == other.violationType)
            && (weaveClass == null ? other.weaveClass == null : weaveClass.equals(other.weaveClass))
            && (violationMessage == null ? other.violationMessage == null : violationMessage.equals(other.violationMessage));
    }

}
