/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.ForceDisconnectException;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class LaspPolicies {

    public static final String LASP_RECORD_SQL = "record_sql";
    public static final String LASP_ATTRIBUTES_INCLUDE = "attributes_include";
    public static final String LASP_ALLOW_RAW_EXCEPTION_MESSAGES = "allow_raw_exception_messages";
    public static final String LASP_CUSTOM_EVENTS = "custom_events";
    public static final String LASP_CUSTOM_PARAMETERS = "custom_parameters";
    public static final String LASP_CUSTOM_INSTRUMENTATION_EDITOR = "custom_instrumentation_editor";
    public static final String LASP_MESSAGE_PARAMETERS = "message_parameters";
    public static final String ENABLED = "enabled";
    public static final String REQUIRED = "required";

    private static final Set<String> KNOWN_LASP_POLICIES = new HashSet<>(
            Arrays.asList(LASP_RECORD_SQL, LASP_ATTRIBUTES_INCLUDE, LASP_ALLOW_RAW_EXCEPTION_MESSAGES, LASP_CUSTOM_EVENTS,
                    LASP_CUSTOM_PARAMETERS, LASP_CUSTOM_INSTRUMENTATION_EDITOR, LASP_MESSAGE_PARAMETERS));

    private LaspPolicies() {
    }

    public static Map<String, Boolean> validatePolicies(JSONObject policies) throws ForceDisconnectException {
        if (policies == null) {
            return Collections.emptyMap();
        }

        Agent.LOG.log(Level.INFO, "LASP Policies received from server side: {0}", policies);

        Map<String, Boolean> validatedPolicies = new HashMap<>();
        Set<Map.Entry<String, JSONObject>> policyEntries = policies.entrySet();
        for (Map.Entry<String, JSONObject> entry : policyEntries) {
            String key = entry.getKey();
            JSONObject value = entry.getValue();
            if ((Boolean) value.get(REQUIRED) && !KNOWN_LASP_POLICIES.contains(key)) {
                throw new ForceDisconnectException("Found unknown policy that is required: " + key);
            }
            // only store known/applicable policies
            if (KNOWN_LASP_POLICIES.contains(key)) {
                validatedPolicies.put(key, (Boolean) value.get(ENABLED));
            }
        }

        // check to see if known policies is missing a policy the agent expected
        for (String knownPolicy : KNOWN_LASP_POLICIES) {
            if (!validatedPolicies.containsKey(knownPolicy)) {
                throw new ForceDisconnectException("Did not receive all policies on preconnect");
            }
        }

        return validatedPolicies;
    }

    public static Map<String, Object> convertToConnectPayload(Map<String, Boolean> policiesJson) {
        Map<String, Object> connectPayload = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : policiesJson.entrySet()) {
            JSONObject value = new JSONObject();
            value.put("enabled", entry.getValue());
            connectPayload.put(entry.getKey(), value);
        }
        return connectPayload;
    }

}
