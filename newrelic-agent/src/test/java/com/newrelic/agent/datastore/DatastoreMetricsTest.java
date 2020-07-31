/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.datastore;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.database.DatastoreMetrics;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;

import static org.junit.Assert.*;

public class DatastoreMetricsTest {

    @Before
    public void before() throws Exception {
        AgentHelper.bootstrap(AgentHelper.createAgentConfig(true));
    }

    @Test
    public void testReplaceIdentifier() throws Exception {
        assertEquals(DatastoreMetrics.UNKNOWN, DatastoreMetrics.replaceIdentifier(null));
        assertEquals("myIdentifier", DatastoreMetrics.replaceIdentifier("myIdentifier"));
    }

    @Test
    public void testReplacePort() throws Exception {
        assertEquals(DatastoreMetrics.UNKNOWN, DatastoreMetrics.replacePort(null));
        assertEquals(DatastoreMetrics.UNKNOWN, DatastoreMetrics.replacePort(-1));
        assertEquals("1234", DatastoreMetrics.replacePort(1234));
    }

    @Test
    public void testReplaceLocalhost() throws Exception {
        assertEquals(DatastoreMetrics.UNKNOWN, DatastoreMetrics.replaceLocalhost(null));

        final String hostname = InetAddress.getLocalHost().getHostName();

        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("localhost")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("127.0.0.1")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("0.0.0.0")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("0:0:0:0:0:0:0:1")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("::1")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("0:0:0:0:0:0:0:0")));
        assertTrue(hostname.equals(DatastoreMetrics.replaceLocalhost("::")));
    }

    @Test
    public void testBuildInstanceIdentifier() throws Exception {
        String unknownUnknown = "unknown/unknown";
        assertEquals(unknownUnknown, DatastoreMetrics.buildInstanceIdentifier(null, null, null));
        assertEquals(unknownUnknown,(DatastoreMetrics.buildInstanceIdentifier(null, -1, null)));

        assertEquals("myHost/unknown",(DatastoreMetrics.buildInstanceIdentifier("myHost", -1, null)));
        assertEquals("myHost/1234",(DatastoreMetrics.buildInstanceIdentifier("myHost", 1234, null)));
        assertEquals("myHost/identifier",(DatastoreMetrics.buildInstanceIdentifier("myHost", null, "identifier")));
        assertEquals("myHost/identifier",(DatastoreMetrics.buildInstanceIdentifier("myHost", -1, "identifier")));

        final String hostname = InetAddress.getLocalHost().getHostName();
        assertEquals(hostname + "/identifier",(DatastoreMetrics.buildInstanceIdentifier(null, -1, "identifier")));
        assertEquals(hostname +"/identifier",(DatastoreMetrics.buildInstanceIdentifier(null, null, "identifier")));
        assertEquals(hostname + "/identifier",(DatastoreMetrics.buildInstanceIdentifier("localhost", null, "identifier")));

        assertEquals("myhost/identifier",(DatastoreMetrics.buildInstanceIdentifier("myhost", 1234, "identifier")));
    }

    @Test
    public void testBuildInstanceMetric() throws Exception {
        assertEquals("Datastore/instance/YesSql/unknown/unknown", DatastoreMetrics.buildInstanceMetric("YesSql",null, null, null));
        assertEquals("Datastore/instance/YesSql/unknown/1234", DatastoreMetrics.buildInstanceMetric("YesSql",null, 1234, null));
        assertEquals("Datastore/instance/YesSql/myHost/unknown", DatastoreMetrics.buildInstanceMetric("YesSql","myHost", null, null));
        assertEquals("Datastore/instance/YesSql/myHost/9875", DatastoreMetrics.buildInstanceMetric("YesSql","myHost", 9875, null));
        assertEquals("Datastore/instance/YesSql/myHost/identifier", DatastoreMetrics.buildInstanceMetric("YesSql","myHost", null, "identifier"));


        final String hostname = InetAddress.getLocalHost().getHostName();
        // The following are really edge cases. They check that:
        // 1, 2) an identifier always wins over port. In practice, we should never get both a port and an identifier.
        // We protect against this by providing two different APIs that accept either a host and port,
        // or host and identifier, but never port and identifier.
        assertEquals("Datastore/instance/YesSql/" + hostname + "/identifier", DatastoreMetrics.buildInstanceMetric("YesSql",null, 1234, "identifier"));
        assertEquals("Datastore/instance/YesSql/myHost/identifier", DatastoreMetrics.buildInstanceMetric("YesSql","myHost", 9875, "identifier"));

        assertEquals("Datastore/instance/YesSql/" + hostname + "/identifier", DatastoreMetrics.buildInstanceMetric("YesSql",null, null, "identifier"));
        assertEquals("Datastore/instance/YesSql/" + hostname + "/identifier", DatastoreMetrics.buildInstanceMetric("YesSql","localhost", null, "identifier"));
    }
}