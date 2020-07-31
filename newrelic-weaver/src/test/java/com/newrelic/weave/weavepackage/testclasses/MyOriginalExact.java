/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public class MyOriginalExact {
    public boolean isWeaved() {
        return false;
    }

    public String getMemberField() {
        return null;
    }

    public static String getStaticField() {
        return null;
    }

    public String getImplementationTitle() {
        return "wrong";
    }
}
