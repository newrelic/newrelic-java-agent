/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public abstract class MyOriginalBase {

    public abstract boolean isWeaved();

    public boolean isBaseCallWeaved() {
        return false;
    }

    public boolean unweaved() {
        return true;
    }

}
