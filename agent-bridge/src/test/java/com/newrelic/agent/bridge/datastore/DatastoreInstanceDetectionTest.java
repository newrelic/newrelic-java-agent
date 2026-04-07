/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.NewRelic;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.sql.Connection;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class DatastoreInstanceDetectionTest {

    @Test
    public void testAddConnection() {
        DatastoreInstanceDetection.detectConnectionAddress();
        InetSocketAddress myhost = InetSocketAddress.createUnresolved("myhost", 1234);
        DatastoreInstanceDetection.saveAddress(myhost);
        Connection connection = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connection);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
        assertEquals(myhost, address);
    }

    @Test
    public void testMultipleAddresses() {
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        Connection connection = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connection);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
        assertEquals("myhost", address.getHostName());
        assertEquals(1234, address.getPort());
    }

    @Test
    public void testDifferentAddresses_UseFirst() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue("datastore_multihost_preference",
                DatastoreInstanceDetection.MultiHostConfig.NONE.name())).thenReturn(DatastoreInstanceDetection.MultiHostConfig.FIRST.name());
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            DatastoreInstanceDetection.detectConnectionAddress();
            DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
            DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("someOtherHost", 9128));
            Connection connection = Mockito.mock(Connection.class);
            DatastoreInstanceDetection.associateAddress(connection);
            DatastoreInstanceDetection.stopDetectingConnectionAddress();

            InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
            assertEquals("myhost", address.getHostName());
            assertEquals(1234, address.getPort());
        }
    }

    @Test
    public void testDifferentAddresses_UseLast() {
        Agent mockAgent = Mockito.mock(Agent.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(mockAgent.getConfig().getValue("datastore_multihost_preference",
                DatastoreInstanceDetection.MultiHostConfig.NONE.name())).thenReturn(DatastoreInstanceDetection.MultiHostConfig.LAST.name());
        try (MockedStatic<NewRelic> mockNewRelic = Mockito.mockStatic(NewRelic.class)) {
            mockNewRelic.when(NewRelic::getAgent).thenReturn(mockAgent);
            DatastoreInstanceDetection.detectConnectionAddress();
            DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
            DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("someOtherHost", 9128));
            Connection connection = Mockito.mock(Connection.class);
            DatastoreInstanceDetection.associateAddress(connection);
            DatastoreInstanceDetection.stopDetectingConnectionAddress();

            InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
            assertEquals("someOtherHost", address.getHostName());
            assertEquals(9128, address.getPort());
        }
    }

    @Test
    public void testDifferentAddresses_UseNone() {
        // NONE is the default
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("someOtherHost", 9128));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        Connection connection = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connection);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
        assertNull(address);
    }

    @Test
    public void testThreading() throws Exception {
        PausedConnectThread t1 = new PausedConnectThread("first");
        PausedConnectThread t2 = new PausedConnectThread("second");

        t1.start();
        Thread.sleep(100);
        t2.start();
        Thread.sleep(100);

        t2.goOn();
        Thread.sleep(100);
        t1.goOn();
        Thread.sleep(100);
    }

    private class PausedConnectThread extends Thread {
        String host;
        boolean paused = false;
        PausedConnectThread(String name) {
            this.host = name;
        }
        public void run() {
            DatastoreInstanceDetection.detectConnectionAddress();
            DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved(host, 1234));
            paused = true;
            while (paused) {
                //System.out.println("Paused, waiting 10ms: "+host);
                try { Thread.sleep(10); } catch (Exception e) {}
            }
            Connection connection = Mockito.mock(Connection.class);
            DatastoreInstanceDetection.associateAddress(connection);
            DatastoreInstanceDetection.stopDetectingConnectionAddress();

            InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
            assertEquals(host, address.getHostName());
            assertEquals(1234, address.getPort());

        }
        public void goOn() {
            this.paused = false;
        }
    }

    @Test
    public void testMultipleConnection() {
        DatastoreInstanceDetection.detectConnectionAddress();
        InetSocketAddress pair = InetSocketAddress.createUnresolved("pair", 1234);
        DatastoreInstanceDetection.saveAddress(pair);
        Connection connectionOne = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connectionOne);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        DatastoreInstanceDetection.detectConnectionAddress();
        InetSocketAddress apple = InetSocketAddress.createUnresolved("apple", 9876);
        DatastoreInstanceDetection.saveAddress(apple);
        Connection connectionTwo = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connectionTwo);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        assertEquals(pair, DatastoreInstanceDetection.getAddressForConnection(connectionOne));
        assertEquals(apple, DatastoreInstanceDetection.getAddressForConnection(connectionTwo));
    }

}