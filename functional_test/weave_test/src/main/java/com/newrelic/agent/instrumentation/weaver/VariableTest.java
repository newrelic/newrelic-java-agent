/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver;

import java.util.Map;

import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public class VariableTest {

    @NewField
    private double cacheDouble;
    @NewField
    private int cache;
    @NewField
    private static final String KEY = "test";

    public String useConstant(Map<String, Object> map) {
        Object v = map.get(KEY);

        String orig = Weaver.callOriginal();

        if (v == null) {
            return orig;
        } else {
            return v.toString();
        }
    }

    public int memberVariable(int x) {
        int tmp = cache;
        Weaver.callOriginal();

        cache = x;
        return tmp;
    }

    public double doubleMemberVariable(double d) {
        double tmp = cacheDouble;

        System.err.println("D = " + d + " TMP = " + tmp);

        Weaver.callOriginal();

        cacheDouble = d;
        return tmp;
    }

    public Object test() {
        Object t = Weaver.callOriginal();
        return "dude";
    }

}
