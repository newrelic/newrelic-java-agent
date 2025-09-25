/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class needs to be accessible by instrumentation loaded on the bootstrap loader (jdbc-socket instrumentation),
 * that's why this isn't in agent-bridge-datastore.
 *
 * To support Java 9+, this class has no java.sql dependencies.
 */
public final class DatastoreInstanceDetection {

    private DatastoreInstanceDetection() {
    }

    private enum ConnectionState {
        DO_NOT_DETECT_ADDRESS, DETECT_ADDRESS,
    }

    private static ThreadLocal<ConnectionState> state = new ThreadLocal<ConnectionState>() {
        @Override
        protected ConnectionState initialValue() {
            return ConnectionState.DO_NOT_DETECT_ADDRESS;
        }
    };

    private static final ThreadLocal<InetSocketAddress> address = new ThreadLocal<>();
    private static final Map<Object, InetSocketAddress> connectionToAddress = AgentBridge.collectionFactory.createConcurrentWeakKeyedMap();

    /**
     * @return true if we should be detecting the address of a connection, false otherwise.
     */
    public static boolean shouldDetectConnectionAddress() {
        return ConnectionState.DETECT_ADDRESS.equals(DatastoreInstanceDetection.state.get());
    }

    /**
     * Stop detecting the address of a connection.
     */
    public static void stopDetectingConnectionAddress() {
        DatastoreInstanceDetection.state.set(ConnectionState.DO_NOT_DETECT_ADDRESS);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Not detecting connection address");
        clearAddress();
    }

    /**
     * Detect address of a connection.
     */
    public static void detectConnectionAddress() {
        DatastoreInstanceDetection.state.set(ConnectionState.DETECT_ADDRESS);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Detecting connection address");
    }

    /**
     * Associate last detected address with a connection.
     *
     * @param connection Connection to associate with last detected address.
     */
    public static void associateAddress(Object connection) {
        associateAddress(connection, getCurrentAddress());
    }

    // public for testing only
    public static void associateAddress(Object connection, InetSocketAddress addressToStore) {
        if (ConnectionState.DO_NOT_DETECT_ADDRESS.equals(state.get())) {
            return;
        }

        if (connection != null && addressToStore != null) {
            DatastoreInstanceDetection.connectionToAddress.put(connection, addressToStore);
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Added connection: {0} for address: {1}", connection,
                    addressToStore);
        } else {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "Unable to add address: {0} for connection: {1}",
                    addressToStore, connection);
        }
    }

    public static void saveAddress(InetSocketAddress addressToStore) {
        if (addressToStore == null || ConnectionState.DO_NOT_DETECT_ADDRESS.equals(state.get())) {
            return;
        }

        InetSocketAddress previousAddress = address.get();
        if (previousAddress != null && !previousAddress.equals(addressToStore)) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "JGB: Two different addresses detected: {0} and {1}. Using originally detected address.",
                    previousAddress, addressToStore);
            //stopDetectingConnectionAddress();
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "JGB: Keeping previous address");
            //DatastoreInstanceDetection.address.set(previousAddress);
            return;
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing address: {0}", addressToStore);
        DatastoreInstanceDetection.address.set(addressToStore);
    }

    /**
     * Clear detected address.
     */
    public static void clearAddress() {
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Clearing last detected address");
        DatastoreInstanceDetection.address.set(null);
    }

    /**
     * @return address detected.
     */
    public static InetSocketAddress getCurrentAddress() {
        return address.get();
    }

    /**
     * Get address detected for this connection.
     *
     * @param connection Connection to lookup
     * @return address address detected for the connection, null if no address was not detected for this connection.
     */
    public static InetSocketAddress getAddressForConnection(Object connection) {
        if (connection == null) {
            return null;
        }

        InetSocketAddress address = DatastoreInstanceDetection.connectionToAddress.get(connection);
        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Address for connection: {0} is: {1}", connection, address);
        return address;
    }

}
