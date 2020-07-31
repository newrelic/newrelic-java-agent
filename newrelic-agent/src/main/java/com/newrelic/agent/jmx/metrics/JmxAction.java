/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx.metrics;

import java.text.MessageFormat;
import java.util.Map;

public enum JmxAction {

    /** The is the typical behavior. It uses the value of the first attribute listed. */
    USE_FIRST_ATT {

        @Override
        public float performAction(String[] pAttributes, Map<String, Float> pValues) throws IllegalArgumentException {
            if ((pAttributes == null) || (pAttributes.length == 0)) {
                return 0;
            }
            return getValue(pValues, pAttributes[0]);
        }

    },
    /**
     * This can be used when different MBean Servers are being used within the same JVM (there was a ticket for this for
     * JBoss 5). In this case, one server had ActiveSession while the other server had activeSession resulting in only
     * some sessions being reported. We want to report all sessions, but we want to ensure that we only report each
     * session once. Therefore this will grab the first value listed in the map.
     */
    USE_FIRST_RECORDED_ATT {

        @Override
        public float performAction(String[] pAttributes, Map<String, Float> pValues) throws IllegalArgumentException {
            if ((pAttributes == null) || (pAttributes.length == 0)) {
                return 0;
            }
            Float value = null;
            for (String current : pAttributes) {
                value = getValueNullOkay(pValues, current);
                if (value != null) {
                    return value;
                }
            }
            return 0;
        }

    },
    /** This will perform pAttributes[0] - pAttributes[1] - pAttributes[2] . . . - pAttributes[n]. */
    SUBTRACT_ALL_FROM_FIRST {
        @Override
        public float performAction(String[] pAttributes, Map<String, Float> values) throws IllegalArgumentException {
            float output;
            if (pAttributes == null) {
                output = 0;
            } else {
                int length = pAttributes.length;
                if (length == 0) {
                    output = 0;
                } else {
                    output = getValue(values, pAttributes[0]);
                    if (length > 1) {
                        for (int i = 1; i < length; i++) {
                            output -= getValue(values, pAttributes[i]);
                        }
                    }
                    if (output < 0) {
                        throw new IllegalArgumentException(MessageFormat.format(
                                "The output value can not be negative: {0} ", output));
                    }
                }
            }
            return output;
        }
    },
    /** This will perform pAttributes[0] + pAttributes[1] + pAttributes[2] . . . + pAttributes[n]. */
    SUM_ALL {
        @Override
        public float performAction(String[] pAttributes, Map<String, Float> values) throws IllegalArgumentException {
            float output;
            if (pAttributes == null) {
                output = 0;
            } else {
                int length = pAttributes.length;
                if (length == 0) {
                    output = 0;
                } else {
                    output = getValue(values, pAttributes[0]);
                    if (length > 1) {
                        for (int i = 1; i < length; i++) {
                            output += getValue(values, pAttributes[i]);
                        }
                    }
                    if (output < 0) {
                        throw new IllegalArgumentException(MessageFormat.format(
                                "The output value can not be negative: {0} ", output));
                    }
                }
            }
            return output;
        }
    };

    public abstract float performAction(String[] attributes, Map<String, Float> values) throws IllegalArgumentException;

    private static float getValue(Map<String, Float> values, String att) {
        Float value = values.get(att);
        if (value == null) {
            throw new IllegalArgumentException(MessageFormat.format("There is no value for attribute {0}", att));
        } else {
            return value;
        }
    }

    private static Float getValueNullOkay(Map<String, Float> values, String att) {
        Float value = values.get(att);
        if (value == null) {
            return null;
        } else {
            return value;
        }
    }

}
