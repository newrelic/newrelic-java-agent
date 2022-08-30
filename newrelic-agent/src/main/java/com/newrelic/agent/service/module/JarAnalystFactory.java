/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.api.agent.Logger;

import java.io.File;
import java.net.URL;
import java.util.function.Function;

public class JarAnalystFactory {
    private final Function<URL, JarData> processor;
    private final Consumer<JarData> analyzedJars;
    private final Logger logger;

    public JarAnalystFactory(Function<URL, JarData> processor, Consumer<JarData> analyzedJars, Logger logger) {
        this.processor = processor;
        this.analyzedJars = analyzedJars;
        this.logger = logger;
    }

    public Runnable createWeavePackageAnalyzer(File file) {
        return new WeavePackageAnalyzer(file, analyzedJars, logger);
    }

    public Runnable createURLAnalyzer(URL url) {
        return new URLAnalyzer(url, processor, analyzedJars, logger);
    }
}

