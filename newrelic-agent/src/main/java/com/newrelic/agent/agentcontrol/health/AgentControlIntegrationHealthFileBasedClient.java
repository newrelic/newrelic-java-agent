/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol.health;

import com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.FileType;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.createAgentControlFileFolderInstance;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.createYamlWriter;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.generateAgentControlFilename;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.writeMapPayloadToFile;

public class AgentControlIntegrationHealthFileBasedClient implements AgentControlIntegrationHealthClient {
    private Yaml yamlWriter;
    private File healthFile = null;
    private boolean isValid = false;

    public AgentControlIntegrationHealthFileBasedClient(AgentControlIntegrationConfig config) {
        URI locationFromConfig = config.getHealthDeliveryLocation();

        File fileFolder = createAgentControlFileFolderInstance(locationFromConfig, FileType.health);
        if (fileFolder != null) {
            this.healthFile = new File(fileFolder,
                    generateAgentControlFilename(AgentControlIntegrationUtils.FileType.health));

            yamlWriter = createYamlWriter();
            isValid = true;
        }
    }

    @Override
    public void sendHealthMessage(AgentHealth agentHealth) {
        writeMapPayloadToFile(createHeathMessageMap(agentHealth), healthFile,
                yamlWriter, AgentControlIntegrationUtils.FileType.health);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    private Map<String, Object> createHeathMessageMap(AgentHealth agentHealth) {
        Map<String, Object> healthMap = new LinkedHashMap<>();
        healthMap.put("entity_guid", agentHealth.getEntityGuid() == null ? "" : agentHealth.getEntityGuid());
        healthMap.put("healthy", agentHealth.isHealthy());
        healthMap.put("status", agentHealth.getCurrentStatus());
        healthMap.put("start_time_unix_nano", agentHealth.getStartTimeNanos());
        healthMap.put("status_time_unix_nano", AgentControlIntegrationUtils.getPseudoCurrentTimeNanos());
        if (!agentHealth.isHealthy()) {
            healthMap.put("last_error", agentHealth.getLastError());
        }

        return healthMap;
    }
}
