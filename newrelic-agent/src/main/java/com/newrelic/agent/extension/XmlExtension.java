/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.jmx.create.JmxConfiguration;

class XmlExtension extends Extension {

    private final com.newrelic.agent.extension.beans.Extension extension;

    public XmlExtension(ClassLoader classloader, String name, com.newrelic.agent.extension.beans.Extension ext,
            boolean custom) {
        super(classloader, name, custom);
        this.extension = ext;
    }

    @Override
    public boolean isEnabled() {
        return extension.isEnabled();
    }

    @Override
    public String getVersion() {
        return Double.toString(extension.getVersion());
    }

    @Override
    public double getVersionNumber() {
        return extension.getVersion();
    }

    @Override
    public Collection<JmxConfiguration> getJmxConfig() {
        return Collections.emptyList();
    }

    @Override
    public Collection<ExtensionClassAndMethodMatcher> getInstrumentationMatchers() {
        if (isEnabled()) {
            return ExtensionConversionUtility.convertToEnabledPointCuts(Arrays.asList(extension), isCustom(),
                    InstrumentationType.LocalCustomXml);
        }

        return Collections.emptyList();
    }

}
