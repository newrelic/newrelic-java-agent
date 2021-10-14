/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.event.CommandListener;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.mongodb")
public class MongoClientSettingsTest {

    @Test
    public void testMultiBuildOneListener() throws Exception {
        MongoClientSettings.Builder builder = MongoClientSettings.builder();

        MongoClientSettings options1 = builder.build();
        MongoClientSettings options2 = builder.build();

        List<CommandListener> commandListeners1 = options1.getCommandListeners();
        List<CommandListener> commandListeners2 = options2.getCommandListeners();
        assertEquals(1, commandListeners1.size());
        assertEquals(1, commandListeners2.size());
        assertSame(commandListeners1.get(0), commandListeners2.get(0));
    }
}
