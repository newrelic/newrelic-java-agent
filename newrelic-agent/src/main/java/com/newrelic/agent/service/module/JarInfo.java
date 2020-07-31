/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

class JarInfo {

    static final JarInfo MISSING = new JarInfo(JarCollectorServiceProcessor.UNKNOWN_VERSION,
            ImmutableMap.<String, String> of());

    public final String version;
    public final Map<String, String> attributes;

    public JarInfo(String version, Map<String, String> attributes) {
        // add unknown version if version is null
        this.version = version == null ? JarCollectorServiceProcessor.UNKNOWN_VERSION : version;
        this.attributes = attributes == null ? ImmutableMap.<String, String> of() : ImmutableMap.copyOf(attributes);
    }

    @Override
    public String toString() {
        return "JarInfo [version=" + version + ", attributes=" + attributes + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        JarInfo other = (JarInfo) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

}