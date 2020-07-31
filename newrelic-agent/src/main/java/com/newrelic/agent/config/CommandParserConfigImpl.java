/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommandParserConfigImpl extends BaseConfig implements CommandParserConfig {
    public static final String ROOT = "command_parser";
    public static final String ENABLED = "enabled";
    public static final String DISALLOW = "disallow";

    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static final String BLACKLIST = "blacklist";

    private static final boolean DEFAULT_ENABLED = true;

    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config." + ROOT + ".";

    private final boolean enabled;
    private final Set<String> disallowedCommands;

    public CommandParserConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        enabled = getProperty(ENABLED, DEFAULT_ENABLED);
        Set<String> disallowedList = new HashSet<>(getUniqueStrings(DISALLOW));

        addDeprecatedProperty(
                new String[] { ROOT, BLACKLIST },
                new String[] { ROOT, DISALLOW }
        );
        if (disallowedList.isEmpty()) {
            disallowedList.addAll(getUniqueStrings(BLACKLIST));
        }

        disallowedCommands = Collections.unmodifiableSet(disallowedList);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Set<String> getDisallowedCommands() {
        return disallowedCommands;
    }
}
