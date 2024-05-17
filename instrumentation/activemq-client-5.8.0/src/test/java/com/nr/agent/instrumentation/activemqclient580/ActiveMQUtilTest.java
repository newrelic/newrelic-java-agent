/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.activemqclient580;

import com.newrelic.agent.bridge.messaging.HostAndPort;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ActiveMQUtilTest {

    @Test
    public void testAwsUrlParsing() {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        final HostAndPort hostAndPort = ActiveMQUtil.get().parseHostAndPort(awsAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPort);

        // Verify caching works
        final HostAndPort hostAndPortRepeated = ActiveMQUtil.get().parseHostAndPort(awsAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPortRepeated);

    }

    @Test
    public void testAwsUrlParsingWithLocalPort() {
        final String awsAddress = "ssl://b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com/174.65.25.235:61617@59925";

        final String expectedHost = "b-cd914095-3880-10d3-bb93-ee07ce1f57a5-1.mq.us-east-2.amazonaws.com";
        final Integer expectedPort = 61617;

        final HostAndPort hostAndPort = ActiveMQUtil.get().parseHostAndPort(awsAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPort);

        // Verify caching works
        final HostAndPort hostAndPortRepeated = ActiveMQUtil.get().parseHostAndPort(awsAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPortRepeated);

    }

    @Test
    public void testLocalhostParsing() {
        final String localhostAddress = "tcp://localhost/127.0.0.1:61616@59925";

        final String expectedHost = "localhost";
        final Integer expectedPort = 61616;

        final HostAndPort hostAndPort = ActiveMQUtil.get().parseHostAndPort(localhostAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPort);

        // Verify caching works
        final HostAndPort hostAndPortRepeated = ActiveMQUtil.get().parseHostAndPort(localhostAddress);
        assertHostAndPort(expectedHost, expectedPort, hostAndPortRepeated);
    }

    private void assertHostAndPort(final String expectedHostName, final Integer expectedPort, final HostAndPort hostAndPort) {
        assertNotNull(hostAndPort);
        assertEquals(expectedHostName, hostAndPort.getHostName());
        assertEquals(expectedPort, hostAndPort.getPort());
    }
}
