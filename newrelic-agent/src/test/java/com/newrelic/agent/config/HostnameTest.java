/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class HostnameTest {

    @After
    public void after() {
        Hostname.setInUseIpAddress(null);
        Hostname.setIpAddress(new ArrayList<String>());
    }

    @Test
    public void testGetHostname() {
        Assert.assertNotNull(Hostname.getHostname(AgentConfigFactory.createAgentConfig(new HashMap<String, Object>(), null, null)));
    }

    @Test
    public void testGetDisplayHostname() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> hostStuff = new HashMap<>();
        config.put("process_host", hostStuff);
        hostStuff.put("display_name", "food");

        AgentConfig ac = AgentConfigFactory.createAgentConfig(config, null, null);
        Assert.assertEquals("food", Hostname.getDisplayHostname(ac, "bird"));

        hostStuff.clear();
        ac = AgentConfigFactory.createAgentConfig(config, null, null);
        Assert.assertEquals("bird", Hostname.getDisplayHostname(ac, "bird"));
    }

    @Test
    public void testPReferIpv4() {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> hostStuff = new HashMap<>();
        config.put("process_host", hostStuff);
        hostStuff.put("display_name", "food");
        Assert.assertFalse(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));

        hostStuff.put("ipv_preference", "6");
        Assert.assertTrue(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));

        hostStuff.put("ipv_preference", "4");
        Assert.assertFalse(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));

        hostStuff.put("ipv_preference", "newPref");
        Assert.assertFalse(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));

        hostStuff.put("ipv_preference", 6);
        Assert.assertTrue(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));

        hostStuff.put("ipv_preference", 4);
        Assert.assertFalse(Hostname.preferIpv6(AgentConfigFactory.createAgentConfig(config, null, null)));
    }

    private AgentConfig createConfig(Boolean ipv6) {
        Map<String, Object> config = new HashMap<>();
        Map<String, Object> hostStuff = new HashMap<>();
        config.put("process_host", hostStuff);
        String value;
        if (ipv6 == null) {
            value = "nothing";
        } else if (ipv6) {
            value = "6";
        } else {
            value = "4";
        }
        hostStuff.put("ipv_preference", value);
        return AgentConfigFactory.createAgentConfig(config, null, null);
    }

    @Test
    public void testHostNameIpvs() {
        String ipv4Address = Hostname.getInUseIpAddress(createConfig(false));
        String ipv6Address = Hostname.getInUseIpAddress(createConfig(true));

        Assert.assertNotNull("There is no IPv4 mapping for this server. This test can not verify anything.", ipv4Address);
        Assert.assertNotNull("There is no IPv6 mapping for this server. This test can not verify anything.", ipv6Address);

        String actual = Hostname.getInUseIpAddress(createConfig(false));
        Assert.assertNotNull(actual);
        Assert.assertEquals("The hostname did not match.", ipv4Address, actual);
    }
}