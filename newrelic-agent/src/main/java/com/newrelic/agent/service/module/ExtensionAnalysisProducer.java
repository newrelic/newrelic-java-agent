/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.extension.ExtensionsLoadedListener;
import com.newrelic.api.agent.Logger;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;

public class ExtensionAnalysisProducer implements ExtensionsLoadedListener {
    private final Logger logger;
    private final JarAnalystFactory jarAnalystFactory;
    private final ExecutorService executorService;

    public ExtensionAnalysisProducer(JarAnalystFactory jarAnalystFactory, ExecutorService executorService, Logger logger) {
        this.jarAnalystFactory = jarAnalystFactory;
        this.executorService = executorService;
        this.logger = logger;
    }

    @Override
    public void loaded(Set<File> extensions) {
        for (File file : extensions) {
            logger.log(Level.FINEST, "{0} offered to the analysis queue.", file);
            executorService.submit(jarAnalystFactory.createWeavePackageAnalyzer(file));
        }
    }
}
