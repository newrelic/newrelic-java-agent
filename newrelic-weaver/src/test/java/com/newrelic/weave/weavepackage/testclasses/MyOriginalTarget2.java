/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

public class MyOriginalTarget2 implements MyOriginalInterface {

    @Override
    public boolean isInterfaceWeaved() {
        return false;
    }

    @Override
    public String getMemberField() {
        return null;
    }

    @Override
    public String getStaticField() {
        return null;
    }
}
