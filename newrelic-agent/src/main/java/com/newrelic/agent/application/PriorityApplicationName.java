/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.application;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import com.newrelic.agent.config.BaseConfig;
import com.newrelic.api.agent.ApplicationNamePriority;

/**
 * A name that participates in the priority application naming rules. This class is immutable and therefore threadsafe.
 */
public class PriorityApplicationName {

    public static final PriorityApplicationName NONE = PriorityApplicationName.create(null,
            ApplicationNamePriority.NONE);

    private final ApplicationNamePriority priority;
    private final String name;
    private final List<String> names;

    private PriorityApplicationName(String name, ApplicationNamePriority priority) {
        this.priority = priority;
        if (name == null) {
            this.name = null;
            names = null;
        } else {
            names = Collections.unmodifiableList(BaseConfig.getUniqueStringsFromString(name,
                    BaseConfig.SEMI_COLON_SEPARATOR));
            this.name = names.get(0);
        }
    }

    public String getName() {
        return name;
    }

    public List<String> getNames() {
        return names;
    }

    public ApplicationNamePriority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}[name={1}, priority={2}]", getClass().getName(), getName(), getPriority());
    }

    public static PriorityApplicationName create(String name, ApplicationNamePriority priority) {
        return new PriorityApplicationName(name, priority);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PriorityApplicationName other = (PriorityApplicationName) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (priority != other.priority) {
            return false;
        }
        return true;
    }

}
