/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.example.weavepackage;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "com.example.weavepackage.InterfaceOriginal")
public class InterfaceWeave {

    public String interfaceMethod() {
        return "weaved " + Weaver.callOriginal();
    }
}
