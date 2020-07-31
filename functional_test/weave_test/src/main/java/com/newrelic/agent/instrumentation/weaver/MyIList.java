/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.Interface, originalName = "com.newrelic.agent.instrumentation.weaver.IList")
public abstract class MyIList {

    @NewField
    private Object instance = "WORKING_INSTANCE";

    @NewField
    private static Object STATIC = "WORKING_STATIC";

    @Trace
    public int count() {
        return Weaver.<Integer> callOriginal() + 666;
    }

    @Trace
    public boolean remove(Object item) {
        NewRelic.getAgent().getTracedMethod().setMetricName("DUDE");
        Weaver.callOriginal();
        return true;
    }

    public Object instanceFieldTest() {
        Weaver.callOriginal();
        return instance;
    }

    public Object staticFieldTest() {
        Weaver.callOriginal();
        return STATIC;
    }

    public int unimplementedWeaveMethodTest() {
        Weaver.callOriginal();
        return this.unimplementedWeaveMethod() + 5;
    }

    public abstract int unimplementedWeaveMethod();
}