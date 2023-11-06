/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomInsightsEvent extends AnalyticsEvent implements JSONStreamAware {

    private final Map<String, Object> extraIntrinsics;
    private volatile float mutablePriority;

    public CustomInsightsEvent(String type, long timestamp, Map<String, Object> attributes, float priority) {
        this(type, timestamp, attributes, Collections.emptyMap(), priority);
    }

    public CustomInsightsEvent(String type, long timestamp, Map<String, Object> attributes,
                               Map<String, Object> extraIntrinsics, float priority) {
        super(type, timestamp, priority, attributes);
        this.mutablePriority = priority;
        this.extraIntrinsics = extraIntrinsics;
    }

    public Map<String, Object> getIntrinsics() {
        Map<String, Object> intrinsics = new HashMap<>();
        intrinsics.put("type", getType());
        intrinsics.put("timestamp", getTimestamp());
        intrinsics.putAll(extraIntrinsics);
        return intrinsics;
    }

    public void setPriority(float priority) {
        this.mutablePriority = priority;
    }

    @Override
    public float getPriority() {
        return mutablePriority;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject intrinsics = new JSONObject();
        intrinsics.putAll(getIntrinsics());
        JSONArray.writeJSONString(Arrays.asList(intrinsics, getMutableUserAttributes()), out);
    }

}
