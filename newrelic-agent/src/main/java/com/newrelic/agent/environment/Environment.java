/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.environment;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ObfuscateJvmPropsConfig;
import com.newrelic.agent.samplers.MemorySampler;
import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Environmental information known to the New Relic Java Agent. This class is not thread safe.
 */
public class Environment implements JSONStreamAware, Cloneable {

    private static final String PHYSICAL_CORE_KEY = "Physical Processors";
    private static final String LOGICAL_CORE_KEY = "Logical Processors";
    private static final String TOTAL_MEMORY_MB = "Total Physical Memory (MB)";
    private static final String SOLR_VERSION_KEY = "Solr Version";
    private static final Pattern JSON_WORKAROUND = Pattern.compile("\\\\+$");

    private final List<EnvironmentChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final List<List<?>> environmentMap = new ArrayList<>();
    private volatile AgentIdentity agentIdentity;

    // These are volatile instance variables instead of being in the map
    // because they are set after the Environment constructor, meaning there
    // would be a concurrent issue if put in the environmentMap.

    private volatile Integer physicalCoreCount;
    private volatile Float physicalMemoryMB;
    private volatile Object solrVersion;

    public Environment(AgentConfig config, String logFilePath) {
        if (config.isSendEnvironmentInfo()) {
            OperatingSystemMXBean systemMXBean = ManagementFactory.getOperatingSystemMXBean();

            addVariable(LOGICAL_CORE_KEY, systemMXBean.getAvailableProcessors());
            addVariable("Arch", systemMXBean.getArch());
            addVariable("OS version", systemMXBean.getVersion());
            addVariable("OS", systemMXBean.getName());
            addVariable("Java vendor", System.getProperty("java.vendor"));
            addVariable("Java VM", System.getProperty("java.vm.name"));
            addVariable("Java VM version", System.getProperty("java.vm.version"));
            addVariable("Java version", System.getProperty("java.version"));
            addVariable("Log path", logFilePath);

            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            addVariable("ThreadCpuTimeSupported", threadMXBean.isThreadCpuTimeSupported());
            if (threadMXBean.isThreadCpuTimeSupported()) {
                addVariable("ThreadCpuTimeEnabled", threadMXBean.isThreadCpuTimeEnabled());
            }
            addVariable("CurrentThreadCpuTimeSupported", threadMXBean.isCurrentThreadCpuTimeSupported());
            if (threadMXBean.isCurrentThreadCpuTimeSupported()) {
                addVariable("CurrentThreadCpuTimeEnabled", threadMXBean.isThreadCpuTimeEnabled());
            }
            if (threadMXBean.isThreadContentionMonitoringSupported()) {
                addVariable("ThreadContentionMonitoringEnabled", threadMXBean.isThreadContentionMonitoringEnabled());
            }

            MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            addVariable("Heap initial (MB)", heapMemoryUsage.getInit() / MemorySampler.BYTES_PER_MB);
            addVariable("Heap max (MB)", heapMemoryUsage.getMax() / MemorySampler.BYTES_PER_MB);

            if (config.isSendJvmProps()) {
                RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                List<String> inputArguments = fixInputArguments(runtimeMXBean.getInputArguments());
                ObfuscateJvmPropsConfig obfuscateJvmPropsConfig = config.getObfuscateJvmPropsConfig();
                if (obfuscateJvmPropsConfig.isEnabled()) {
                   Set<String> allow = obfuscateJvmPropsConfig.getAllowedJvmProps();
                    inputArguments = obfuscateProps(inputArguments, allow);
                }
                environmentMap.add(Arrays.asList("JVM arguments", inputArguments));
            }
        }

        String dispatcher = null;
        if (System.getProperty("com.sun.aas.installRoot") != null) {
            dispatcher = "Glassfish";
        } else if (System.getProperty("resin.home") != null) {
            dispatcher = "Resin";
        } else if (System.getProperty("org.apache.geronimo.base.dir") != null) {
            dispatcher = "Apache Geronimo";
        } else if (System.getProperty("weblogic.home") != null) {
            dispatcher = "WebLogic";
        } else if (System.getProperty("wlp.install.dir") != null) {
            dispatcher = "WebSphere Application Server Liberty profile";
        } else if (System.getProperty("was.install.root") != null) {
            dispatcher = "IBM WebSphere Application Server";
        } else if (System.getProperty("jboss.home") != null) {
            dispatcher = "JBoss";
        } else if ((System.getProperty("jboss.home.dir") != null)
                || (System.getProperty("org.jboss.resolver.warning") != null)
                || (System.getProperty("jboss.partition.name") != null)) {
            dispatcher = "JBoss Web";
        } else if (System.getProperty("catalina.home") != null) {
            dispatcher = "Apache Tomcat";
        } else if (System.getProperty("jetty.home") != null) {
            dispatcher = "Jetty";
        }

        // If dispatcher is still null at this point, try to read it from the config
        if (dispatcher == null) {
            dispatcher = config.getProperty("appserver_dispatcher");
        }

        addVariable("Framework", "java");

        Number appServerPort = config.getProperty("appserver_port");
        Integer serverPort = null;
        if (appServerPort != null) {
            serverPort = appServerPort.intValue();
        }

        String instanceName = config.getProperty("instance_name");

        agentIdentity = new AgentIdentity(dispatcher, null, serverPort, instanceName);
    }

