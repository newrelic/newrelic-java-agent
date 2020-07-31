/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.commands;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.MockRPMService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class CommandTest {

    @Test
    public void restart() throws Exception {
        MockRPMService rpmService = new MockRPMService() {

            @Override
            public void reconnect() {
                throw new RuntimeException("restart");
            }
        };
        MockCoreService executor = new MockCoreService();
        Assert.assertEquals(Collections.EMPTY_MAP, new ShutdownCommand(executor).process(rpmService,
                Collections.EMPTY_MAP));
        try {
            new RestartCommand().process(rpmService, Collections.EMPTY_MAP);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("restart", e.getMessage());
        }
    }

    @Test
    public void shutdown() throws Exception {
        MockRPMService rpmService = new MockRPMService();

        MockCoreService executor = new MockCoreService() {

            @Override
            public void shutdownAsync() {
                throw new RuntimeException("shutdown");
            }

        };

        Assert.assertEquals(Collections.EMPTY_MAP, new RestartCommand().process(rpmService, Collections.EMPTY_MAP));
        try {
            new ShutdownCommand(executor).process(rpmService, Collections.EMPTY_MAP);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("shutdown", e.getMessage());
        }
    }
}
