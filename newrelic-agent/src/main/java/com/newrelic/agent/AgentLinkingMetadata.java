/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.Hostname;
import com.newrelic.api.agent.TraceMetadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Utility class for providing agent linking metadata.
 * This metadata can be used to link events to specific traces, spans, hosts, and entities.
 */
public class AgentLinkingMetadata {
    public static final String ENTITY_TYPE_DEFAULT = "SERVICE";
    public static final String LOCALHOST = "localhost";
    // Agent linking metadata attributes
    public static final String TRACE_ID = "trace.id";
    public static final String SPAN_ID = "span.id";
    public static final String HOSTNAME = "hostname";
    public static final String ENTITY_GUID = "entity.guid";
    public static final String ENTITY_NAME = "entity.name";
    public static final String ENTITY_TYPE = "entity.type";
    // Kubernetes metadata injection attributes.
    public static final String K8S_CLUSTER_NAME = "k8s.clusterName";
    public static final String K8S_NAMESPACE_NAME = "k8s.namespaceName";
    public static final String K8S_POD_NAME = "k8s.podName";
    public static final String K8S_NODE_NAME = "k8s.nodeName";
    public static final String K8S_DEPLOYMENT_NAME = "k8s.deploymentName";
    public static final String K8S_REPLICASET_NAME = "k8s.replicasetName";
    public static final String K8S_CONTAINER_NAME = "k8s.containerName";
    public static final String K8S_CONTAINER_IMAGE_NAME = "k8s.containerImageName";

    private static final Map<String, String> KUBERNETES_METADATA = buildKubernetesMetadata(System.getenv());

    /**
     * Get a map of all agent linking metadata.
     *
     * @param traceMetadata TraceMetadataImpl instance to get spanId and traceId
     * @param configService ConfigService to get hostName and entityName
     * @param rpmService    IRPMService to get entityGuid
     * @return Map of all agent linking metadata
     */
    public static Map<String, String> getLinkingMetadata(TraceMetadata traceMetadata, ConfigService configService, IRPMService rpmService) {
        AgentConfig agentConfig = configService.getDefaultAgentConfig();
        Map<String, String> linkingMetadata = new ConcurrentHashMap<>();

        linkingMetadata.put(TRACE_ID, getTraceId(traceMetadata));
        linkingMetadata.put(SPAN_ID, getSpanId(traceMetadata));
        linkingMetadata.put(HOSTNAME, getHostname(agentConfig));
        linkingMetadata.put(ENTITY_NAME, getEntityName(agentConfig));
        linkingMetadata.put(ENTITY_TYPE, getEntityType());

        try {
            String entityGuid = getEntityGuid(rpmService);
            if (!entityGuid.isEmpty()) {
                linkingMetadata.put(ENTITY_GUID, entityGuid);
            }
        } catch (NullPointerException ignored) {
            logWarning();
        }

        return linkingMetadata;
    }

    /**
     * Get a map of agent linking metadata minus
     * entity.type and any attributes with an empty value.
     * This subset of linking metadata is added to LogEvents.
     *
     * @param traceMetadata TraceMetadataImpl to get spanId and traceId
     * @param configService ConfigService to get hostName and entityName
     * @param rpmService    IRPMService to get entityGuid
     * @return Filtered map of agent linking metadata
     */
    public static Map<String, String> getLogEventLinkingMetadata(TraceMetadata traceMetadata, ConfigService configService, IRPMService rpmService) {
        AgentConfig agentConfig = configService.getDefaultAgentConfig();
        Map<String, String> logEventLinkingMetadata = new ConcurrentHashMap<>();

        String traceId = getTraceId(traceMetadata);
        if (!traceId.isEmpty()) {
            logEventLinkingMetadata.put(TRACE_ID, traceId);
        }

        String spanId = getSpanId(traceMetadata);
        if (!spanId.isEmpty()) {
            logEventLinkingMetadata.put(SPAN_ID, spanId);
        }

        String hostname = getHostname(agentConfig);
        if (!hostname.isEmpty()) {
            logEventLinkingMetadata.put(HOSTNAME, hostname);
        }

        String entityName = rpmService.getApplicationName();
        if (!entityName.isEmpty()) {
            logEventLinkingMetadata.put(ENTITY_NAME, entityName);
        }

        try {
            String entityGuid = rpmService.getEntityGuid();
            if (!entityGuid.isEmpty()) {
                logEventLinkingMetadata.put(ENTITY_GUID, entityGuid);
            }
        } catch (NullPointerException ignored) {
            logWarning();
        }

        logEventLinkingMetadata.putAll(KUBERNETES_METADATA);

        return logEventLinkingMetadata;
    }

    /**
     * Build a map of Kubernetes metadata injection attributes from the environment variables set by the
     * k8s-metadata-injection webhook. Only non-blank values are included, so this is naturally empty outside of Kubernetes.
     *
     * @param env environment variables to read from
     * @return unmodifiable map of Kubernetes linking metadata attributes
     */
    static Map<String, String> buildKubernetesMetadata(Map<String, String> env) {
        Map<String, String> kubernetesMetadata = new HashMap<>();
        putIfNotBlank(kubernetesMetadata, K8S_CLUSTER_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_CLUSTER_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_NAMESPACE_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_NAMESPACE_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_POD_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_POD_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_NODE_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_NODE_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_DEPLOYMENT_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_DEPLOYMENT_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_REPLICASET_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_REPLICASET_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_CONTAINER_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_CONTAINER_NAME"));
        putIfNotBlank(kubernetesMetadata, K8S_CONTAINER_IMAGE_NAME, env.get("NEW_RELIC_METADATA_KUBERNETES_CONTAINER_IMAGE_NAME"));
        return Collections.unmodifiableMap(kubernetesMetadata);
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value != null && !value.isEmpty()) {
            map.put(key, value);
        }
    }

    public static String getTraceId(TraceMetadata traceMetadata) {
        return traceMetadata.getTraceId();
    }

    public static String getSpanId(TraceMetadata traceMetadata) {
        return traceMetadata.getSpanId();
    }

    private static String getHostname(AgentConfig agentConfig) {
        String fullHostname = Hostname.getFullHostname(agentConfig);
        if (fullHostname == null || fullHostname.isEmpty() || fullHostname.equals(LOCALHOST)) {
            return Hostname.getHostname(agentConfig);
        }
        return fullHostname;
    }

    public static String getEntityName(AgentConfig agentConfig) {
        return agentConfig.getApplicationName();
    }

    public static String getEntityType() {
        return ENTITY_TYPE_DEFAULT;
    }

    public static String getEntityGuid(IRPMService rpmService) {
        return rpmService.getEntityGuid();
    }

    private static void logWarning() {
        // It's possible to call getEntityGuid in the agent premain before the
        // RPMService has been initialized, which will cause a NullPointerException.
        Agent.LOG.log(Level.WARNING, "Cannot get entity.guid from getLinkingMetadata() until RPMService has initialized.");
    }
}
