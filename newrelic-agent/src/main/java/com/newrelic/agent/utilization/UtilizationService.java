/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.Hostname;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.DefaultThreadFactory;
import com.newrelic.agent.utilization.AWS.AwsData;
import com.newrelic.agent.utilization.Azure.AzureData;
import com.newrelic.agent.utilization.GCP.GcpData;
import com.newrelic.agent.utilization.PCF.PcfData;
import com.newrelic.agent.utilization.AzureAppService.AzureAppServiceData;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * Service to gather utilization and pricing data.
 *
 * Utilization data should be gathered each time an agent connect is triggered.
 */
public class UtilizationService extends AbstractService {

    public static final String DETECT_AWS_KEY = "utilization.detect_aws";
    public static final String DETECT_DOCKER_KEY = "utilization.detect_docker";
    public static final String DETECT_PIVOTAL_CLOUD_FOUNDRY_KEY = "utilization.detect_pcf";
    public static final String DETECT_GOOGLE_CLOUD_PROVIDER_KEY = "utilization.detect_gcp";
    public static final String DETECT_AZURE_KEY = "utilization.detect_azure";
    public static final String DETECT_KUBERNETES_KEY = "utilization.detect_kubernetes";
    // protected for testing
    protected volatile UtilizationData utilizationData;
    private final String hostName;
    private final String fullHostName;
    private final ArrayList<String> ipAddress;
    private final String bootId;
    private final String dockerContainerId;
    private final String ecsFargateDockerContainerId;
    private final int processorCount;
    private final Future<Long> totalRamInMibFuture;
    private final UtilizationConfig configData;
    private final KubernetesData kubernetesData;
    private final ExecutorService executor = Executors.newFixedThreadPool(2, new DefaultThreadFactory(THREAD_NAME, true));
    private Future<UtilizationData> future = null;


    private static final String THREAD_NAME = "New Relic Utilization Service";

    /**
     * This is an optimization. false when were 100% sure we're not running on Linux. true otherwise.
     */
    private final boolean isLinux;
    private final boolean detectAws;
    private final boolean detectDocker;
    private final boolean detectPcf;
    private final boolean detectGcp;
    private final boolean detectAzure;
    private final boolean detectKubernetes;
    private final DockerData dockerData;

    private static final CloudUtility cloudUtility = new CloudUtility();
    private static final AWS aws = new AWS(cloudUtility);
    private static final PCF pcf = new PCF(cloudUtility);
    private static final GCP gcp = new GCP(cloudUtility);
    private static final Azure azure = new Azure(cloudUtility);
    private static final AzureAppService azureAppService = new AzureAppService(cloudUtility);

    public UtilizationService() {
        super(UtilizationService.class.getSimpleName());
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        detectAws = agentConfig.getValue(DETECT_AWS_KEY, Boolean.TRUE);
        detectDocker = agentConfig.getValue(DETECT_DOCKER_KEY, Boolean.TRUE);
        detectPcf = agentConfig.getValue(DETECT_PIVOTAL_CLOUD_FOUNDRY_KEY, Boolean.TRUE);
        detectGcp = agentConfig.getValue(DETECT_GOOGLE_CLOUD_PROVIDER_KEY, Boolean.TRUE);
        detectAzure = agentConfig.getValue(DETECT_AZURE_KEY, Boolean.TRUE);
        detectKubernetes = agentConfig.getValue(DETECT_KUBERNETES_KEY, Boolean.TRUE);
        dockerData = new DockerData(agentConfig.getCloudConfig());

        hostName = Hostname.getHostname(agentConfig);
        fullHostName = Hostname.getFullHostname(agentConfig);
        ipAddress = Hostname.getIpAddress(agentConfig);
        isLinux = isLinuxOs();
        bootId = DataFetcher.getBootId();
        dockerContainerId = detectDocker ? getDockerContainerId() : null;
        ecsFargateDockerContainerId = detectAws ? getEcsFargateDockerContainerId() : null;
        processorCount = DataFetcher.getLogicalProcessorCount();
        totalRamInMibFuture = executor.submit(DataFetcher.getTotalRamInMibCallable());
        configData = UtilizationConfig.createFromConfigService();
        kubernetesData = getKubernetesData();
        utilizationData = new UtilizationData(hostName, fullHostName, ipAddress, processorCount, dockerContainerId, ecsFargateDockerContainerId,
                bootId, null, totalRamInMibFuture, configData, kubernetesData);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    protected void doStart() throws Exception {
        scheduleUtilizationTask();
    }

    @Override
    protected void doStop() throws Exception {
        executor.shutdownNow();
    }

    private void scheduleUtilizationTask() {
        future = executor.submit(new UtilizationTask());
    }

    /**
     * Updates utilization data asynchronously. This should only be called when this service starts and before the agent
     * connects.<br>
     * <br>
     * Implementation details: this method can make a number of HTTP requests and read from different files to get
     * utilization data.
     *
     * @return Updated utilization data.
     */
    public UtilizationData updateUtilizationData() {
        if (future == null) {
            // future should be null on every reconnect, but not on the initial connect.
            future = executor.submit(new UtilizationTask());
        }

        try {
            /*
             * AWS does 1 HTTP request, PCF does 0 HTTP requests, GCP does 1 HTTP request, Azure does 1 HTTP request.
             * Each HTTP request has a 100ms timeout. The worst case should be for Azure requiring 3 requests in total.
             */
            utilizationData = future.get(2000, TimeUnit.MILLISECONDS);
            future = null;
        } catch (TimeoutException e) {
            Agent.LOG.log(Level.FINER, "Utilization task timed out. Returning cached utilization data.");
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "Utilization task exception. Returning cached utilization data. {0}", t);
        }

        return utilizationData;
    }

