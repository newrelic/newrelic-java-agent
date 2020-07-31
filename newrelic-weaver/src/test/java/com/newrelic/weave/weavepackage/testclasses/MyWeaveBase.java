/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.BaseClass, originalName = "com.newrelic.weave.weavepackage.testclasses.MyOriginalBase")
public class MyWeaveBase {

    public boolean isWeaved() {
        return true;
    }

    public boolean isBaseCallWeaved() {
        return true;
    }

}
