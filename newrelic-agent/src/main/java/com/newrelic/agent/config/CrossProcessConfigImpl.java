/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.util.Obfuscator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CrossProcessConfigImpl extends BaseConfig implements CrossProcessConfig {

    public static final String CROSS_APPLICATION_TRACING = "cross_application_tracing"; // deprecated enabled setting
    public static final String CROSS_PROCESS_ID = "cross_process_id";
    public static final String ENABLED = "enabled";
    public static final String ENCODING_KEY = "encoding_key";
    public static final String TRUSTED_ACCOUNT_IDS = "trusted_account_ids";
    public static final String APPLICATION_ID = "application_id";
    public static final boolean DEFAULT_ENABLED = false;
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.cross_application_tracer.";

    private final boolean isCrossApplicationTracing;
    private final String crossProcessId;
    private final String encodingKey;
    private final String encodedCrossProcessId;
    private final String applicationId;
    private final Set<String> trustedIds;

    private CrossProcessConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        trustedIds = Collections.unmodifiableSet(new HashSet<>(getUniqueStrings(TRUSTED_ACCOUNT_IDS)));
        isCrossApplicationTracing = initEnabled();
        crossProcessId = getProperty(CROSS_PROCESS_ID);
        encodingKey = getProperty(ENCODING_KEY);
        encodedCrossProcessId = initEncodedCrossProcessId(crossProcessId, encodingKey);
        applicationId = getProperty(APPLICATION_ID);
    }

    private boolean initEnabled() {
        Boolean enabled = getProperty(ENABLED);
        if (enabled != null) {
            return enabled;
        }
        // honor the deprecated property
        return getProperty(CROSS_APPLICATION_TRACING, DEFAULT_ENABLED);
    }

    private String initEncodedCrossProcessId(String crossProcessId, String encodingKey) {
        if (crossProcessId == null || encodingKey == null) {
            return null;
        }

        return Obfuscator.obfuscateNameUsingKey(crossProcessId, encodingKey);
    }

    @Override
    public boolean isCrossApplicationTracing() {
        return isCrossApplicationTracing;
    }

    @Override
    public String getCrossProcessId() {
        return isCrossApplicationTracing ? crossProcessId : null;
    }

    @Override
    public String getApplicationId() {
        return applicationId;
    }

    @Override
    public String getEncodedCrossProcessId() {
        return isCrossApplicationTracing ? encodedCrossProcessId : null;
    }

    @Override
    public String getEncodingKey() {
        return isCrossApplicationTracing ? encodingKey : null;
    }

    @Override
    public String getSyntheticsEncodingKey() {
        return encodingKey;
    }

    @Override
    public boolean isTrustedAccountId(String accountId) {
        return trustedIds.contains(accountId);
    }

    public static CrossProcessConfig createCrossProcessConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new CrossProcessConfigImpl(settings);
    }

}
