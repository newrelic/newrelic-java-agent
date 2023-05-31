/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.attributes.AttributeNames;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class AttributesConfigImpl extends BaseConfig implements AttributesConfig {

    private static final boolean DEFAULT_ENABLED = true;
    private static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.attributes.";

    public static final String ENABLED = "enabled";
    public static final String INCLUDE = "include";
    public static final String EXCLUDE = "exclude";
    public static final String ATTS_ENABLED = "attributes.enabled";
    public static final String ATTS_EXCLUDE = "attributes.exclude";
    public static final String ATTS_INCLUDE = "attributes.include";

    private final boolean enabledRoot;
    private final List<String> attributesInclude;
    private final List<String> attributeExclude;

    public AttributesConfigImpl(Map<String, Object> pProps) {
        super(pProps, SYSTEM_PROPERTY_ROOT);
        enabledRoot = initEnabled();
        attributesInclude = initAttributesInclude();
        attributeExclude = initAttributesExclude();
    }

    private boolean initEnabled() {
        return getProperty(ENABLED, DEFAULT_ENABLED);
    }

    private List<String> initAttributesInclude() {
        return new ArrayList<>(getUniqueStrings(INCLUDE, COMMA_SEPARATOR));
    }

    private List<String> initAttributesExclude() {
        return new ArrayList<>(getUniqueStrings(EXCLUDE, COMMA_SEPARATOR));
    }

    @Override
    public boolean isEnabledRoot() {
        return enabledRoot;
    }

    @Override
    public List<String> attributesRootInclude() {
        return attributesInclude;
    }

    @Override
    public List<String> attributesRootExclude() {
        return attributeExclude;
    }

    @Override
    public boolean isAttsEnabled(AgentConfig config, boolean defaultProp, String... dest) {
        if (!enabledRoot) {
            return false;
        }

        boolean toEnable = false;
        Boolean destEnabled;
        for (String current : dest) {
            destEnabled = getBooleanValue(config, current + "." + ATTS_ENABLED);
            if (destEnabled != null) {
                if (!destEnabled) {
                    return destEnabled;
                } else {
                    toEnable = true;
                }
            }
        }

        // the root property is not used unless it is false
        return (toEnable || defaultProp);
    }

    private static Boolean getBooleanValue(AgentConfig config, String value) {
        try {
            Object inputObj = config.getValue(value);
            if (inputObj != null) {
                if (inputObj instanceof Boolean) {
                    return ((Boolean) inputObj);
                } else if (inputObj instanceof String) {
                    return Boolean.parseBoolean((String) inputObj);
                }
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, MessageFormat.format("The configuration property {0} should be a boolean but is not.", value));
        }
        return null;
    }

    static AttributesConfigImpl createAttributesConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new AttributesConfigImpl(settings);
    }
}
