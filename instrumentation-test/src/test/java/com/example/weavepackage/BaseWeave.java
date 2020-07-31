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

@Weave(type = MatchType.BaseClass, originalName = "com.example.weavepackage.BaseOriginal")
public class BaseWeave {

    public String baseMethod() {
        return "weaved " + Weaver.callOriginal();
    }

    public String abstractMethod() {
        return "weaved " + Weaver.callOriginal();
    }
}
