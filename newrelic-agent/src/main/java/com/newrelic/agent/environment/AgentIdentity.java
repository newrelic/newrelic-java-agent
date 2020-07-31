/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import java.util.logging.Level;

import com.newrelic.agent.Agent;

/**
 * This object encapsulates the key environment variables that identify an application to the New Relic service. If any
 * of these change and the agent has already established an agent run it must reconnect.
 */
public class AgentIdentity {

    private static final String UNKNOWN_DISPATCHER = "Unknown";
    private final String dispatcher;
    private final String dispatcherVersion;

    private final Integer serverPort;

    private final String instanceName;

    public AgentIdentity(String dispatcher, String dispatcherVersion, Integer serverPort, String instanceName) {
        super();
        this.dispatcher = dispatcher == null ? UNKNOWN_DISPATCHER : dispatcher;
        this.dispatcherVersion = dispatcherVersion;
        this.serverPort = serverPort;
        this.instanceName = instanceName;
    }

    public String getDispatcher() {
        return dispatcher;
    }

    public String getDispatcherVersion() {
        return dispatcherVersion;
    }

    /**
     * Returns the port number of the instrumented application server, or null if it is unknown.
     * 
     */
    public Integer getServerPort() {
        return serverPort;
    }

    /**
     * Returns the instanceName of the instrumented application server, or null if it is unknown.
     * 
     */
    public String getInstanceName() {
        return instanceName;
    }

    public boolean isServerInfoSet() {
        return dispatcher != null && !UNKNOWN_DISPATCHER.equals(dispatcher) && dispatcherVersion != null;
    }

    private boolean isDispatcherNameNotSet() {
        return dispatcher == null || UNKNOWN_DISPATCHER.equals(dispatcher);
    }

    private boolean isDispatcherVersionNotSet() {
        return dispatcherVersion == null;
    }

    /**
     * Creates a new agent identity with the given port or null if the port is already set.
     */
    public AgentIdentity createWithNewServerPort(Integer port) {
        if (serverPort == null) {
            return new AgentIdentity(dispatcher, dispatcherVersion, port, instanceName);
        }
        if (!serverPort.equals(port)) {
            Agent.LOG.log(Level.FINER, "Port is already {0}.  Ignore call to set it to {1}.", serverPort, port);
        }
        return null;
    }

    /**
     * Creates a new agent identity with the given port or null if the port is already set.
     */
    public AgentIdentity createWithNewInstanceName(String name) {
        if (instanceName == null) {
            return new AgentIdentity(dispatcher, dispatcherVersion, serverPort, name);
        }
        if (!instanceName.equals(name)) {
            Agent.LOG.log(Level.FINER, "Instance Name is already {0}.  Ignore call to set it to {1}.", instanceName,
                    name);
        }
        return null;
    }

    /**
     * Creates a new agent identity with the given dispatcher info or null if the dispatcher info is already set.
     * 
     */
    public AgentIdentity createWithNewDispatcher(String dispatcherName, String version) {
        if (isServerInfoSet()) {
            Agent.LOG.log(Level.FINER, "Dispatcher is already {0}:{1}.  Ignore call to set it to {2}:{3}.",
                    getDispatcher(), getDispatcherVersion(), dispatcherName, version);
            return null;
            /*
             * Originally this code reset the name and version if one or the other was not set. However, this causes the
             * Dispatcher name to potentially be changed mid process. The dispatcher name is part of the agent id,
             * meaning the UI creates a new JVM and it looks like you are running two JVMs instead of 1. Therefore we do
             * not change the name if name has already been set. We now do not change version if a version has already
             * been set.
             */
        } else if (isDispatcherNameNotSet() && isDispatcherVersionNotSet()) {

            if (dispatcherName == null) {
                dispatcherName = this.dispatcher;
            }
            if (version == null) {
                version = this.dispatcherVersion;
            }

            return new AgentIdentity(dispatcherName, version, serverPort, instanceName);
        } else if (isDispatcherNameNotSet()) {
            Agent.LOG.log(Level.FINER,
                    "Dispatcher previously set to {0}:{1}. Ignoring new version {3} but setting name to {2}.",
                    getDispatcher(), getDispatcherVersion(), dispatcherName, version);
            return createWithNewDispatcherName(dispatcherName);
        } else {
            // only the dispatcher version is not set
            Agent.LOG.log(Level.FINER,
                    "Dispatcher previously set to {0}:{1}. Ignoring new name {2} but setting version to {3}.",
                    getDispatcher(), getDispatcherVersion(), dispatcherName, version);
            return createWithNewDispatcherVersion(version);
        }
    }


    private AgentIdentity createWithNewDispatcherVersion(String version) {
        return new AgentIdentity(dispatcher, version, serverPort, instanceName);
    }

    private AgentIdentity createWithNewDispatcherName(String name) {
        // leave the unknown if the input is null
        if (name == null) {
            name = this.dispatcher;
        }

        return new AgentIdentity(name, dispatcherVersion, serverPort, instanceName);
    }

}
