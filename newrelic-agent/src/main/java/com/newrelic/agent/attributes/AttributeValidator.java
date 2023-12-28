/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.ConfigConstant;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

public class AttributeValidator {
    private final String attributeType;

    private String getAttributeType() {
        return attributeType;
    }

    // Most attribute senders are transaction (demand that a transaction be in progress).
    // Subclasses may choose to disable this check.
    private boolean isTransactional = true;

    // Set of APIs that can report attributes outside of a transaction
    private static final Collection<String> sendParametersOutsideOfTxn = Arrays.asList("noticeError",
            "Span.addCustomParameter", "Span.addCustomParameters", "TracedMethod.addCustomAttributes");

    public AttributeValidator(String attributeType) {
        this.attributeType = attributeType;
    }

    /**
     * Allow attribute sending when outside a transaction, such as error events or custom events.
     *
     * @param isTransactional new value of transactional setting.
     */
    protected void setTransactional(boolean isTransactional) {
        this.isTransactional = isTransactional;
    }

    /**
     * Verifies the input key and value. Null is returned if the key/value is invalid, else the value is returned.
     * Parameters added by the noticeError or transaction-less Span APIs will be reported regardless if they're in a transaction
     * or not. In cases where parameters are longer than the maxUserParameterSize the returned value will be truncated.
     */
    public <T> T verifyParameterAndReturnValue(String key, T value, String methodCalled) {
        if (key == null) {
            Agent.LOG.log(Level.FINER, "Unable to add {0} attribute because {1} was invoked with a null key",
                    getAttributeType(), methodCalled);
            return null;
        }
        if (value == null) {
            Agent.LOG.log(Level.FINER,
                    "Unable to add {0} attribute because {1} was invoked with a null value for key \"{2}\"",
                    getAttributeType(), methodCalled, key);
            return null;
        }

        if (!isAcceptableValueType(value)) {
            return null;
        }

        // key and value are limited to 255 characters
        if (!validateAndLogKeyLength(key, methodCalled)) {
            return null;
        }

        if (value instanceof String) {
            value = (T) truncateValue(key, (String) value, methodCalled);
        }

        if (value instanceof Number || value instanceof Boolean || value instanceof AtomicBoolean) {
            value = (T) verifyValue(value);
        }

        if (sendParametersOutsideOfTxn.contains(methodCalled)) {
            return value;
        }

        Transaction tx = Transaction.getTransaction(false);
        if (isTransactional && (tx == null || !tx.isInProgress())) {
            Agent.LOG.log(Level.FINER,
                    "Unable to add {0} attribute with key \"{1}\" because {2} was invoked outside a New Relic transaction.",
                    getAttributeType(), key, methodCalled);
            return null;
        }

        return value;
    }

    /**
     * Verifies a map of  key/value pairs and returns a new map containing the verified parameters. An empty
     * map is returned if there are no valid keys/values pairs or if addCustomParameters(Map<String, Object>) is invoked
     * outside of a New Relic transaction, with the exception being that parameters added by the noticeError or
     * transaction-less Span APIs will be reported regardless if they're in a transaction or not. In cases where
     * parameters are longer than the maxUserParameterSize the returned value will be truncated.
     */
    protected Map<String, Object> verifyParametersAndReturnValues(Map<String, Object> params, String methodCalled) {
        Map<String, Object> verifiedParams = new LinkedHashMap<>();
        if (params == null || params.isEmpty()) {
            Agent.LOG.log(Level.FINER, "Unable to add {0} attributes because {1} was invoked with a null or empty map",
                    getAttributeType(), methodCalled);
            return Collections.emptyMap();
        }

        // iterate over params in map, creating new map containing only valid entries
        for (Map.Entry<String, Object> current : params.entrySet()) {
            int remainingParamCapacity = ConfigConstant.MAX_USER_ATTRIBUTES - verifiedParams.size();
            if (remainingParamCapacity == 0) {
                logParametersToDrop(verifiedParams, params);
                return verifiedParams;
            }

            String currentKey = current.getKey();
            Object currentValue = current.getValue();
            Object verifiedValue = verifyParameterAndReturnValue(currentKey, currentValue, methodCalled);
            if (verifiedValue != null) {
                verifiedParams.put(currentKey, verifiedValue);
            }
        }
        return verifiedParams;
    }

