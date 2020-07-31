/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

@Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.testclasses.BearInterface")
public class WeaveBearInterface {

    public boolean isWeaved() {
        return true;
    }

    public boolean isAlsoWeaved() {
        return true;
    }
}