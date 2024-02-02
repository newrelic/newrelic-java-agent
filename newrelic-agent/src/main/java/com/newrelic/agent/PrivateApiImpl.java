/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.attributes.AgentAttributeSender;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.environment.Environment;
import com.newrelic.agent.jmx.JmxApiImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.AgentCollectionFactory;
import com.newrelic.api.agent.Logger;

import javax.management.MBeanServer;
import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PrivateApiImpl implements PrivateApi {
    private final AttributeSender attributeSender = new AgentAttributeSender();

    public PrivateApiImpl() {
    }

    public static void initialize(Logger logger) {
        PrivateApiImpl api = new PrivateApiImpl();
        AgentBridge.privateApi = api;
        AgentBridge.asyncApi = new AsyncApiImpl(logger);
        AgentBridge.jmxApi = new JmxApiImpl();
        AgentBridge.collectionFactory = new AgentCollectionFactory();
        AgentBridge.agent = new AgentImpl(logger);
    }

    @Override
    public Closeable addSampler(Runnable sampler, int period, TimeUnit timeUnit) {
        return ServiceFactory.getSamplerService().addSampler(sampler, period, timeUnit);
    }

    @Override
    public void setServerInfo(String serverInfo) {
        Environment env = ServiceFactory.getEnvironmentService().getEnvironment();
        if (!env.getAgentIdentity().isServerInfoSet()) {
            env.setServerInfo(serverInfo);
        }
    }

    @Override
    public void addMBeanServer(MBeanServer server) {
        ServiceFactory.getJmxService().setJmxServer(server);
    }

    @Override
    public void removeMBeanServer(MBeanServer serverToRemove) {
        ServiceFactory.getJmxService().removeJmxServer(serverToRemove);
    }

    // **************************** Transaction APIs ********************************//

    @Override
    public void addCustomAttribute(String key, String value) {
        attributeSender.addAttribute(key, value, "addCustomAttribute");
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        attributeSender.addAttribute(key, value, "addCustomAttribute");
    }

    @Override
    public void addCustomAttribute(String key, Map<String, String> values) {
        attributeSender.addAttribute(key, values, "addCustomAttribute");
    }

    @Override
    public void addTracerParameter(String key, Number value) {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            currentTxn.getTransactionActivity().getLastTracer().setAgentAttribute(key, value);
        }
    }

    @Override
    public void reportHTTPError(String message, int statusCode, String uri) {
        ServiceFactory.getRPMService().getErrorService().reportHTTPError(message, statusCode, uri);
    }

    @Override
    public void reportException(Throwable throwable) {
        ServiceFactory.getRPMService().getErrorService().reportException(throwable);
    }

    @Override
    public void setAppServerPort(int port) {
        AgentBridge.publicApi.setAppServerPort(port);
    }

    @Override
    public void setServerInfo(String dispatcherName, String version) {
        AgentBridge.publicApi.setServerInfo(dispatcherName, version);
    }

    @Override
    public void setInstanceName(String instanceName) {
        AgentBridge.publicApi.setInstanceName(instanceName);
    }

    /**
     * Allows modules to add strings to a segment in a transaction trace.
     */
    @Override
    public void addTracerParameter(String key, String value) {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            currentTxn.getTransactionActivity().getLastTracer().setAgentAttribute(key, value);
        }
    }

    @Override
    public void addTracerParameter(String key, Map<String, String> values) {
        Transaction currentTxn = Transaction.getTransaction(false);
        if (currentTxn != null) {
            currentTxn.getTransactionActivity().getLastTracer().setAgentAttribute(key, values);
        }
    }

}
