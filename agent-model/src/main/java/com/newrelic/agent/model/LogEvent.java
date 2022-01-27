/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
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

    // FIXME probably don't need to pass timestamp as we use the value from the log event captured in the library instrumentation
    public LogEvent(long timestamp, Map<String, Object> attributes, float priority) {
        super(LOG_EVENT_TYPE, timestamp, priority, attributes);
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
