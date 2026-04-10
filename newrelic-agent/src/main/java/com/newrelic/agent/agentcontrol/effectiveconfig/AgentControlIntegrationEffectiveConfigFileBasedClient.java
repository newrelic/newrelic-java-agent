package com.newrelic.agent.agentcontrol.effectiveconfig;

import com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils;
import com.newrelic.agent.config.agentcontrol.AgentControlIntegrationConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URI;
import java.util.Map;

import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.createAgentControlFileFolderInstance;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.createYamlWriter;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.generateAgentControlFilename;
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.writeMapPayloadToFile;

public class AgentControlIntegrationEffectiveConfigFileBasedClient implements AgentControlIntegrationEffectiveConfigClient {
    private Yaml yamlWriter;
    private File effectiveConfigFile;
    private boolean isValid = false;

    public AgentControlIntegrationEffectiveConfigFileBasedClient(AgentControlIntegrationConfig agentConfig) {
        URI locationFromConfig = agentConfig.getEffectiveConfigDeliveryLocation();
        File fileFolder = createAgentControlFileFolderInstance(locationFromConfig, AgentControlIntegrationUtils.FileType.effective_config);
        if (fileFolder != null) {
            this.effectiveConfigFile = new File(fileFolder,
                    generateAgentControlFilename(AgentControlIntegrationUtils.FileType.effective_config));

            yamlWriter = createYamlWriter();
            isValid = true;
        }
    }

    @Override
    public void sendEffectiveConfigMessage(Map<String, Object> config) {
        writeMapPayloadToFile(config, effectiveConfigFile, yamlWriter,
                AgentControlIntegrationUtils.FileType.effective_config);
    }

    @Override
    public boolean isValid() {
        return isValid;
    }
}
