/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;

import com.google.common.collect.ImmutableList;

public class Jar implements JSONStreamAware, Cloneable {

    /** The name of the jar. */
    private final String name;
    /** The jar info. */
    private final JarInfo jarInfo;

    /**
     * 
     * Creates this Jar.
     */
    public Jar(String name, JarInfo jarInfo) {
        super();
        this.name = name;
        this.jarInfo = jarInfo;
    }

    /**
     * Gets the field name.
     */
    protected String getName() {
        return name;
    }

    /**
     * Gets the field version.
     */
    protected String getVersion() {
        return jarInfo.version;
    }
    
    public JarInfo getJarInfo() {
        return jarInfo;
    }

    @Override
    public void writeJSONString(Writer pWriter) throws IOException {
        List<Object> toSend = ImmutableList.of(name, jarInfo.version, jarInfo.attributes);

        JSONArray.writeJSONString(toSend, pWriter);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getVersion() == null) ? 0 : getVersion().hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        Jar other = (Jar) obj;
        if (getVersion() == null) {
            if (other.getVersion() != null)
                return false;
        } else if (!getVersion().equals(other.getVersion()))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