    private boolean isAcceptableValueType(Object value) {
        if (!(value instanceof String) && !(value instanceof Number) && !(value instanceof Boolean) &&
                !(value instanceof AtomicBoolean)) {
            return false;
        }
        return true;
    }

    private boolean validateAndLogKeyLength(String key, String methodCalled) {
        try {
            if (key.getBytes(StandardCharsets.UTF_8).length > ConfigConstant.MAX_USER_ATTRIBUTE_SIZE) {
                Agent.LOG.log(Level.FINER,
                        "Unable to add {0} attribute because {1} was invoked with a key longer than {2} bytes. Key is \"{3}\".",
                        getAttributeType(), methodCalled, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE, key);
                return false;
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "Exception while verifying attribute", t);
            return false;
        }
        return true;
    }

    protected String truncateValue(String key, String value, String methodCalled) {
        String truncatedVal = truncateString(value, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE);
        logTruncatedValue(key, value, truncatedVal, methodCalled, ConfigConstant.MAX_USER_ATTRIBUTE_SIZE);
        return truncatedVal;
    }

    protected void logTruncatedValue(String key, String value, String truncatedVal, String methodCalled, int maxAttributeSize) {
        if (!value.equals(truncatedVal)) {
            Agent.LOG.log(Level.FINER,
                    "{0} was invoked with a value longer than {2} bytes for key \"{3}\". The value will be shortened to the first {4} characters.",
                    methodCalled, value, maxAttributeSize, key, truncatedVal.length());
        }
    }

    /**
     * This function truncates a Unicode String so that it can be encoded in maxBytes. It uses the UTF-8 encoding to
     * determine where the String has to be truncated.
     *
     * @param s String to be truncated
     * @param maxBytes Maximum number of bytes in UTF-8 charset encoding
     * @return truncated input string
     */
    public static String truncateString(String s, int maxBytes) {
        int truncatedSize = 0;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // ranges from https://tools.ietf.org/html/rfc3629

            int characterSize; // Character size in bytes
            // first range: 0000 0000-0000 007F
            if (c <= 0x007f) {
                characterSize = 1;
            } else if (c <= 0x07FF) {
                // second range: 0000 0080-0000 07FF
                characterSize = 2;
            } else if (c <= 0xd7ff) {
                // third range: 0000 0800-0000 FFFF
                characterSize = 3;
            } else if (c <= 0xDFFF) {
                // fourth range: 0x10000 to 0x10FFFF
                // this is the surrogate area (D800 <= c <= DFFF) and is used
                // to encode the characters in the fourth range, which require
                // an additional character in the string buffer
                characterSize = 4;
                // skip the additional character used for encoding
                i++;
            } else {
                // remaining third range: DFFF < c <= FFFF
                characterSize = 3;
            }

            if (truncatedSize + characterSize > maxBytes) {
                return s.substring(0, i);
            }
            truncatedSize += characterSize;
        }
        return s;
    }

    private <T> Object verifyValue(T value) {
        if (value instanceof Double && (((Double) value).isInfinite() || ((Double) value).isNaN())) {
            return null;
        }

        if (value instanceof Float && (((Float) value).isInfinite() || ((Float) value).isNaN())) {
            return null;
        }

        if (value instanceof Double || value instanceof Float || value instanceof Long || value instanceof Integer ||
                value instanceof Boolean || value instanceof BigInteger || value instanceof BigDecimal) {
            return (T) value;
        }

        if (value instanceof AtomicInteger) {
            return ((AtomicInteger) value).intValue();
        }

        if (value instanceof AtomicLong) {
            return ((AtomicLong) value).longValue();
        }

        if (value instanceof AtomicBoolean) {
            return ((AtomicBoolean) value).get();
        }

        return null;
    }

    private void logParametersToDrop(Map<String, Object> verified, Map<String, Object> allParams) {
        MapDifference<String, Object> diff = Maps.difference(verified, allParams);
        // attribute map is at the max, return the map of params that can be added
        Agent.LOG.log(Level.FINER,
                "Unable to add attributes for keys \"{0}\" because the limit on {1} attributes has been reached.",
                diff.entriesOnlyOnRight().keySet(), getAttributeType());
    }

}
