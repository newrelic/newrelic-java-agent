/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

public class SystemPropertyProvider {

    // environment variables
    private static final String NEW_RELIC_PREFIX_ENV = "NEW_RELIC_";
    private static final String LOG_ENV = NEW_RELIC_PREFIX_ENV + "LOG";
    private static final String LOG_FILE_NAME = AgentConfigImpl.SYSTEM_PROPERTY_ROOT + AgentConfigImpl.LOG_FILE_NAME;
    private static final String NEW_RELIC_SYSTEM_PROPERTY_ROOT = "newrelic.";

    private final Map<String, String> envVarToSystemPropKeyMap;
    private final Map<String, String> newRelicSystemProps;
    private final Map<String, Object> newRelicPropsWithoutPrefix;
    private final Map<String, Object> newRelicEnvVarsWithoutPrefix;
    private final SystemProps systemProps;
    private final EnvironmentFacade environmentFacade;

    public SystemPropertyProvider() {
        this(SystemProps.getSystemProps(), EnvironmentFacade.getInstance());
    }

    public SystemPropertyProvider(SystemProps sysProps, EnvironmentFacade environmentFacade) {
        systemProps = sysProps;
        this.environmentFacade = environmentFacade;
        envVarToSystemPropKeyMap = initEnvVarToSystemPropMap();
        newRelicSystemProps = initNewRelicSystemProperties();
        newRelicPropsWithoutPrefix = createNewRelicSystemPropertiesWithoutPrefix();
        newRelicEnvVarsWithoutPrefix = createNewRelicEnvVarsWithoutPrefix();
    }

    private Map<String, String> initEnvVarToSystemPropMap() {
        // general environment variables, originally added for Heroku
        Map<String, String> envVars = new HashMap<>();
        envVars.put(LOG_ENV, LOG_FILE_NAME);
        return envVars;
    }

    /**
     * Get the New Relic system properties.
     */
    private Map<String, String> initNewRelicSystemProperties() {
        Map<String, String> nrProps = new HashMap<>();
        try {
            for (Entry<Object, Object> entry : systemProps.getAllSystemProperties().entrySet()) {
                String key = entry.getKey().toString();
                if (key.startsWith(NEW_RELIC_SYSTEM_PROPERTY_ROOT)) {
                    String val = entry.getValue().toString();
                    nrProps.put(key, val);
                }
            }
        } catch (SecurityException t) {
            Agent.LOG.log(Level.FINE, "Unable to get system properties");
        }
        return Collections.unmodifiableMap(nrProps);
    }

    private Map<String, Object> createNewRelicSystemPropertiesWithoutPrefix() {
        Map<String, Object> nrProps = new HashMap<>();
        addNewRelicSystemProperties(nrProps, systemProps.getAllSystemProperties());
        return Collections.unmodifiableMap(nrProps);
    }

    private Map<String, Object> createNewRelicEnvVarsWithoutPrefix() {
        Map<String, Object> nrEnv = new HashMap<>();
        addNewRelicEnvProperties(nrEnv, environmentFacade.getAllEnvProperties());
        return Collections.unmodifiableMap(nrEnv);
    }

    private void addNewRelicSystemProperties(Map<String, Object> nrProps, Properties allSysProps) {
        for (Entry<Object, Object> entry : allSysProps.entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith(AgentConfigImpl.SYSTEM_PROPERTY_ROOT)) {
                addPropertyWithoutSystemPropRoot(nrProps, key, entry.getValue().toString());
            }
        }
    }

    private void addNewRelicEnvProperties(Map<String, Object> nrProps, Map<String, String> allEnvVars) {
        for (Map.Entry<String, String> entry : allEnvVars.entrySet()) {
            String envVar = entry.getKey();
            if (envVar.startsWith(NEW_RELIC_PREFIX_ENV)) {
                String envVarNameToReplace = envVarToSystemPropKeyMap.get(envVar);
                if (envVarNameToReplace != null) {
                    addPropertyWithoutSystemPropRoot(nrProps, envVarNameToReplace, entry.getValue());
                } else {
                    addPropertyWithoutEnvPrefix(nrProps, envVar.toLowerCase(), entry.getValue());
                }
            } else if (envVar.startsWith(AgentConfigImpl.SYSTEM_PROPERTY_ROOT)) {
                Agent.LOG.log(Level.WARNING,
                        "The agent only supports environment variable configurations consisting of" +
                                " alphanumeric characters and underscores. Use {0} instead.",
                        formatNewRelicEnvVarPrefix(envVar));
            }
        }
    }

    private void addPropertyWithoutEnvPrefix(Map<String, Object> nrProps, String key, Object value) {
        nrProps.put(key.substring(NEW_RELIC_PREFIX_ENV.length()), value);
    }

    private void addPropertyWithoutSystemPropRoot(Map<String, Object> nrProps, String key, Object value) {
        nrProps.put(key.substring(AgentConfigImpl.SYSTEM_PROPERTY_ROOT.length()), value);
    }

    public String getEnvironmentVariable(String key) {
        // compatibility: map NEW_RELIC_LOG to newrelic.config.log_file_name
        // The latter is the general name. NEW_RELIC_LOG_FILE_NAME, which
        // also works, will be checked below.
        if (LOG_FILE_NAME.equals(key)) {
            String logFileVal = environmentFacade.getenv(LOG_ENV);
            if (logFileVal != null) {
                return logFileVal;
            }
        }

        return getenv(key);
    }

    private String getenv(String key) {
        //check if current key needs to be converted from NR config prop to NR env var

        return environmentFacade.getenv(formatNewRelicEnvVarPrefix(key));
    }

    private String formatNewRelicEnvVarPrefix(String key) {
        // Replace any dots and dashes with underscores to allow config to be set as environment variables. We are replacing dashes here because
        // our instrumentation modules have dashes in their names and we want to be able to allow those to be disabled via environment variables.
        return key.replace("newrelic.config", "new.relic")
                .replaceAll("[.-]", "_")
                .toUpperCase();
    }

    public String getSystemProperty(String prop) {
        return systemProps.getSystemProperty(prop);
    }

    /**
     * Get a map of the New Relic system properties (any property starting with newrelic.)
     */
    public Map<String, String> getNewRelicSystemProperties() {
        return newRelicSystemProps;
    }

    /**
     * Returns the New Relic system properties with the 'newrelic.config.' prefixes removed.
     */
    public Map<String, Object> getNewRelicPropertiesWithoutPrefix() {
        return newRelicPropsWithoutPrefix;
    }

    /**
     * Returns the New Relic environment properties with the 'NEW_RELIC_' prefixes removed.
     */
    public Map<String, Object> getNewRelicEnvVarsWithoutPrefix() {
        return newRelicEnvVarsWithoutPrefix;
    }

}
