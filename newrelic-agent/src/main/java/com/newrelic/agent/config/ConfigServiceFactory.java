/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.ForceDisconnectException;
import com.newrelic.agent.agentcontrol.AgentHealth;
import com.newrelic.agent.agentcontrol.AgentControlIntegrationUtils;
import com.newrelic.api.agent.Logger;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Level;

public class ConfigServiceFactory {

    @VisibleForTesting
    public static ConfigService createConfigService(AgentConfig config, Map<String, Object> localSettings) {
        return new ConfigServiceImpl(config, null, localSettings, false);
    }

    @VisibleForTesting
    public static ConfigService createConfigServiceUsingSettings(Map<String, Object> settings) {
        Map<String, Object> deObfuscatedSettings = new ObscuringConfig(
                settings, AgentConfigImpl.SYSTEM_PROPERTY_ROOT).getDeobscuredProperties();
        return new ConfigServiceImpl(AgentConfigImpl.createAgentConfig(deObfuscatedSettings), null, deObfuscatedSettings, false);
    }

    public static ConfigService createConfigService(Logger log, boolean checkConfig) throws ConfigurationException, ForceDisconnectException {
        File configFile = getConfigFile(log);
        Map<String, Object> configSettings = null;
        ConfigurationException fileParseException = null;
        try {
            configSettings = getConfigurationFileSettings(configFile, log);
        } catch (ConfigurationException ce) {
            fileParseException = ce;
        }
        Map<String, Object> deObfuscatedSettings = new ObscuringConfig(
                configSettings, AgentConfigImpl.SYSTEM_PROPERTY_ROOT).getDeobscuredProperties();
        AgentConfig config = AgentConfigImpl.createAgentConfig(deObfuscatedSettings);

        validateConfig(config, fileParseException);
        return new ConfigServiceImpl(config, configFile, deObfuscatedSettings, checkConfig);
    }

    public static Map<String, Object> getConfigurationFileSettings(File configFile, Logger log) throws ConfigurationException {
        if (configFile != null) {
            log.log(Level.INFO, "New Relic Agent: Loading configuration file \"{0}\"", configFile.getPath());

            try {
                return AgentConfigHelper.getConfigurationFileSettings(configFile);
            } catch (Exception e) {
                String msg = MessageFormat.format(
                        "An error occurred reading the configuration file {0}. Check the permissions and format of the file. - {1}",
                        configFile.getAbsolutePath(), e.toString());
                throw new ConfigurationException(msg, e);
            }
        }

        return null;
    }

    /**
     * Get the Agent's configuration file.
     *
     * @return the configuration file
     */
    private static File getConfigFile(Logger log) {
        File configFile = ConfigFileHelper.findConfigFile();
        if (configFile == null) {
            log.log(Level.INFO, "Configuration file not found. The agent will attempt to read required values from environment variables.");
        }
        return configFile;
    }

    @VisibleForTesting
    public static void validateConfig(AgentConfig config, ConfigurationException fileParseException) throws ConfigurationException, ForceDisconnectException {
        if (fileParseException != null) {
            AgentControlIntegrationUtils.reportUnhealthyStatusPriorToServiceStart(config, AgentHealth.Status.CONFIG_FILE_PARSE_ERROR);
            throw fileParseException;
        }
        if (config.getApplicationName() == null) {
            AgentControlIntegrationUtils.reportUnhealthyStatusPriorToServiceStart(config, AgentHealth.Status.MISSING_APP_NAME);
            throw new ConfigurationException("The agent requires an application name. Check the app_name setting in newrelic.yml");
        }
        if (config.getApplicationNames().size() > 3) {
            AgentControlIntegrationUtils.reportUnhealthyStatusPriorToServiceStart(config, AgentHealth.Status.MAX_APP_NAMES_EXCEEDED);
            throw new ConfigurationException("The agent does not support more than three application names. Check the app_name setting in newrelic.yml");
        }
        if (config.isHighSecurity() && config.laspEnabled()) {
            throw new ForceDisconnectException("Security Policies and High Security Mode cannot both be present in the agent configuration. " +
                    "If Security Policies have been set for your account, please ensure the security_policies_token " +
                    "is set but high_security is disabled (default).");
        }
    }

}
