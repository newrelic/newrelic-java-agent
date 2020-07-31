/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metrics;

public class ContextObject {

    private long lValue;
    private String sValue;

    public ContextObject(Object val) {
        if (val instanceof String) {
            sValue = (String) val;
        } else if (val instanceof Long) {
            lValue = (Long) val;
            sValue = "";
        } else {
            sValue = String.valueOf(val);
        }
    }

    public ContextObject(Long pValue) {
        lValue = pValue;

    }

}
