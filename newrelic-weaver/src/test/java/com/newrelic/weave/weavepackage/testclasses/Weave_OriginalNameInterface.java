/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage.testclasses;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "com.newrelic.weave.weavepackage.testclasses.OriginalNameInterface")
public abstract class Weave_OriginalNameInterface {

    public abstract void foo();

    public void bar() {
        foo();
        Weaver.callOriginal();
    }

}