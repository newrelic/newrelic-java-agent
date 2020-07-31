/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.Map;

public class ExtensionsConfigImpl extends BaseConfig implements ExtensionsConfig {
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.extensions.";
    public static final String DIRECTORY = "dir";
    public static final String RELOAD_MODIFIED = "reload_modified";

    private String directory;
    private boolean reloadModified;

    public ExtensionsConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        directory = getProperty(DIRECTORY);
        reloadModified = getProperty(RELOAD_MODIFIED, true);

    }

    static ExtensionsConfigImpl createExtensionsConfig(Map<String, Object> settings) {
        return new ExtensionsConfigImpl(settings);
    }

    @Override
    public String getDirectory() {
        return directory;
    }

    @Override
    public boolean shouldReloadModified() {
        return reloadModified;
    }
}
