/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.spy.memcached;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MemcachedUtilTest {

    private static final String ENDPOINT = "my-cache-abc123.serverless.use1.cache.amazonaws.com";

    @Test
    public void configuredHostIsReportedForIpOnlyNodeAddress() throws Exception {
        byte[] ip = new byte[] { (byte) 172, 31, 1, 2 };
        InetSocketAddress configuredAddress = new InetSocketAddress(InetAddress.getByAddress(ENDPOINT, ip), 11211);
        InetSocketAddress nodeAddress = new InetSocketAddress(InetAddress.getByAddress(ip), 11211);

        Map<InetSocketAddress, String> configuredHosts = MemcachedUtil.getConfiguredHosts(Collections.singletonList(configuredAddress));

        assertEquals(ENDPOINT, MemcachedUtil.getHost(nodeAddress, configuredHosts));
    }

    @Test
    public void nodeHostStringIsReportedWhenNotConfigured() throws Exception {
        InetSocketAddress configuredAddress = new InetSocketAddress(InetAddress.getByAddress(ENDPOINT, new byte[] { 10, 0, 0, 1 }), 11211);
        InetSocketAddress nodeAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 10, 0, 0, 2 }), 11211);

        Map<InetSocketAddress, String> configuredHosts = MemcachedUtil.getConfiguredHosts(Collections.singletonList(configuredAddress));

        assertEquals("10.0.0.2", MemcachedUtil.getHost(nodeAddress, configuredHosts));
    }

    @Test
    public void differentPortIsNotMatched() throws Exception {
        byte[] ip = new byte[] { 10, 0, 0, 1 };
        InetSocketAddress configuredAddress = new InetSocketAddress(InetAddress.getByAddress(ENDPOINT, ip), 11211);
        InetSocketAddress nodeAddress = new InetSocketAddress(InetAddress.getByAddress(ip), 11212);

        Map<InetSocketAddress, String> configuredHosts = MemcachedUtil.getConfiguredHosts(Collections.singletonList(configuredAddress));

        assertEquals("10.0.0.1", MemcachedUtil.getHost(nodeAddress, configuredHosts));
    }

    @Test
    public void unresolvedNodeAddressReportsConfiguredHostname() {
        InetSocketAddress address = InetSocketAddress.createUnresolved(ENDPOINT, 11211);

        Map<InetSocketAddress, String> configuredHosts = MemcachedUtil.getConfiguredHosts(Collections.singletonList(address));

        assertEquals(ENDPOINT, MemcachedUtil.getHost(InetSocketAddress.createUnresolved(ENDPOINT, 11211), configuredHosts));
    }

    @Test
    public void nullNodeAddressReturnsNull() {
        assertNull(MemcachedUtil.getHost(null, Collections.<InetSocketAddress, String>emptyMap()));
    }

    @Test
    public void nullConfiguredHostsFallsBackToHostString() throws Exception {
        InetSocketAddress nodeAddress = new InetSocketAddress(InetAddress.getByAddress(new byte[] { 10, 0, 0, 1 }), 11211);

        assertEquals("10.0.0.1", MemcachedUtil.getHost(nodeAddress, null));
    }

    @Test
    public void nullAndEmptyAddressListsProduceEmptyMap() {
        assertTrue(MemcachedUtil.getConfiguredHosts(null).isEmpty());
        assertTrue(MemcachedUtil.getConfiguredHosts(Collections.<InetSocketAddress>emptyList()).isEmpty());
    }

    @Test
    public void nullAddressEntriesAreSkipped() {
        InetSocketAddress address = InetSocketAddress.createUnresolved(ENDPOINT, 11211);
        List<InetSocketAddress> addresses = Arrays.asList(null, address);

        Map<InetSocketAddress, String> configuredHosts = MemcachedUtil.getConfiguredHosts(addresses);

        assertEquals(1, configuredHosts.size());
        assertEquals(ENDPOINT, configuredHosts.get(address));
    }
}
