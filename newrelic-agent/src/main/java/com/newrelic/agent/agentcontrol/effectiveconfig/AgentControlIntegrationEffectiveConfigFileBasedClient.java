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
import static com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils.gzipFile;

public class AgentControlIntegrationEffectiveConfigFileBasedClient implements AgentControlIntegrationEffectiveConfigClient {
    private Yaml yamlWriter;
    private File effectiveConfigFile;
    private boolean isValid = false;

    public AgentControlIntegrationEffectiveConfigFileBasedClient(AgentControlIntegrationConfig agentConfig) {
        URI locationFromConfig = agentConfig.getEffectiveConfigDeliveryLocation();
        File fileFolder = createAgentControlFileFolderInstance(locationFromConfig, AgentControlIntegrationUtils.FileType.effective_config);
        if (fileFolder != null) {
            this.effectiveConfigFile = new File(fileFolder,
                    generateAgentControlFilename(AgentControlIntegrationUtils.FileType.effective_config, false));

            yamlWriter = createYamlWriter();
            isValid = true;
        }
    }

    @Override
    public void sendEffectiveConfigMessage(Map<String, Object> config) {
        writeMapPayloadToFile(config, effectiveConfigFile, yamlWriter,
                AgentControlIntegrationUtils.FileType.effective_config);

        // If the resulting file is greater than 1000 bytes, we gzip the contents
        // to mitigate resource concerns when running in k8s environments
        if (effectiveConfigFile != null && effectiveConfigFile.length() > 1000) {
            // A bit of a fail-safe -- if the gzip process fails, we leave the original
            // file in place so we still report the config up
            if (gzipFile(effectiveConfigFile)) {
                effectiveConfigFile.delete();
            }
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }
}
