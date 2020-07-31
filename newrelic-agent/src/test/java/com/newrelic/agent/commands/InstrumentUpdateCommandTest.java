/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentUpdateCommandTest {

    @Test
    public void testParseAgentInstrumentationOkay() {
        Map<String, String> inner = new HashMap<>();
        inner.put(InstrumentUpdateCommand.ARG_VALUE_MAP_NAME, "the xml");

        Map<String, Object> outer = new HashMap<>();
        outer.put(InstrumentUpdateCommand.ARG_NAME, inner);

        Assert.assertNotNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }

    @Test
    public void testParseAgentInstrumentationDifferentObject() {
        List<String> inner = new ArrayList<>();
        inner.add("the xml");

        Map<String, Object> outer = new HashMap<>();
        outer.put(InstrumentUpdateCommand.ARG_NAME, inner);

        Assert.assertNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }

    @Test
    public void testParseAgentInstrumentationNoConfig() {
        Map<String, String> inner = new HashMap<>();

        Map<String, Object> outer = new HashMap<>();
        outer.put(InstrumentUpdateCommand.ARG_NAME, inner);

        Assert.assertNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }

    @Test
    public void testParseAgentInstrumentationXmlNull() {
        Map<String, String> inner = new HashMap<>();
        inner.put(InstrumentUpdateCommand.ARG_VALUE_MAP_NAME, null);

        Map<String, Object> outer = new HashMap<>();
        outer.put(InstrumentUpdateCommand.ARG_NAME, inner);

        Assert.assertNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }

    @Test
    public void testParseAgentInstrumentationConfigNotString() {
        Map<String, Object> inner = new HashMap<>();
        inner.put(InstrumentUpdateCommand.ARG_VALUE_MAP_NAME, new Object());

        Map<String, Object> outer = new HashMap<>();
        outer.put(InstrumentUpdateCommand.ARG_NAME, inner);

        Assert.assertNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }

    @Test
    public void testParseAgentInstrumentationOuterMapMissingCommand() {

        Map<String, Object> outer = new HashMap<>();

        Assert.assertNull(InstrumentUpdateCommand.getXmlFromMaps(outer));
    }
}
