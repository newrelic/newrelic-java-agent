/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public abstract class AnalyticsEvent implements PriorityAware {
    private static final Pattern TYPE_VALID = Pattern.compile("^[a-zA-Z0-9:_ ]{1,255}$");

    private final float priority;

    /**
     * Required. Must match /^[a-zA-Z0-9:_ ]+$/ and be less than 256 chars.<br>
     * Validate and discard invalid events before reporting.
     */
    private final String type;

    /**
     * Required. Start time of the transaction (UTC timestamp)
     */
    private final long timestamp;

    /**
     * Optional. User custom parameters.
     */
    private final Map<String, ?> userAttributes;

    protected AnalyticsEvent(String type, long timestamp, float priority, Map<String, ?> userAttributes) {
        this.type = type;
        this.timestamp = timestamp;
        this.priority = priority;
        if (userAttributes != null) {
            this.userAttributes = userAttributes;
        } else {
            this.userAttributes = new HashMap<>();
        }
    }

    /**
     * Purposefully protected so the Map can only be manipulated by
     * subclasses.
     */
    protected Map<String, ?> getMutableUserAttributes() {
        return userAttributes;
    }

    public static boolean isValidType(String type) {
        return type != null && TYPE_VALID.matcher(type).matches();
    }

    public boolean isValid() {
        return isValidType(type);
    }

    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public float getPriority() {
        return priority;
    }

    public Map<String, Object> getUserAttributesCopy() {
        if (userAttributes == null) {
            return null;
        }
        return new HashMap<>(userAttributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AnalyticsEvent that = (AnalyticsEvent) o;
        return Float.compare(that.priority, priority) == 0 &&
                timestamp == that.timestamp &&
                Objects.equals(type, that.type) &&
                Objects.equals(userAttributes, that.userAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, type, timestamp, userAttributes);
    }
}