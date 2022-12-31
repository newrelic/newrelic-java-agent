/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.attributes;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AttributesConfig;
import com.newrelic.agent.config.AttributesConfigImpl;
import com.newrelic.agent.config.BaseConfig;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class DestinationFilter {

    private final boolean isEnabled;
    private final DestinationPredicate filter;

    public DestinationFilter(String mainNameForFilter, boolean defaultInclude, AgentConfig config,
            String[] defaultExclude, String... namesForIsEnabled) {
        isEnabled = config.getAttributesConfig().isAttsEnabled(config, defaultInclude, namesForIsEnabled);
        Agent.LOG.log(Level.FINE, "Attributes are {0} for {1}", isEnabled ? "enabled" : "disabled", mainNameForFilter);
        AttributesConfig attributesConfig = config.getAttributesConfig();
        List<String> rootExcludes = attributesConfig.attributesRootExclude();
        List<String> rootIncludes = attributesConfig.attributesRootInclude();
        filter = getDestinationPredicate(isEnabled, config, rootExcludes, rootIncludes, mainNameForFilter, updateDefaults(defaultExclude));
    }

    private static DestinationPredicate getDestinationPredicate(boolean isEnabled, AgentConfig config,
            List<String> rootExcludes, List<String> rootIncludes, String name, Set<String> defaultExclude) {
        if (isEnabled) {
            Set<String> configExclude = getExcluded(config, rootExcludes, name);
            Set<String> configInclude = Collections.emptySet();
            if (!config.isHighSecurity()) {
                configInclude = getIncluded(config, rootIncludes, name);
            }
            return new DefaultDestinationPredicate(name, configExclude, configInclude, defaultExclude, getMandatoryExcludes(config));
        } else {
            return new DisabledDestinationPredicate();
        }
    }

    private static Set<String> getMandatoryExcludes(AgentConfig config) {
        HashSet<String> excludes = new HashSet<>();
        if (config.isHighSecurity()) {
            excludes.add(AttributeNames.MESSAGE_REQUEST_STAR);
            excludes.add(AttributeNames.HTTP_REQUEST_STAR);
        }
        return excludes;
    }

    private static Set<String> updateDefaults(String[] defaultExclude) {
        Set<String> defaultExc = Sets.newHashSet(defaultExclude);
        defaultExc.add("request.parameters.*");
        defaultExc.add("message.parameters.*");
        return defaultExc;
    }

    protected static Set<String> getExcluded(AgentConfig config, List<String> baseList, String dest) {
        Set<String> output = new HashSet<>();
        output.addAll(baseList);
        output.addAll(getBaseList(config, dest + "." + AttributesConfigImpl.ATTS_EXCLUDE));
        return output;
    }

    protected static Set<String> getIncluded(AgentConfig config, List<String> baseList, String dest) {
        Set<String> output = new HashSet<>();
        output.addAll(baseList);
        output.addAll(getBaseList(config, dest + "." + AttributesConfigImpl.ATTS_INCLUDE));
        return output;
    }

    protected static List<String> getBaseList(AgentConfig config, String key) {
        Object val = config.getValue(key);
        if (val instanceof String) {
            return BaseConfig.getUniqueStringsFromString((String) val, BaseConfig.LIST_ITEM_SEPARATOR);
        }
        if (val instanceof Collection<?>) {
            return BaseConfig.getUniqueStringsFromCollection((Collection<?>) val, null);
        }
        return Collections.emptyList();
    }

    protected boolean isPotentialConfigMatch(String paramStart) {
        return filter.isPotentialConfigMatch(paramStart);
    }

    protected boolean isEnabled() {
        return isEnabled;
    }

    protected Map<String, ?> filterAttributes(Map<String, ?> values) {
        return filterAttributes(values, filter);
    }

    private Map<String, ?> filterAttributes(Map<String, ?> values, DestinationPredicate predicate) {
        return (isEnabled && values != null && !values.isEmpty() ? Maps.filterKeys(values, predicate) : Collections.<String, Object>emptyMap());
    }

    public boolean shouldIncludeAttribute(String attributeName) {
        return filter.apply(attributeName);
    }
}
