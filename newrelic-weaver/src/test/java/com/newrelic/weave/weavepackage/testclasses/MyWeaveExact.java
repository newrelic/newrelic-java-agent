/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.newrelic.weave.weavepackage.testclasses.MyOriginalExact")
public class MyWeaveExact {

    static {
        // This is here to reproduce a thread-safety issue with utility class loading
        WeaveUtilityClass2 utilityClass2 = new WeaveUtilityClass2();
    }

    @NewField
    private String newMemberField = "newMemberField";

    @NewField
    private static String newStaticField = "newStaticField";

    public boolean isWeaved() {
        return true;
    }

    public String getMemberField() {
        return newMemberField;
    }

    public static String getStaticField() {
        return newStaticField;
    }

    public String getImplementationTitle() {
        return Weaver.getImplementationTitle();
    }
}
