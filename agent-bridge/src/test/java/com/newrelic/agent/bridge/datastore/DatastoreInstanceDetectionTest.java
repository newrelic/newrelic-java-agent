/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import org.junit.Test;
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
    public void testDifferentAddresses() {
        DatastoreInstanceDetection.detectConnectionAddress();
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("someOtherHost", 9128));
        DatastoreInstanceDetection.saveAddress(InetSocketAddress.createUnresolved("myhost", 1234));
        Connection connection = Mockito.mock(Connection.class);
        DatastoreInstanceDetection.associateAddress(connection);
        DatastoreInstanceDetection.stopDetectingConnectionAddress();

        InetSocketAddress address = DatastoreInstanceDetection.getAddressForConnection(connection);
        assertEquals("myhost", address.getHostName());
        assertEquals(1234, address.getPort());
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