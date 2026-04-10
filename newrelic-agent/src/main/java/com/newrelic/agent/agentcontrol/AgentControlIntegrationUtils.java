/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.agentcontrol;

import com.newrelic.agent.Agent;
import com.newrelic.agent.agentcontrol.health.AgentControlIntegrationHealthClient;
import com.newrelic.agent.agentcontrol.health.AgentHealth;
import com.newrelic.agent.agentcontrol.health.HealthDataChangeListener;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class AgentControlIntegrationUtils {
    public enum FileType {
        health, effective_config
    }

    public static long getPseudoCurrentTimeNanos() {
        // The message expects the time in nanoseconds. Since this is a practical impossibility on most hardware,
        // simply get the current ms and multiply.
        return System.currentTimeMillis() * 1000000;
    }

    public static void reportUnhealthyStatus(List<HealthDataChangeListener> healthDataChangeListeners,
            AgentHealth.Status newStatus, String... additionalInfo) {
        for (HealthDataChangeListener listener : healthDataChangeListeners) {
            listener.onUnhealthyStatus(newStatus, additionalInfo);
        }
    }

    public static void reportHealthyStatus(List<HealthDataChangeListener> healthDataChangeListeners, AgentHealth.Category... categories) {
        for (HealthDataChangeListener listener : healthDataChangeListeners) {
            listener.onHealthyStatus(categories);
        }
    }

    public static void assignEntityGuid(List<HealthDataChangeListener> healthDataChangeListeners, String connectPayloadJson) {
        if (connectPayloadJson != null) {
            JSONObject payload = (JSONObject) JSONValue.parse(connectPayloadJson);
            JSONObject returnValueObject = (JSONObject) payload.get("return_value");
            for (HealthDataChangeListener listener : healthDataChangeListeners) {
                listener.assignEntityGuid((String) returnValueObject.get("entity_guid"));
            }
        }
    }

    public static void reportUnhealthyStatusPriorToServiceStart(AgentConfig config, AgentHealth.Status status) {
        AgentControlIntegrationConfig agentControlIntegrationConfig = config.getAgentControlIntegrationConfig();
        if (agentControlIntegrationConfig.isEnabled()) {
            AgentControlIntegrationHealthClient client = AgentControlIntegrationClientFactory.createHealthClient(agentControlIntegrationConfig);
            AgentHealth agentHealth = new AgentHealth(getPseudoCurrentTimeNanos());
            agentHealth.setUnhealthyStatus(status);
            client.sendHealthMessage(agentHealth);
        }
    }

    public static void writeMapPayloadToFile(Map<String, Object> contents, File target,
            Yaml yamlWriter, FileType fileType) {
        if (target != null) {
            try {
                FileWriter fw = new FileWriter(target);
                yamlWriter.dump(contents, fw);
                fw.close();

                if (Agent.LOG.isFinestEnabled() && Agent.isDebugEnabled()) {
                    Agent.LOG.log(Level.FINEST, "Wrote agent control {0} file: {1}", fileType, target.getAbsolutePath());
                }
            } catch (IOException e) {
                Agent.LOG.log(Level.WARNING, "Error writing agent control {0} message to file: {1}", fileType, e.getMessage());
            }
        }
    }

    public static String generateAgentControlFilename(FileType fileType) {
        StringBuilder sb = new StringBuilder(fileType + "-")
                .append(UUID.randomUUID().toString().replace("-", ""))
                .append(".yml");
        return sb.toString();
    }

    public static File createAgentControlFileFolderInstance(URI location, FileType fileType) {
        File fileFolder = null;
        if (location != null) {
            fileFolder = new File(location);
            if (!(fileFolder.isDirectory() && fileFolder.canWrite())) {
                Agent.LOG.log(Level.WARNING, "agent_control." + fileType + ".delivery_location is not a valid folder. " +
                                "Messages will not be generated.  Configured location: {0}  isFolder: {1}  canWrite: {2}",
                        fileFolder.getAbsolutePath(), fileFolder.isDirectory(), fileFolder.canWrite());
                fileFolder = null;
            }
        }

        return fileFolder;
    }

    public static Yaml createYamlWriter() {
        DumperOptions yamlDumperOptions = new DumperOptions();
        yamlDumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        yamlDumperOptions.setPrettyFlow(true);
        return new Yaml(yamlDumperOptions);
    }
}
