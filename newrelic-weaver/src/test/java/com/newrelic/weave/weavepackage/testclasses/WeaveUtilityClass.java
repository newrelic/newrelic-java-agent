/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.Weaver;

public class WeaveUtilityClass {
    public String s = "s";
    public MyOriginalInterface anInterfaceField = new MyOriginalTarget1();

    public WeaveUtilityClass wuc = null;

    public ShadowedBaseClass[] aUtilMethod(MyOriginalBase base, MyOriginalTarget1 target1, MyOriginalExact exact) {
        base.isWeaved();
        target1.unweaved();

        return null;
    }

    public String getImplementationTitle() {
        return Weaver.getImplementationTitle();
    }
}
