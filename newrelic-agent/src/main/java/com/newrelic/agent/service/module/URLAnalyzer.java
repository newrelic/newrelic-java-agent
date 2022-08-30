/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.api.agent.Logger;

import java.net.URL;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class URLAnalyzer implements Runnable {
    private final URL url;
    private final Function<URL, JarData> processor;
    private final Consumer<JarData> analyzedJars;
    private final Logger logger;

    public URLAnalyzer(URL url, Function<URL, JarData> processor, Consumer<JarData> analyzedJars, Logger logger) {
        this.url = url;
        this.processor = processor;
        this.analyzedJars = analyzedJars;
        this.logger = logger;
    }

    @Override
    public void run() {
        JarData jarData = processor.apply(url);
        if (jarData != null) {
            logger.log(Level.FINEST, "{0} adding analyzed jar: {1}", url, jarData);
            analyzedJars.accept(jarData);
        }
    }
}
