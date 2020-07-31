/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.ForceDisconnectException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import static org.junit.Assert.fail;

public class LaspPoliciesConfigTest {

    @Test
    public void noDisconnect() throws ParseException, ForceDisconnectException {
        String policiesString = "{" +
                "\"record_sql\":                    { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false },\n" +
                "\"allow_raw_exception_messages\":  { \"enabled\": false, \"required\": false },\n" +
                "\"custom_events\":                 { \"enabled\": false, \"required\": false },\n" +
                "\"custom_parameters\":             { \"enabled\": false, \"required\": false },\n" +
                "\"custom_instrumentation_editor\": { \"enabled\": false, \"required\": false },\n" +
                "\"message_parameters\":            { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false },\n" +
                "\"job_arguments\":                 { \"enabled\": false, \"required\": false }}";

        JSONObject policiesJson = createPolicies(policiesString);

        try {
            LaspPolicies.validatePolicies(policiesJson);
        } catch (ForceDisconnectException ex) {
            fail();
        }
    }

    @Test
    public void knownRequired() throws ParseException, ForceDisconnectException {
        String policiesString = "{" +
                "\"record_sql\":                    { \"enabled\": false, \"required\": true },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": true },\n" +
                "\"allow_raw_exception_messages\":  { \"enabled\": false, \"required\": true },\n" +
                "\"custom_events\":                 { \"enabled\": false, \"required\": true },\n" +
                "\"custom_parameters\":             { \"enabled\": false, \"required\": true },\n" +
                "\"custom_instrumentation_editor\": { \"enabled\": false, \"required\": true },\n" +
                "\"message_parameters\":            { \"enabled\": false, \"required\": true },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": true }}";

        JSONObject policiesJson = createPolicies(policiesString);

        try {
            LaspPolicies.validatePolicies(policiesJson);
        } catch (ForceDisconnectException ex) {
            fail();
        }
    }

    @Test
    public void emptyList() throws ParseException, ForceDisconnectException {
        try {
            LaspPolicies.validatePolicies(new JSONObject());
            fail();
        } catch (ForceDisconnectException ex) {
        }
    }

    @Test
    public void unknownRequired() throws ParseException {
        String policiesString = "{" +
                "\"totally_required_yo\":      { \"enabled\": true, \"required\": true },\n" +
                "\"record_sql\":                    { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false },\n" +
                "\"allow_raw_exception_messages\":  { \"enabled\": false, \"required\": false },\n" +
                "\"custom_events\":                 { \"enabled\": false, \"required\": false },\n" +
                "\"custom_parameters\":             { \"enabled\": false, \"required\": false },\n" +
                "\"custom_instrumentation_editor\": { \"enabled\": false, \"required\": false },\n" +
                "\"message_parameters\":            { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false },\n" +
                "\"job_arguments\":                 { \"enabled\": false, \"required\": false }}";

        JSONObject policiesJson = createPolicies(policiesString);

        try {
            LaspPolicies.validatePolicies(policiesJson);
            fail();
        } catch (ForceDisconnectException ex) {
        }
    }

    @Test
    public void jobArgumentsRequired() throws ParseException {
        String policiesString = "{" +
                "\"job_arguments\":      { \"enabled\": true, \"required\": true },\n" +
                "\"record_sql\":                    { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false },\n" +
                "\"allow_raw_exception_messages\":  { \"enabled\": false, \"required\": false },\n" +
                "\"custom_events\":                 { \"enabled\": false, \"required\": false },\n" +
                "\"custom_parameters\":             { \"enabled\": false, \"required\": false },\n" +
                "\"custom_instrumentation_editor\": { \"enabled\": false, \"required\": false },\n" +
                "\"message_parameters\":            { \"enabled\": false, \"required\": false },\n" +
                "\"attributes_include\":            { \"enabled\": false, \"required\": false }}";

        JSONObject policiesJson = createPolicies(policiesString);

        try {
            LaspPolicies.validatePolicies(policiesJson);
            fail();
        } catch (ForceDisconnectException ex) {
        }
    }

    @Test
    public void missingKnownPolicies() throws ParseException {
        String policiesString = "{" +
                "\"custom_events\":            { \"enabled\": true, \"required\": false }}";

        JSONObject policiesJson = createPolicies(policiesString);

        try {
            LaspPolicies.validatePolicies(policiesJson);
            fail();
        } catch (ForceDisconnectException ex) {
        }
    }

    public static JSONObject createPolicies(String policies) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(policies);
        return json;
    }

}
