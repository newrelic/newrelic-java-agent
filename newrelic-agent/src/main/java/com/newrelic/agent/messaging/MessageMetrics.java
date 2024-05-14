package com.newrelic.agent.messaging;

import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.MessageBrokerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.TracedMethod;

public class MessageMetrics {
    public static final String METRIC_NAMESPACE = "MessageBroker";
    public static final String SLASH = "/";
    public static final String MESSAGE_BROKER_INSTANCE = METRIC_NAMESPACE + "/instance/";

    public static final String UNKNOWN = "unknown";
    public static String HOSTNAME = Hostname.getHostname(ServiceFactory.getConfigService().getDefaultAgentConfig());

    public static boolean isEndpointParamsKnown(String host, Integer port) {
        return !(isParamUnknown(host) && isParamUnknown(port));
    }
    public static void collectMessageProducerRollupMetrics(TracedMethod method, String library, String host, Integer port) {
        reportInstanceIfEnabled(method, library, host, port);
    }

    public static void collectMessageConsumerRollupMetrics(TracedMethod method, String library, String host, Integer port) {
        reportInstanceIfEnabled(method, library, host, port);
    }

    public static void reportInstanceIfEnabled(TracedMethod method, String library, String host, Integer port) {
        MessageBrokerConfig messageBrokerConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getMessageBrokerConfig();
        if (messageBrokerConfig.isInstanceReportingEnabled()) {
            String metric = buildInstanceMetric(library, host, port);
            method.addRollupMetricName(metric);
        }
    }

    public static String buildInstanceMetric(String library, String host, Integer port) {
        String instance = buildInstanceIdentifier(library, host, port);
        return MESSAGE_BROKER_INSTANCE + instance;
    }

    public static String buildInstanceIdentifier(String library, String host, Integer port) {
        String libraryName = replaceLibrary(library);
        String hostname = replaceLocalhost(host);
        String portName = replacePort(port);

        return libraryName + SLASH + hostname + SLASH + portName;
    }

    public static String replaceLibrary(String library) {
        if (isParamUnknown(library)) {
            return UNKNOWN;
        }
        return library.toLowerCase();
    }

    public static String replaceLocalhost(String host) {
        if (isParamUnknown(host)) {
            return HOSTNAME;
        }

        if ("localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)
                || "0:0:0:0:0:0:0:1".equals(host) || "::1".equals(host) || "0:0:0:0:0:0:0:0".equals(host)
                || "::".equals(host)) {
            return HOSTNAME;
        }

        return host;
    }

    public static String replacePort(Integer port) {
        if (isParamUnknown(port)) {
            return UNKNOWN;
        }
        return String.valueOf(port);
    }

    private static boolean isParamUnknown(String str) {
        return str == null || str.isEmpty();
    }

    private static boolean isParamUnknown(Integer integer) {
        return integer == null || integer == -1;
    }
}
