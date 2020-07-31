/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.application.PriorityApplicationName;
import com.newrelic.agent.errors.ErrorService;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

public class MockRPMServiceManager extends AbstractService implements RPMServiceManager {

    private final AtomicReference<ConnectionConfigListener> connectionConfigListener = new AtomicReference<>();
    private final List<ConnectionListener> connectionListeners = new ArrayList<>();
    private AtomicReference<IRPMService> defaultRPMService = new AtomicReference<>();
    private final String defaultAppName;
    private ConcurrentMap<String, IRPMService> rpmServices = new ConcurrentHashMap<>();

    public MockRPMServiceManager() {
        super(RPMServiceManager.class.getSimpleName());
        defaultAppName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
    }

    private IRPMService createRPMService(String appName) {
        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(appName);
        rpmService.setEverConnected(true);
        rpmService.setIsConnected(true);
        ErrorService errorService = new ErrorServiceImpl(appName);
        errorService.addHarvestableToService();

        try {
            errorService.start();
        } catch (Exception e) {
        }
        rpmService.setErrorService(errorService);
        addHarvestablesToServices(appName);
        return rpmService;
    }

    private void addHarvestablesToServices(String appName) {
        if (ServiceFactory.getServiceManager().getInsights() != null) {
            ServiceFactory.getServiceManager().getInsights().addHarvestableToService(appName);
        }
        if (ServiceFactory.getTransactionEventsService() != null) {
            ServiceFactory.getTransactionEventsService().addHarvestableToService(appName);
        }
    }

    @Override
    public IRPMService getOrCreateRPMService(String appName) {
        IRPMService rpmService = rpmServices.get(appName);
        if (rpmService != null) {
            return rpmService;
        }
        rpmService = createRPMService(appName);
        IRPMService oldRPMService = rpmServices.putIfAbsent(appName, rpmService);
        return oldRPMService == null ? rpmService : oldRPMService;
    }

    @Override
    public IRPMService getOrCreateRPMService(PriorityApplicationName appName) {
        return getOrCreateRPMService(appName.getName());
    }

    @Override
    public IRPMService getRPMService() {
        IRPMService rpmService = defaultRPMService.get();
        if (rpmService != null) {
            return rpmService;
        }

        if (defaultAppName == null) {
            throw new RuntimeException("defaultAppName must be set in the test.");
        }
        IRPMService createdService = createRPMService(defaultAppName);
        if (defaultRPMService.compareAndSet(null, createdService)) {
            // we initialized the defaultRPMService, so we should make sure the dictionary of services is updated as well.
            rpmServices.put(createdService.getApplicationName(), createdService);
        }
        return defaultRPMService.get();
    }

    public void setRPMService(IRPMService rpmService) {
        defaultRPMService.set(rpmService);
        if (rpmService.getApplicationName() == null) {
            throw new RuntimeException("You must set the application name on the RPMService first.");
        }
        rpmServices.putIfAbsent(rpmService.getApplicationName(), rpmService);
    }

    @Override
    public IRPMService getRPMService(String appName) {
        if (appName == null || (defaultRPMService.get() != null && defaultRPMService.get().getApplicationName().equals(appName))) {
            return defaultRPMService.get();
        }

        return rpmServices.get(appName);
    }

    @Override
    public void setConnectionConfigListener(ConnectionConfigListener listener) {
        this.connectionConfigListener.compareAndSet(null, listener);
    }

    @Override
    public void addConnectionListener(ConnectionListener listener) {
        connectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    public List<ConnectionListener> getConnectionListeners() {
        return connectionListeners;
    }

    public ConnectionConfigListener getConnectionConfigListener() {
        return connectionConfigListener.get();
    }

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<IRPMService> getRPMServices() {
        return Collections.emptyList();
    }

}
