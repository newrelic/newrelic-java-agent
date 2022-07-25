/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

public class LogEvent extends AnalyticsEvent implements JSONStreamAware {

    public static final String LOG_EVENT_TYPE = "LogEvent";

    private volatile float mutablePriority;

    public LogEvent(Map<String, Object> attributes, float priority) {
        super(LOG_EVENT_TYPE, System.currentTimeMillis(), priority, attributes);
        this.mutablePriority = priority;
    }

    @Override
    public float getPriority() {
        return mutablePriority;
    }

    public void setPriority(float priority) {
        this.mutablePriority = priority;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject.writeJSONString(getMutableUserAttributes(), out);
    }

}
