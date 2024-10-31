/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.superagent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.SuperAgentIntegrationConfig;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SuperAgentIntegrationHealthFileBasedClient implements SuperAgentIntegrationHealthClient {
    private Yaml yamlWriter;
    private File healthFile = null;
    private boolean isValid = false;

    public SuperAgentIntegrationHealthFileBasedClient(SuperAgentIntegrationConfig config) {
        URI locationFromConfig = config.getHealthDeliveryLocation();

        File fileFolder = createHealthFileFolderInstance(locationFromConfig);
        if (fileFolder != null) {
            this.healthFile = new File(fileFolder, generateHealthFilename());

            DumperOptions yamlDumperOptions = new DumperOptions();
            yamlDumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            yamlDumperOptions.setPrettyFlow(true);
            yamlWriter = new Yaml(yamlDumperOptions);
            isValid = true;
        }
    }

    @Override
    public void sendHealthMessage(AgentHealth agentHealth) {
        if (healthFile != null) {
            try {
                FileWriter fw = new FileWriter(healthFile);
                yamlWriter.dump(createHeathMessageMap(agentHealth), fw);
                fw.close();

                if (Agent.LOG.isFinestEnabled() && Agent.isDebugEnabled()) {
                    Agent.LOG.log(Level.FINEST, "Wrote SA health file: {0}", healthFile.getAbsolutePath());
                }
            } catch (IOException e) {
                Agent.LOG.log(Level.WARNING, "Error writing health message to file: {0}", e.getMessage());
            }
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    private Map<String, Object> createHeathMessageMap(AgentHealth agentHealth) {
        Map<String, Object> healthMap = new HashMap<>();

        healthMap.put("healthy", agentHealth.isHealthy());
        healthMap.put("status", agentHealth.getCurrentStatus());
        healthMap.put("start_time_unix_nano", agentHealth.getStartTimeNanos());
        healthMap.put("status_time_unix_nano", SuperAgentIntegrationUtils.getPseudoCurrentTimeNanos());
        if (!agentHealth.isHealthy()) {
            healthMap.put("last_error", agentHealth.getLastError());
        }

        return healthMap;
    }
    
    private File createHealthFileFolderInstance(URI location) {
        File fileFolder = null;
        if (location != null) {
            fileFolder = new File(location);
            if (!(fileFolder.isDirectory() && fileFolder.canWrite())) {
                Agent.LOG.log(Level.WARNING, "superagent.health.delivery_location is not a valid folder. " +
                        "Health messages will not be generated.  Configured location: {0}  isFolder: {1}  canWrite: {2}",
                        fileFolder.getAbsolutePath(), fileFolder.isDirectory(), fileFolder.canWrite());
                fileFolder = null;
            }
        }
        
        return fileFolder;
    }

    private String generateHealthFilename() {
        StringBuilder sb = new StringBuilder("health-")
                .append(UUID.randomUUID().toString().replace("-", ""))
                .append(".yml");
        return sb.toString();
    }
}
