/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import org.junit.Assert;
import org.junit.Test;

public class AgentIdentityTest {

    @Test
    public void testAgentIdentityInstance() {
        AgentIdentity id = new AgentIdentity("Unknown", null, 8080, "Name");
        AgentIdentity act = id.createWithNewInstanceName("Newer");
        Assert.assertNull(act);
        Assert.assertEquals("Name", id.getInstanceName());

        id = new AgentIdentity("Unknown", null, 8080, null);
        act = id.createWithNewInstanceName("Newer");
        Assert.assertEquals(Integer.valueOf(8080), act.getServerPort());
    }

    @Test
    public void testAgentIdentityPort() {
        AgentIdentity id = new AgentIdentity("Unknown", null, 8081, "Name");
        AgentIdentity act = id.createWithNewServerPort(8080);
        Assert.assertNull(act);
        Assert.assertEquals(Integer.valueOf(8081), id.getServerPort());

        id = new AgentIdentity("Unknown", null, null, null);
        act = id.createWithNewServerPort(8080);
        Assert.assertEquals(Integer.valueOf(8080), act.getServerPort());
    }

    @Test
    public void testAgentIdentityDispatcher() {
        AgentIdentity id = new AgentIdentity("Jetty", "5.5", 8081, "Name");
        AgentIdentity act = id.createWithNewDispatcher("Jetty Web", "6.6");
        Assert.assertNull(act);
        Assert.assertEquals("Jetty", id.getDispatcher());
        Assert.assertEquals("5.5", id.getDispatcherVersion());

        id = new AgentIdentity("Unknown", "6.6", 8080, "Name");
        act = id.createWithNewDispatcher("Jetty Web", "5.5");
        Assert.assertEquals("Jetty Web", act.getDispatcher());
        Assert.assertEquals("6.6", act.getDispatcherVersion());

        id = new AgentIdentity(null, "6.6", 8080, "Name");
        act = id.createWithNewDispatcher("Jetty Web", "5.5");
        Assert.assertEquals("Jetty Web", act.getDispatcher());
        Assert.assertEquals("6.6", act.getDispatcherVersion());

        id = new AgentIdentity("Jetty Web", null, 8080, "Name");
        act = id.createWithNewDispatcher("Jetty Web", "5.5");
        Assert.assertEquals("Jetty Web", act.getDispatcher());
        Assert.assertEquals("5.5", act.getDispatcherVersion());

        id = new AgentIdentity("Unknown", "5.5", 8080, "Name");
        act = id.createWithNewDispatcher(null, "7.7");
        Assert.assertEquals("Unknown", act.getDispatcher());
        Assert.assertEquals("5.5", act.getDispatcherVersion());
    }

    @Test
    public void testAgentIdentityNotSetAtAll() {
        AgentIdentity id = new AgentIdentity("Unknown", null, 8080, "Name");
        AgentIdentity act = id.createWithNewDispatcher("Hello", "4.4");
        Assert.assertEquals("Hello", act.getDispatcher());
        Assert.assertEquals("4.4", act.getDispatcherVersion());

        id = new AgentIdentity(null, null, 8080, "Name");
        act = id.createWithNewDispatcher("Hello1", "4.5");
        Assert.assertEquals("Hello1", act.getDispatcher());
        Assert.assertEquals("4.5", act.getDispatcherVersion());
    }
}
