/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Mocks;
import com.newrelic.agent.ThreadService;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;
import com.newrelic.agent.config.UtilizationDataConfig;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtilizationServiceTest {
    private static AgentConfig config;

    @Before
    public void before() {
        MockServiceManager mockServiceManager = new MockServiceManager();
        config = mock(AgentConfig.class);
        MockConfigService configService = new MockConfigService(null);
        configService.setAgentConfig(config);
        ServiceFactory.setServiceManager(mockServiceManager);
        mockServiceManager.setConfigService(configService);
        ThreadService mockThreadService = mock(ThreadService.class);
        mockServiceManager.setThreadService(mockThreadService);

        // Do not make AWS/Azure/GCP/PCF calls. Unit tests should work even without internet connection :)
        when(config.getValue(UtilizationService.DETECT_DOCKER_KEY, true)).thenReturn(false);
        when(config.getValue(UtilizationService.DETECT_AWS_KEY, true)).thenReturn(false);
        when(config.getValue(UtilizationService.DETECT_AZURE_KEY, true)).thenReturn(false);
        when(config.getValue(UtilizationService.DETECT_GOOGLE_CLOUD_PROVIDER_KEY, true)).thenReturn(false);
        when(config.getValue(UtilizationService.DETECT_PIVOTAL_CLOUD_FOUNDRY_KEY, true)).thenReturn(false);
        when(config.getValue(UtilizationService.DETECT_KUBERNETES_KEY, true)).thenReturn(false);

        UtilizationDataConfig utilDataConfig = mock(UtilizationDataConfig.class);
        when(config.getUtilizationDataConfig()).thenReturn(utilDataConfig);
    }

    @Test
    public void testNoAwsCallsAwsConfigurationDisabled() {
        UtilizationService utilizationService = Mockito.spy(new UtilizationService());
        UtilizationData oldData = utilizationService.utilizationData;
        UtilizationData newData = utilizationService.updateUtilizationData();
        Mockito.verify(utilizationService, Mockito.times(0)).getAwsData();
        Assert.assertNotEquals("There was an error running the utilization task. The utilization data was not updated.",
                oldData, newData);
    }

    @Test
    public void testNoDockerCheckDockerConfigDisabled() {
        UtilizationService utilizationService = Mockito.spy(new UtilizationService());
        UtilizationData oldData = utilizationService.utilizationData;
        UtilizationData newData = utilizationService.updateUtilizationData();
        Mockito.verify(utilizationService, Mockito.times(0)).getDockerContainerId();
        Assert.assertNotEquals("There was an error running the utilization task. The utilization data was not updated.",
                oldData, newData);
    }

    @Test
    public void testDockerCheckDockerConfigEnabled() {
        when(config.getValue(UtilizationService.DETECT_DOCKER_KEY, true)).thenReturn(true);

        UtilizationService utilizationService = Mockito.spy(new UtilizationService());
        DockerData dockerData = mock(DockerData.class);
        when(dockerData.getDockerContainerIdFromCGroups(false)).thenReturn("f96c541a87e1376f25461f1386cb60208cea35750eac1e24e11566f078715920");
        when(utilizationService.getDockerData()).thenReturn(dockerData);
        UtilizationData oldData = utilizationService.utilizationData;
        UtilizationData newData = utilizationService.updateUtilizationData();
        Assert.assertNotEquals("There was an error running the utilization task. The utilization data was not updated.",
                oldData, newData);
    }

    @Test
    public void testKubernetesConfigEnabled() {
        SystemPropertyProvider originalProvider = SystemPropertyFactory.getSystemPropertyProvider();
        try {
            SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(new HashMap<String, String>(),
                    ImmutableMap.of(KubernetesData.KUBERNETES_SERVICE_HOST_ENV, "10.0.0.1"));
            SystemPropertyFactory.setSystemPropertyProvider(testProvider);

            when(config.getValue(UtilizationService.DETECT_KUBERNETES_KEY, true)).thenReturn(true);

            UtilizationService utilizationService = Mockito.spy(new UtilizationService());
            KubernetesData kubernetesData = utilizationService.getKubernetesData();
            Assert.assertNotNull(kubernetesData);
            Assert.assertNotNull(kubernetesData.getValueMap());
            Assert.assertEquals("10.0.0.1", kubernetesData.getValueMap().get(KubernetesData.KUBERNETES_SERVICE_HOST_KEY));
        } finally {
            SystemPropertyFactory.setSystemPropertyProvider(originalProvider);
        }
    }

    @Test
    public void testKubernetesConfigDisabled() {
        SystemPropertyProvider originalProvider = SystemPropertyFactory.getSystemPropertyProvider();
        try {
            SystemPropertyProvider testProvider = Mocks.createSystemPropertyProvider(new HashMap<String, String>(),
                    ImmutableMap.of(KubernetesData.KUBERNETES_SERVICE_HOST_ENV, "10.0.0.2"));
            SystemPropertyFactory.setSystemPropertyProvider(testProvider);

            when(config.getValue(UtilizationService.DETECT_KUBERNETES_KEY, true)).thenReturn(false);

            UtilizationService utilizationService = Mockito.spy(new UtilizationService());
            KubernetesData kubernetesData = utilizationService.getKubernetesData();
            Assert.assertNull(kubernetesData);
        } finally {
            SystemPropertyFactory.setSystemPropertyProvider(originalProvider);
        }
    }

}
