/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.Agent;
import com.newrelic.agent.HarvestListener;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JarCollectorConfig;
import com.newrelic.agent.config.JarCollectorConfigImpl;
import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.weave.weavepackage.WeavePackageConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Responsible for gathering and sending jars with version to the collector.
 */
public class JarCollectorServiceImpl extends AbstractService implements JarCollectorService, HarvestListener {

    /**
     * Performs the parsing and IO.
     */
    private final JarCollectorServiceProcessor processor = new JarCollectorServiceProcessor();

    /**
     * The last time we sent all jars because of a restart.
     */
    private long lastAllJarFlush = 0;

    /**
     * The name of the main application.
     */
    private final String defaultApp;

    private final boolean enabled;

    /**
     * The jar URLs that are queued up to be sent. On each harvest we swap out this set with a new one.
     */
    private final AtomicReference<Map<String, URL>> queuedJars = new AtomicReference<>(newUrlMap());

    private volatile Map<File, WeavePackageConfig> weaveConfigurations;

    private Map<String, URL> newUrlMap() {
        return new ConcurrentHashMap<>();
    }

    /**
     * Creates this ClassPathServiceImpl.
     */
    public JarCollectorServiceImpl() {
        super(JarCollectorService.class.getSimpleName());
        // get the default application
        AgentConfig config = ServiceFactory.getConfigService().getDefaultAgentConfig();
        defaultApp = config.getApplicationName();

        JarCollectorConfig jarCollectorConfig = config.getJarCollectorConfig();
        enabled = jarCollectorConfig.isEnabled();

        if (JarCollectorConfigImpl.isUsingDeprecatedConfigSettings()) {
            String deprecatedConfigMsg = "Jar Collector system properties prefixed with 'newrelic.config.module.' and environment variables prefixed with "
                    + "'NEW_RELIC_MODULE_' are deprecated and will be removed in a future agent release. Instead use the "
                    + "'newrelic.config.jar_collector.' and 'NEW_RELIC_JAR_COLLECTOR_' prefixes.";
            Agent.LOG.info(deprecatedConfigMsg);
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_DEPRECATED_CONFIG_JAR_COLLECTOR);
        }
    }

    /**
     * Returns true if this service is enabled.
     */
    @Override
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Called at startup.
     */
    @Override
    protected void doStart() throws Exception {
        if (enabled) {
            ServiceFactory.getHarvestService().addHarvestListener(this);
            weaveConfigurations = ImmutableMap.copyOf(getWeavePackageConfig(ServiceFactory.getExtensionService().getWeaveExtensions()));
        }
    }

    /**
     * Builds a map of weave jar files to their WeavePackageConfig.
     */
    private Map<File, WeavePackageConfig> getWeavePackageConfig(Collection<File> weaveExtensions) {
        Map<File, WeavePackageConfig> configs = new HashMap<>();
        for (File file : weaveExtensions) {
            try {
                WeavePackageConfig weaveConfig = WeavePackageConfig.builder().url(file.toURI().toURL()).build();
                configs.put(file, weaveConfig);
            } catch (Exception e) {
                Agent.LOG.log(Level.FINEST, e.getMessage(), e);
            }
        }
        return configs;
    }

    /**
     * Called when the service should stop.
     */
    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getHarvestService().removeHarvestListener(this);
    }

    /**
     * Returns true if all jars need to be sent to the collector.
     *
     * @return True if all jars need to be sent to the collector, else false.
     */
    private boolean needToSendAllJars() {
        return (ServiceFactory.getRPMService().getConnectionTimestamp() > lastAllJarFlush);
    }

    /**
     * Grabs all the new urls and file paths, and processes them. All newly converted jars will be sent to the
     * collector.
     *
     * @param pAppName The name of the application.
     * @param pStatsEngine Not used in this service.
     */
    @Override
    public synchronized void beforeHarvest(String pAppName, StatsEngine pStatsEngine) {

        if (!defaultApp.equals(pAppName)) {
            return;
        }

        Agent.LOG.log(Level.FINER, "Harvesting Modules");

        boolean sendAll = needToSendAllJars();

        // get new jars
        Map<String, URL> urls = queuedJars.getAndSet(newUrlMap());

        // add the new jars
        List<Jar> jars = processor.processModuleData(urls.values(), sendAll);
        addWeaveModules(jars);

        if (sendAll) {
            lastAllJarFlush = System.nanoTime();
        }

        // debug logging if needed
        if (!jars.isEmpty()) {
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                StringBuilder sb = new StringBuilder();
                for (Jar jar : jars) {
                    sb.append("   ");
                    sb.append(jar.getName());
                    sb.append(":");
                    sb.append(jar.getVersion());
                }
                Agent.LOG.log(Level.FINEST, "Sending jars: " + sb.toString());
            }
            try {
                // send the jars to the collector
                ServiceFactory.getRPMServiceManager()
                        .getOrCreateRPMService(pAppName)
                        .sendModules(jars);
            } catch (Exception e) {
                // HttpError/LicenseException handled here
                Agent.LOG.log(Level.FINE, MessageFormat.format("Unable to send {0} jar(s). Will attempt next harvest.", jars.size()));

                // requeue all the jar urls
                queuedJars.get().putAll(urls);
            }
        }
    }

    /**
     * Add jar information about the loaded external weave modules.
     *
     * @param jars
     */
    private void addWeaveModules(List<Jar> jars) {
        if (null != weaveConfigurations) {
            jars.addAll(JarCollectorServiceProcessor.getWeaveJars(weaveConfigurations));
            weaveConfigurations = null;
        }
    }

    /**
     * Nothing is done in the after harvest for this process.
     *
     * @param pAppName The name of the application
     */
    @Override
    public void afterHarvest(String pAppName) {
    }

    Map<String, URL> getQueuedJars() {
        return queuedJars.get();
    }

    /**
     * Adds urls which represent jars or directories containing jars.
     *
     * @param urls The jars to be added.
     */
    void addUrls(final URL... urls) {
        if (enabled) {
            for (URL url : urls) {
                if (JarCollectorServiceProcessor.JAR_PROTOCOL.equals(url.getProtocol())) {
                    String path = url.getFile();
                    if (!queuedJars.get().containsKey(path)) {
                        int index = path.lastIndexOf(JarCollectorServiceProcessor.JAR_EXTENSION);
                        if (index > 0) {
                            path = path.substring(0, index + JarCollectorServiceProcessor.JAR_EXTENSION.length());
                        }
                        try {
                            URL newUrl = new URL(path);
                            queuedJars.get().put(url.getPath(), newUrl);
                        } catch (MalformedURLException e) {
                            Agent.LOG.log(Level.FINEST, e, "Error parsing jar: {0}", e.getMessage());
                        }
                    }
                } else if (url.getFile().endsWith(JarCollectorServiceProcessor.JAR_EXTENSION)) {
                    queuedJars.get().put(url.getFile(), url);
                } else {
                    int jarIndex = url.getFile().lastIndexOf(JarCollectorServiceProcessor.JAR_EXTENSION);
                    if (jarIndex > 0) {
                        String path = url.getFile().substring(0, jarIndex + JarCollectorServiceProcessor.JAR_EXTENSION.length());

                        // this doesn't have to be perfectly thread safe, just fast. We're trying to avoid the overhead
                        // of constructing a URL
                        if (!queuedJars.get().containsKey(path)) {
                            try {
                                URL newUrl = new URL(url.getProtocol(), url.getHost(), path);
                                queuedJars.get().put(path, newUrl);
                            } catch (MalformedURLException e) {
                                Agent.LOG.log(Level.FINEST, e, "Error parsing jar: {0}", e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public ClassMatchVisitorFactory getSourceVisitor() {
        return new ClassMatchVisitorFactory() {

            @Override
            public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
                    ClassReader reader, ClassVisitor cv, InstrumentationContext context) {
                if (enabled && null != context.getProtectionDomain()
                        && null != context.getProtectionDomain().getCodeSource()
                        && null != context.getProtectionDomain().getCodeSource().getLocation()) {
                    addUrls(context.getProtectionDomain().getCodeSource().getLocation());
                }
                return null;
            }
        };
    }

}