    private static boolean isLinuxOs() {
        /*
         * This is an optimization. If we are on windows or mac then we know we are not on Docker.
         */
        String os = ManagementFactory.getOperatingSystemMXBean().getName().toLowerCase();
        return os != null && !(os.startsWith("windows") || os.startsWith("mac"));
    }

    /*
     * These methods allow us to test configuration properties.
     */
    protected AwsData getAwsData() {
        return aws.getData();
    }

    protected PcfData getPcfData() {
        return pcf.getData();
    }

    protected GcpData getGcpData() {
        return gcp.getData();
    }

    protected AzureData getAzureData() {
        return azure.getData();
    }

    protected AzureAppService.AzureAppServiceData getAzureAppServiceData() {
        return azureAppService.getData();
    }

    DockerData getDockerData() {
        return dockerData;
    }

    KubernetesData getKubernetesData() {
        return detectKubernetes ? DataFetcher.getKubernetesData(SystemPropertyFactory.getSystemPropertyProvider()) : null;
    }

    /*
     * Do not call DockerData.getDockerContainerId(boolean, String) directly, call this method instead.
     */
    String getDockerContainerId() {
        return getDockerData().getDockerContainerIdFromCGroups(isLinux);
    }

    String getEcsFargateDockerContainerId() {
        return getDockerData().getDockerContainerIdForEcsFargate(isLinux);
    }

    class UtilizationTask implements Callable<UtilizationData> {

        @Override
        public UtilizationData call() throws Exception {
            return doUpdateUtilizationData();
        }

        private UtilizationData doUpdateUtilizationData() {
            CloudData foundData = null;

            /* Fail fast optimization: short-circuit once we've made a successful connection to a cloud vendor.
             * Try AWS first, if that returns metadata there's no need to try others as we're running on AWS. If the
             * call to AWS fails try the next provider (Pivotal Cloud Foundry) using the same logic.
             */
            AwsData awsData = detectAws ? getAwsData() : AwsData.EMPTY_DATA;
            if (awsData != AwsData.EMPTY_DATA) {
                foundData = awsData;
            } else {
                PcfData pcfData = detectPcf ? getPcfData() : PcfData.EMPTY_DATA;
                if (pcfData != PcfData.EMPTY_DATA) {
                    foundData = pcfData;
                } else {
                    GcpData gcpData = detectGcp ? getGcpData() : GcpData.EMPTY_DATA;
                    if (gcpData != GcpData.EMPTY_DATA) {
                        foundData = gcpData;
                    } else {
                        AzureData azureData = detectAzure ? getAzureData() : AzureData.EMPTY_DATA;
                        if (azureData != AzureData.EMPTY_DATA) {
                            foundData = azureData;
                        } else {
                            AzureAppServiceData azureAppServiceData = detectAzure ? getAzureAppServiceData() : AzureAppServiceData.EMPTY_DATA;
                            if (azureAppServiceData != AzureAppServiceData.EMPTY_DATA) {
                                foundData = azureAppServiceData;
                            }
                        }
                    }
                }
            }

            return new UtilizationData(hostName, fullHostName, ipAddress, processorCount, dockerContainerId, ecsFargateDockerContainerId, bootId,
                    foundData, totalRamInMibFuture, configData, kubernetesData);
        }
    }

}
