package com.newrelic.agent.jmx;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.logging.Level;

public class LinkingMetadataRegistration {

    public static final String MBEAN_NAME = "com.newrelic.jfr:type=LinkingMetadata";
    private final Logger logger;

    public LinkingMetadataRegistration(Logger logger) {
        this.logger = logger;
    }

    public void registerLinkingMetadata() {
        try {
            MBeanServer server = getMbeanServer();
            ObjectName name = new ObjectName(MBEAN_NAME);
            logger.log(Level.INFO, "JMX LinkingMetadata started, registering MBean: " + name);
            Object bean = new LinkingMetadata();
            server.registerMBean(bean, name);
            logger.log(Level.INFO, "JMX LinkingMetadata bean registered");
        } catch (Exception e) {
            logger.log(Level.INFO, "Error registering JMX LinkingMetadata MBean", e);
        }
    }

    @VisibleForTesting
    protected MBeanServer getMbeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

}
