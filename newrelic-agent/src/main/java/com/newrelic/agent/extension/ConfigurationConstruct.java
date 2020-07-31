/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import org.yaml.snakeyaml.constructor.AbstractConstruct;

public abstract class ConfigurationConstruct extends AbstractConstruct {
    private final String name;

    public ConfigurationConstruct(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
