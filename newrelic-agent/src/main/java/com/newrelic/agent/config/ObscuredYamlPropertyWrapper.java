/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.util.Obfuscator;

/**
 * This is a wrapper class indicating that the value provided in the yaml is expected to be an obscured value.
 */
public class ObscuredYamlPropertyWrapper {
    private final String value;

    public ObscuredYamlPropertyWrapper(String value) {
        this.value = value;
    }

    String getValue(String obscuringKey) {
        return Obfuscator.deobfuscateNameUsingKey(this.value, obscuringKey);
    }
}
