/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Parses labels configuration.
 */
public class LabelsConfigImpl implements LabelsConfig, JSONStreamAware {

    private final Map<String, String> labels = new HashMap<>();

    LabelsConfigImpl(Object labelsObj) {
        parseLabels(labelsObj);
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    @SuppressWarnings("unchecked")
    private void parseLabels(Object labelsObj) {
        if (labelsObj == null) {
            return;
        }

        try {
            if (labelsObj instanceof Map) {
                parseLabelsMap((Map) labelsObj);
            } else if (labelsObj instanceof String) {
                BaseConfig.parseMapEntriesFromString((String) labelsObj, this::addLabelPart);
            }
        } catch (ParseException pe) {
            Agent.LOG.log(Level.WARNING, "Error parsing labels - {0}", pe.getMessage());
            Agent.LOG.log(Level.WARNING, "Labels will not be sent to New Relic");
            labels.clear();
        }
    }

    private void parseLabelsMap(Map<String, Object> labelsMap) throws ParseException {
        for (Map.Entry<String, Object> entry : labelsMap.entrySet()) {
            if (entry.getValue() == null) {
                throw new ParseException("empty value");
            }
            addLabelPart(entry.getKey(), entry.getValue().toString());
        }
    }

    private void addLabelPart(String key, String value) throws ParseException {
        key = validateLabelPart(key);
        value = validateLabelPart(value);

        if (labels.size() == 64) {
            Agent.LOG.log(Level.WARNING, "Exceeded 64 label limit - only the first 64 labels will be sent to New Relic");
            return;
        }

        labels.put(key, value);
    }

    private static String validateLabelPart(String keyOrValue) throws ParseException {
        if (keyOrValue == null || keyOrValue.equals("")) {
            throw new ParseException("empty name or value");
        }

        if (keyOrValue.contains(":") || keyOrValue.contains(";")) {
            throw new ParseException("illegal character ':' or ';' in name or value '" + keyOrValue + "'");
        }

        if (keyOrValue.length() > 255) {
            keyOrValue = keyOrValue.substring(0, 255);
            Agent.LOG.log(Level.WARNING, "Label name or value over 255 characters.  Truncated to ''{0}''.", keyOrValue);
        }

        return keyOrValue;
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        List<JSONStreamAware> jsonLabels = new ArrayList<>(labels.size());
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            final String name = entry.getKey();
            final String value = entry.getValue();
            jsonLabels.add(new JSONStreamAware() {
                @Override
                public void writeJSONString(Writer out) throws IOException {
                    JSONObject.writeJSONString(ImmutableMap.of("label_type", name, "label_value", value), out);
                }
            });
        }
        JSONArray.writeJSONString(jsonLabels, out);
    }

}
