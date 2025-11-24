/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manage RPM services and notify listeners when an RPM service connects.
 *
 * This class is thread-safe.
 */
public class RPMServiceManagerImpl extends AbstractService implements RPMServiceManager {

    private final IRPMService defaultRPMService;
    private final Map<String, IRPMService> appNameToRPMService = new ConcurrentHashMap<>();
    private final AtomicReference<ConnectionConfigListener> connectionConfigListenerRef = new AtomicReference<>(null);
    private final ConnectionConfigListener connectionConfigListener;
    private final List<ConnectionListener> connectionListeners = new CopyOnWriteArrayList<>();
    private final ConnectionListener connectionListener;
    private final List<AgentConnectionEstablishedListener> agentConnectionEstablishedListeners;
    // for efficiency: an unmodifiable list of all RPM services
    private volatile List<IRPMService> rpmServices;

    public RPMServiceManagerImpl(AgentConnectionEstablishedListener... agentConnectionEstablishedListeners) {
        super(RPMServiceManager.class.getSimpleName());
        this.agentConnectionEstablishedListeners = new ArrayList<>(Arrays.asList(agentConnectionEstablishedListeners));
        connectionConfigListener = new ConnectionConfigListener() {
            @Override
            public AgentConfig connected(IRPMService rpmService, Map<String, Object> connectionInfo) {
                ConnectionConfigListener listener = connectionConfigListenerRef.get();
                if (listener != null) {
                    return listener.connected(rpmService, connectionInfo);
                }
                return ServiceFactory.getConfigService().getDefaultAgentConfig();
            }
        };

        connectionListener = new ConnectionListener() {

            @Override
            public void connected(IRPMService rpmService, AgentConfig agentConfig) {
                for (ConnectionListener each : connectionListeners) {
                    each.connected(rpmService, agentConfig);
                }
            }

            @Override
            public void disconnected(IRPMService rpmService) {
                for (ConnectionListener each : connectionListeners) {
                    each.disconnected(rpmService);
                }
            }

        };

        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();

        if (config.getServerlessConfig().isEnabled()) {
            getLogger().config("Configured to connect to New Relic via serverless layer");
        } else {
            String host = config.getHost();
            String port = Integer.toString(config.getPort());
            getLogger().config(MessageFormat.format("Configured to connect to New Relic at {0}:{1}", host, port));
        }
        defaultRPMService = createRPMService(config.getApplicationNames(), connectionConfigListener, connectionListener);
        List<IRPMService> list = new ArrayList<>(1);
        list.add(defaultRPMService);
        rpmServices = Collections.unmodifiableList(list);
    }

    @Override
    protected synchronized void doStart() throws Exception {
        for (IRPMService rpmService : rpmServices) {
            rpmService.start();
        }
    }

    @Override
    protected synchronized void doStop() throws Exception {
        for (IRPMService rpmService : rpmServices) {
            rpmService.stop();
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void setConnectionConfigListener(ConnectionConfigListener listener) {
        connectionConfigListenerRef.compareAndSet(null, listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    @Override
    public IRPMService getRPMService() {
        return defaultRPMService;
    }

    @Override
    public IRPMService getRPMService(String appName) {
        if (appName == null || defaultRPMService.getApplicationName().equals(appName)) {
            return defaultRPMService;
        }
        return appNameToRPMService.get(appName);
    }

    @Override
    public IRPMService getOrCreateRPMService(PriorityApplicationName appName) {
        IRPMService rpmService = getRPMService(appName.getName());
        if (rpmService != null) {
            return rpmService;
        }
        return createRPMServiceForAppName(appName.getName(), appName.getNames());
    }

    @Override
    public IRPMService getOrCreateRPMService(String appName) {
        IRPMService rpmService = getRPMService(appName);
        if (rpmService != null) {
            return rpmService;
        }
        List<String> appNames = new ArrayList<>(1);
        appNames.add(appName);
        return createRPMServiceForAppName(appName, appNames);
    }

    private synchronized IRPMService createRPMServiceForAppName(String appName, List<String> appNames) {
        IRPMService rpmService = getRPMService(appName);
        if (rpmService == null) {
            rpmService = createRPMService(appNames, connectionConfigListener, connectionListener);
            appNameToRPMService.put(appName, rpmService);
            List<IRPMService> list = new ArrayList<>(appNameToRPMService.size() + 1);
            list.addAll(appNameToRPMService.values());
            list.add(defaultRPMService);
            rpmServices = Collections.unmodifiableList(list);
            if (isStarted()) {
                try {
                    rpmService.start();
                } catch (Exception e) {
                    String msg = MessageFormat.format("Error starting New Relic Service for {0}: {1}", rpmService.getApplicationName(), e);
                    getLogger().severe(msg);
                }
            }
        }
        return rpmService;
    }

    protected IRPMService createRPMService(List<String> appNames, ConnectionConfigListener connectionConfigListener,
                                           ConnectionListener connectionListener) {
        return new RPMService(appNames, connectionConfigListener, connectionListener, null, agentConnectionEstablishedListeners);
    }

    @Override
    public List<IRPMService> getRPMServices() {
        return rpmServices;
    }

}
