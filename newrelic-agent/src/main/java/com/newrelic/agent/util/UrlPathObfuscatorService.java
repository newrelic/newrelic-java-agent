/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.util;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.config.UrlPathObfuscationConfig;
import com.newrelic.agent.service.AbstractService;
import com.newrelic.agent.service.ServiceFactory;

import java.util.regex.Pattern;

/**
 * Service used to obfuscate URL paths based on the supplied UrlPathObfuscationConfig
 * values.
 */
public class UrlPathObfuscatorService extends AbstractService implements AgentConfigListener {
    private volatile ConfigHolder configHolder;

    public UrlPathObfuscatorService() {
        super(UrlPathObfuscatorService.class.getSimpleName());
        configHolder = new ConfigHolder(ServiceFactory.getConfigService().getDefaultAgentConfig().getUrlPathObfuscationConfig());
        ServiceFactory.getConfigService().addIAgentConfigListener(this);
    }

    @Override
    public boolean isEnabled() {
        return configHolder.isEnabled;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
        ServiceFactory.getConfigService().removeIAgentConfigListener(this);
    }

    /**
     * Obfuscate the supplied path if URL path obfuscation is enabled and the path is not null
     *
     * @param path the path to obfuscate
     *
     * @return the obfuscated path or the path instance, unchanged, if obfuscation is disabled
     */
    public String obfuscatePath(String path) {
        if (configHolder.isEnabled && configHolder.regExPattern != null && path != null) {
            return configHolder.regExPattern.matcher(path).replaceAll(configHolder.regExReplacement);
        }

        return path;
    }

    @Override
    public void configChanged(String appName, AgentConfig agentConfig) {
        configHolder = new ConfigHolder(agentConfig.getUrlPathObfuscationConfig());
    }

    private static final class ConfigHolder {
        private final boolean isEnabled;
        private final Pattern regExPattern;
        private final String regExReplacement;

        ConfigHolder(UrlPathObfuscationConfig config) {
            isEnabled = config != null && config.isEnabled();
            regExPattern = config == null ? null : config.getRegExPattern();
            regExReplacement = config == null ? "" : config.getRegExReplacement();
        }
    }
}
