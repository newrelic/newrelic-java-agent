/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.testclasses.MyOriginalInterface")
public class MyWeaveInterface {
    @NewField
    private String newMemberField = "newMemberField";

    @NewField
    private static String newStaticField = "newStaticField";

    public boolean isInterfaceWeaved() {
        return true;
    }

    public String getMemberField() {
        return newMemberField;
    }

    public String getStaticField() {
        return newStaticField;
    }

}
