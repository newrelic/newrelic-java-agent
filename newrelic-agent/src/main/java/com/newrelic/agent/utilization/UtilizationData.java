/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class UtilizationData {

    private static final String METADATA_VERSION_KEY = "metadata_version";
    private static final int METADATA_VERSION = 5;
    private static final String LOGICAL_CORES_KEY = "logical_processors";
    private static final String RAM_KEY = "total_ram_mib";
    private static final String HOSTNAME_KEY = "hostname";
    private static final String FULL_HOSTNAME_KEY = "full_hostname";
    private static final String IP_ADDRESS = "ip_address";
    private static final String VENDORS_KEY = "vendors";
    private static final String BOOT_ID = "boot_id";
    private static final String DOCKER = "docker";
    private static final String ECS = "ecs";
    private static final String DOCKER_ID_KEY = "id";
    private static final String ECS_ID_KEY = "ecsDockerId";
    private static final String CONFIG_KEY = "config";
    private static final String KUBERNETES = "kubernetes";

    private CloudData cloudData;
    private final String hostname;
    private final String fullHostName;
    private final ArrayList<String> ipAddress;
    private final Integer logicalProcessorCount;
    private final String dockerContainerId;
    private final String ecsFargateDockerContainerId;
    private final String bootId;
    private final Long totalRamMib;
    private final UtilizationConfig dataConfig;
    private final KubernetesData kubernetesData;

    public UtilizationData(String host, String fullHost, ArrayList<String> ip, Integer logicalProcessorCt, String dockerId, String ecsFargateDockerContainerId,
            String bootId, CloudData cloudData, Future<Long> ramFuture, UtilizationConfig configData, KubernetesData kubernetesData) {
        this(host, fullHost, ip, logicalProcessorCt, dockerId, ecsFargateDockerContainerId, bootId, cloudData,
                getTotalRamMibFromFuture(ramFuture), configData, kubernetesData);
    }

    public UtilizationData(String host, String fullHost, ArrayList<String> ip, Integer logicalProcessorCt, String dockerId, String ecsFargateDockerContainerId,
            String bootId, CloudData cloudData, Long ram, UtilizationConfig configData, KubernetesData kubernetesData) {
        this.hostname = host;
        this.fullHostName = fullHost;
        this.ipAddress = ip;
        this.logicalProcessorCount = Integer.valueOf(0).equals(logicalProcessorCt) ? null : logicalProcessorCt;
        this.dockerContainerId = dockerId;
        this.ecsFargateDockerContainerId = ecsFargateDockerContainerId;
        this.bootId = bootId;
        this.cloudData = cloudData;
        this.totalRamMib = Long.valueOf(0).equals(ram) ? null : ram;
        this.dataConfig = configData;
        this.kubernetesData = kubernetesData;
    }

    public Map<String, Object> map() {
        Map<String, Object> data = new HashMap<>();
        data.put(METADATA_VERSION_KEY, METADATA_VERSION);
        data.put(LOGICAL_CORES_KEY, logicalProcessorCount);
        data.put(RAM_KEY, totalRamMib);
        data.put(HOSTNAME_KEY, hostname);

        if (fullHostName != null && !fullHostName.isEmpty() && !fullHostName.equals(hostname) && !fullHostName.equals("localhost")) {
            data.put(FULL_HOSTNAME_KEY, fullHostName);
        }

        if (ipAddress != null && ipAddress.size() > 0) {
            data.put(IP_ADDRESS, ipAddress);
        }

        // only add boot_id to hash if it is valid, default is null
        if (bootId != null) {
            data.put(BOOT_ID, bootId);
        }

        Map<String, Object> vendors = new HashMap<>();
        if (cloudData != null && !cloudData.isEmpty()) {
            vendors.put(cloudData.getProvider(), cloudData.getValueMap());
        }

        Map<String, Object> utilizationConfig = new HashMap<>();
        if (dataConfig.getHostname() != null) {
            utilizationConfig.put(HOSTNAME_KEY, dataConfig.getHostname());
        }
        if (dataConfig.getLogicalProcessors() != null) {
            utilizationConfig.put(LOGICAL_CORES_KEY, dataConfig.getLogicalProcessors());
        }
        if (dataConfig.getTotalRamMib() != null) {
            utilizationConfig.put(RAM_KEY, dataConfig.getTotalRamMib());
        }

        if (!utilizationConfig.isEmpty()) {
            data.put(CONFIG_KEY, utilizationConfig);
        }

        if (kubernetesData != null) {
            Map<String, String> kubernetesValues = kubernetesData.getValueMap();
            if (kubernetesValues != null && !kubernetesValues.isEmpty()) {
                vendors.put(KUBERNETES, new HashMap<>(kubernetesValues));
            }
        }

        if (dockerContainerId != null) {
            Map<String, String> docker = new HashMap<>();
            docker.put(DOCKER_ID_KEY, dockerContainerId);
            vendors.put(DOCKER, docker);
        }

        if (ecsFargateDockerContainerId != null) {
            Map<String, String> ecs = new HashMap<>();
            ecs.put(ECS_ID_KEY, ecsFargateDockerContainerId);
            vendors.put(ECS, ecs);
        }

        if (!vendors.isEmpty()) {
            data.put(VENDORS_KEY, vendors);
        }

        return data;
    }

    private static Long getTotalRamMibFromFuture(Future<Long> ramFuture) {
        Long ram = 0L;
        try {
            // We are using a future here to prevent a hanging call to "executeCommand()" from blocking other data
            ram = ramFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // This will default to using `0` from above
            AgentBridge.getAgent().getLogger().log(Level.FINE, e, "Unable to capture total RAM size");
            new CloudUtility().recordError(MetricNames.SUPPORTABILITY_MEMORY_ERROR);
        }
        return ram;
    }

}
