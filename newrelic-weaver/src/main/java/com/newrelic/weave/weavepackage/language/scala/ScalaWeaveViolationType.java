/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.language.scala;

public enum ScalaWeaveViolationType {
    // @formatter:off
    CLASS_WEAVE_IS_OBJECT("@Weave classes can not be scala objects"),
    CLASS_WEAVE_IS_TRAIT("@Weave classes can not be scala traits");
    // @formatter:on
    private final String message;

    ScalaWeaveViolationType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}