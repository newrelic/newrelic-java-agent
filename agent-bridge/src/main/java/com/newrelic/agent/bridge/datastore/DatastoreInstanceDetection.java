/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

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

    public enum MultiHostConfig {
        NONE, FIRST, LAST;

        public static MultiHostConfig getAndValidateFrom(String value) {
            if (value == null) {
                return NONE;
            }
            try {
                return MultiHostConfig.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NONE;
            }
        }
    }

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
            // the only way I've been able to reproduce this is talking via JDBC to an Azure SQL DB from
            // inside of Azure App Services, where we appear to be redirected to a worker host during the connect
            // in  that case, the first detected address always appeared to be the requested address
            // and the 2nd/last address was always the worker
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "Two different addresses detected: previous: {0} and new: {1}", previousAddress, addressToStore);
            String multihostPreferenceAsString = NewRelic.getAgent().getConfig().getValue("datastore_multihost_preference", MultiHostConfig.NONE.name());
            MultiHostConfig multihostPreference = MultiHostConfig.getAndValidateFrom(multihostPreferenceAsString);
            if (MultiHostConfig.FIRST.equals(multihostPreference)) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "Keeping previous address: "+previousAddress);
                return;
            } else if (MultiHostConfig.LAST.equals(multihostPreference)) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "Using new address: "+addressToStore);
                // just keep going, and we'll store the new address
            } else {
                // I cannot discern why this option was necessary, but it has been left in for backward compatibility
                AgentBridge.getAgent().getLogger().log(Level.FINEST, "Clearing address and stopping detection");
                stopDetectingConnectionAddress();
                return;
            }
        }

        AgentBridge.getAgent().getLogger().log(Level.FINEST, "Storing address: {0}", address);
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
