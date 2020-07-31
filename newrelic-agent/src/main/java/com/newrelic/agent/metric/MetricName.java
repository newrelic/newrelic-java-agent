/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.metric;

import com.newrelic.agent.MetricNames;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to hold the name and scope of a metric.
 * 
 * This class is thread-safe.
 */
public final class MetricName implements JSONStreamAware {

    public static final MetricName WEB_TRANSACTION_ORM_ALL = MetricName.create(MetricNames.ORM + '/'
            + MetricNames.ALL_WEB);
    public static final MetricName OTHER_TRANSACTION_ORM_ALL = MetricName.create(MetricNames.ORM + '/'
            + MetricNames.ALL_OTHER);
    public static final MetricName WEB_TRANSACTION_SOLR_ALL = MetricName.create(MetricNames.SOLR + "/"
            + MetricNames.ALL_WEB);
    public static final MetricName OTHER_TRANSACTION_SOLR_ALL = MetricName.create(MetricNames.SOLR + "/"
            + MetricNames.ALL_OTHER);
    public static final MetricName QUEUE_TIME = MetricName.create(MetricNames.QUEUE_TIME);

    private static final String NAME_KEY = "name";
    private static final String SCOPE_KEY = "scope";
    public static final String EMPTY_SCOPE = "";

    private final String name;
    private final String scope;
    private final int hashCode;

    private MetricName(String name, String scope) {
        this.name = name;
        this.scope = scope;
        this.hashCode = generateHashCode(name, scope);
    }

    public String getName() {
        return name;
    }

    public String getScope() {
        return scope;
    }

    public boolean isScoped() {
        return scope != EMPTY_SCOPE;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    public static int generateHashCode(String name, String scope) {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + scope.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MetricName other = (MetricName) obj;
        return name.equals(other.name) && scope.equals(other.scope);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(name);
        if (isScoped()) {
            sb.append(" (").append(scope).append(')');
        }
        return sb.toString();
    }

    @Override
    public void writeJSONString(Writer writer) throws IOException {
        // this will resize every time is set to 2 since load capacity is .75 percent
        Map<String, Object> props = new HashMap<>(3);
        if (isScoped()) {
            props.put(SCOPE_KEY, scope);
        }
        props.put(NAME_KEY, name);
        JSONObject.writeJSONString(props, writer);
    }

    public static MetricName create(String name, String scope) {
        if (name == null || name.length() == 0) {
            return null;
        }
        if (scope == null || scope.length() == 0) {
            scope = EMPTY_SCOPE;
        }
        return new MetricName(name, scope);
    }

    public static MetricName create(String name) {
        return create(name, null);
    }

    public static MetricName parseJSON(JSONObject jsonObj) {
        String scope = String.class.cast(jsonObj.get(SCOPE_KEY));
        String name = String.class.cast(jsonObj.get(NAME_KEY));
        return MetricName.create(name, scope);
    }

}