    private List<String> obfuscateProps(List<String> inputArguments, Set<String> allow) {
        // This should read the list of inputArguments and obfuscate it according to the allow list scheme
        List<String> sanitizedProps = new ArrayList<>();
        for (String arg: inputArguments) {
            String tmpArg = arg;
            if (shouldObfuscate(arg, allow)) { tmpArg = JvmPropObfuscator.obfuscate(arg);}
            sanitizedProps.add(tmpArg);
        }
        return sanitizedProps;
    }

    private boolean shouldObfuscate(String arg, Set<String> allow) {
        for (String prop: allow) {
            if (arg.startsWith(prop)) { return false;}
        }
        return true;
    }

    public void addEnvironmentChangeListener(EnvironmentChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removeEnvironmentChangeListener(EnvironmentChangeListener listener) {
        this.listeners.remove(listener);
    }

    private static List<String> fixInputArguments(List<String> args) {
        List<String> fixed = new ArrayList<>(args.size());
        for (String arg : args) {
            fixed.add(fixString(arg));
        }
        return fixed;
    }

    static String fixString(String arg) {
        Matcher matcher = JSON_WORKAROUND.matcher(arg);
        return matcher.replaceAll("");
    }

    /**
     * Stores the server port of instrumented application server.
     *
     * @param port
     */
    public void setServerPort(Integer port) {
        AgentIdentity newIdentity = agentIdentity.createWithNewServerPort(port);
        if (newIdentity == null) {
            Agent.LOG.finest("Application server port already set, not changing it to port " + port);
        } else {
            Agent.LOG.finer("Application server port: " + port);
            agentIdentity = newIdentity;
            notifyListenersIdentityChanged();
        }
    }

    public void setInstanceName(String instanceName) {
        AgentIdentity newIdentity = agentIdentity.createWithNewInstanceName(instanceName);
        if (newIdentity == null) {
            Agent.LOG.finest("Instance Name already set, not changing it to " + instanceName);
        } else {
            Agent.LOG.finer("Application server instance name: " + instanceName);
            agentIdentity = newIdentity;
            notifyListenersIdentityChanged();
        }
    }

    private void notifyListenersIdentityChanged() {
        for (EnvironmentChangeListener listener : listeners) {
            listener.agentIdentityChanged(this.agentIdentity);
        }
    }

    public AgentIdentity getAgentIdentity() {
        return agentIdentity;
    }

    public boolean setSolrVersion(Object version) {
        if (version == null) {
            return false;
        }

        if (solrVersion == null) {
            Agent.LOG.fine("Setting environment variable: Solr Version: " + version);
            solrVersion = version;
            return true;
        } else {
            Agent.LOG.finest("Solr version already set, not changing it to version " + version);
            return false;
        }
    }

    /*
     * This should only be called from the constructor. The environmentMap is not synchronized.
     */
    private void addVariable(String name, Object value) {
        environmentMap.add(Arrays.asList(name, value));
    }

    @VisibleForTesting
    public Object getVariable(String name) {
        for (List<?> item : environmentMap) {
            if (name.equals(item.get(0))) {
                return item.get(1);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeJSONString(Writer writer) throws IOException {
        List<Object> map = new ArrayList<Object>(environmentMap);
        map.add(Arrays.asList("Dispatcher", agentIdentity.getDispatcher()));
        map.add(Arrays.asList(PHYSICAL_CORE_KEY, physicalCoreCount));
        map.add(Arrays.asList(TOTAL_MEMORY_MB, physicalMemoryMB));
        if (agentIdentity.getDispatcherVersion() != null) {
            map.add(Arrays.asList("Dispatcher Version", agentIdentity.getDispatcherVersion()));
        }
        if (agentIdentity.getServerPort() != null) {
            map.add(Arrays.asList("Server port", agentIdentity.getServerPort()));
        }
        if (agentIdentity.getInstanceName() != null) {
            map.add(Arrays.asList("Instance Name", agentIdentity.getInstanceName()));
        }
        if (solrVersion != null) {
            map.add(Arrays.asList(SOLR_VERSION_KEY, solrVersion));
        }
        JSONArray.writeJSONString(map, writer);
    }

    public void setServerInfo(String dispatcherName, String version) {
        boolean notifyListeners = false;
        if ("Solr".equals(dispatcherName)) {
            // while the actual "identity" of the process has not changed, we need
            // to reconnect to send up the solr version
            // without the solr version - the solr tag will not be placed on the
            // application, meaning the user will not see their solr tabs
            notifyListeners = setSolrVersion(version);
        }

        AgentIdentity newIdentity = agentIdentity.createWithNewDispatcher(dispatcherName, version);
        if (newIdentity != null) {
            agentIdentity = newIdentity;
            notifyListeners = true;
            Agent.LOG.log(Level.FINER, "The dispatcher was set to {0}:{1}.", dispatcherName, version);
        }

        if (notifyListeners) {
            notifyListenersIdentityChanged();
        }
    }

    public void setServerInfo(String serverInfo) {
        Agent.LOG.config("Server Info: " + serverInfo);
        String[] info = serverInfo.split("/");
        if (info.length == 2) {
            setServerInfo(info[0], info[1]);
        }
    }
}
