/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.util.Strings;
import org.objectweb.asm.commons.Remapper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyRemapper extends Remapper {
    private final Set<String> prefixes;
    private final Map<String, String> oldToNew = new HashMap<>();
    static final String DEPENDENCY_PREFIX = Strings.NEWRELIC_DEPENDENCY_INTERNAL_PACKAGE_PREFIX;

    public DependencyRemapper(Set<String> prefixes) {
        super();
        this.prefixes = fix(prefixes);
    }

    /**
     * Jarjar rewrites the prefixes that are passed in to have the DEPENDENCY_PREFIX prefix. We need to remove that.
     * 
     * @param prefixes
     */
    private static Set<String> fix(Set<String> prefixes) {
        Set<String> fixed = new HashSet<>();
        for (String prefix : prefixes) {

            if (prefix.startsWith(DEPENDENCY_PREFIX)) {
                fixed.add(prefix.substring(DEPENDENCY_PREFIX.length()));
            } else {
                fixed.add(prefix);
            }
        }
        return ImmutableSet.copyOf(fixed);
    }

    @Override
    public String map(String typeName) {
        for (String prefix : prefixes) {
            if (typeName.startsWith(prefix)) {
                String newType = DEPENDENCY_PREFIX + typeName;
                oldToNew.put(typeName, newType);
                return newType;
            }
        }
        return super.map(typeName);
    }

    public Map<String, String> getRemappings() {
        return oldToNew;
    }

    Set<String> getPrefixes() {
        return prefixes;
    }

}
