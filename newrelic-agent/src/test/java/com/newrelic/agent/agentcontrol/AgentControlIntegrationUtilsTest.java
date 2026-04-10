/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.agentcontrol.health.AgentHealth;
import com.newrelic.agent.agentcontrol.health.HealthDataChangeListener;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AgentControlIntegrationUtilsTest {
    private HealthDataChangeListener mockListener;

    @Before
    public void before() {
        mockListener = mock(HealthDataChangeListener.class);

        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);
    }

    @Test
    public void getPseudoCurrentTimeNanos_returnsTimeInNanos() {
        long testTime = System.currentTimeMillis();
        long time = AgentControlIntegrationUtils.getPseudoCurrentTimeNanos();
        assertTrue(time >= testTime * 1000000);
    }

    @Test
    public void reportUnhealthyStatus_updatesHealthListeners() {
        AgentControlIntegrationUtils.reportUnhealthyStatus(Collections.singletonList(mockListener), AgentHealth.Status.GC_CIRCUIT_BREAKER);
        verify(mockListener).onUnhealthyStatus(AgentHealth.Status.GC_CIRCUIT_BREAKER);
    }

    @Test
    public void reportHealthyStatus_updatesHealthListeners() {
        AgentControlIntegrationUtils.reportHealthyStatus(Collections.singletonList(mockListener), AgentHealth.Category.HARVEST);
        verify(mockListener).onHealthyStatus(AgentHealth.Category.HARVEST);
    }

    @Test
    public void createYamlWriter_createValidYamlWriter() {
        assertNotNull(AgentControlIntegrationUtils.createYamlWriter());
    }

    @Test
    public void createAgentControlFileFolderInstance_createsValidFileInstance() {
        URI uri = null;
        try {
            uri = new URI("file://" + System.getProperty("user.dir"));
        } catch (URISyntaxException ignored) {
        }

        File f = AgentControlIntegrationUtils.createAgentControlFileFolderInstance(uri, AgentControlIntegrationUtils.FileType.effective_config);
        assertEquals(System.getProperty("user.dir"), f.getAbsolutePath());
        assertTrue(f.exists());
        assertTrue(f.isDirectory());

        f = AgentControlIntegrationUtils.createAgentControlFileFolderInstance(uri, AgentControlIntegrationUtils.FileType.health);
        assertEquals(System.getProperty("user.dir"), f.getAbsolutePath());
        assertTrue(f.exists());
        assertTrue(f.isDirectory());
    }

    @Test
    public void generateAgentControlFilename_createsValidFilename() {
        String filename = AgentControlIntegrationUtils.generateAgentControlFilename(
                AgentControlIntegrationUtils.FileType.effective_config);
        assertTrue(filename.matches("effective_config-[a-z0-9]{32}.yml"));

        filename = AgentControlIntegrationUtils.generateAgentControlFilename(
                AgentControlIntegrationUtils.FileType.health);
        assertTrue(filename.matches("health-[a-z0-9]{32}.yml"));
    }

    @Test
    public void writeMapPayloadToFile_createsValidYamlFile() {
        URI uri = null;
        try {
            uri = new URI("file://" + System.getProperty("user.dir"));
        } catch (URISyntaxException ignored) {
        }
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("foo", "bar");

        File file = new File(AgentControlIntegrationUtils
                .createAgentControlFileFolderInstance(uri, AgentControlIntegrationUtils.FileType.effective_config),
                AgentControlIntegrationUtils.generateAgentControlFilename(AgentControlIntegrationUtils.FileType.effective_config));
        file.deleteOnExit();

        AgentControlIntegrationUtils.writeMapPayloadToFile(configMap, file,
                AgentControlIntegrationUtils.createYamlWriter(),  AgentControlIntegrationUtils.FileType.effective_config);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }
}
