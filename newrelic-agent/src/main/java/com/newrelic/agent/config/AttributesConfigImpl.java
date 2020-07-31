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

    public static final String[] DEFAULT_BROWSER_EXCLUDES = new String[] {
            AttributeNames.DISPLAY_HOST,
            AttributeNames.HTTP_REQUEST_STAR,
            AttributeNames.HTTP_STATUS_MESSAGE,
            AttributeNames.HTTP_STATUS,
            AttributeNames.INSTANCE_NAME,
            AttributeNames.JVM_STAR,
            AttributeNames.MESSAGE_REQUEST_STAR,
            AttributeNames.REQUEST_REFERER_PARAMETER_NAME,
            AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME,
            AttributeNames.REQUEST_HOST_PARAMETER_NAME,
            AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME,
            AttributeNames.REQUEST_METHOD_PARAMETER_NAME,
            AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME,
            AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME,
            AttributeNames.SOLR_STAR,
    };

    public static final String[] DEFAULT_TRANSACTION_EVENTS_EXCLUDES = new String[] {
            AttributeNames.HTTP_REQUEST_STAR,
            AttributeNames.HTTP_STATUS_MESSAGE,
            AttributeNames.JVM_STAR,
            AttributeNames.MESSAGE_REQUEST_STAR,
            AttributeNames.SOLR_STAR
    };

    // request parameters and message parameters are turned off by default - this is done in attributes filter
    public static final String[] DEFAULT_ERROR_EVENTS_EXCLUDES = new String[] {};
    public static final String[] DEFAULT_TRANSACTION_TRACES_EXCLUDES = new String[] {};
    public static final String[] DEFAULT_TRANSACTION_SEGMENTS_EXCLUDES = new String[] {};
    public static final String[] DEFAULT_SPAN_EVENTS_EXCLUDES = new String[] {};

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

        if (dest.equals(AgentConfigImpl.ATTRIBUTES)) {
            return enabledRoot;
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
