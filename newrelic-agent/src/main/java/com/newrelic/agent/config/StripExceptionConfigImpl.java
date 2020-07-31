/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.annotations.VisibleForTesting;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StripExceptionConfigImpl extends BaseConfig implements StripExceptionConfig {

    public static final String ENABLED = "enabled";

    @Deprecated
    public static final String WHITELIST = "whitelist";

    public static final String ALLOWED_CLASSES = "allowed_classes";
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + AgentConfigImpl.STRIP_EXCEPTION_MESSAGES + ".";

    private final boolean isEnabled;
    private final Set<String> allowedClasses;

    private StripExceptionConfigImpl(Map<String, Object> props, boolean highSecurity) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = getProperty(ENABLED, highSecurity);

        Set<String> allowedClasses = new HashSet<>(getUniqueStrings(ALLOWED_CLASSES));

        addDeprecatedProperty(
                new String[] { AgentConfigImpl.STRIP_EXCEPTION_MESSAGES, WHITELIST },
                new String[] { AgentConfigImpl.STRIP_EXCEPTION_MESSAGES, ALLOWED_CLASSES }
        );
        if (allowedClasses.isEmpty()) {
            allowedClasses.addAll(getUniqueStrings(WHITELIST));
        }

        this.allowedClasses = Collections.unmodifiableSet(new HashSet<>(allowedClasses));
    }

    @VisibleForTesting
    public StripExceptionConfigImpl(boolean enabled, Set<String> allowedClasses) {
        super(Collections.<String, Object>emptyMap());
        this.isEnabled = enabled;
        this.allowedClasses = allowedClasses == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(allowedClasses);
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }

    static StripExceptionConfig createStripExceptionConfig(Map<String, Object> settings, boolean highSecurity) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new StripExceptionConfigImpl(settings, highSecurity);
    }
}
