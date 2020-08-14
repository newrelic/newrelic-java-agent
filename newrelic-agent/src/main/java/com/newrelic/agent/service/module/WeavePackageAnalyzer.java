/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.interfaces.backport.Consumer;
import com.newrelic.api.agent.Logger;
import com.newrelic.weave.weavepackage.WeavePackageConfig;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;

public class WeavePackageAnalyzer implements Runnable {
    private final File file;
    private final Consumer<JarData> analyzedJars;
    private final Logger logger;

    public WeavePackageAnalyzer(File file, Consumer<JarData> analyzedJars, Logger logger) {
        this.file = file;
        this.analyzedJars = analyzedJars;
        this.logger = logger;
    }

    @Override
    public void run() {
        WeavePackageConfig weaveConfig;
        try {
            weaveConfig = WeavePackageConfig.builder().url(file.toURI().toURL()).build();
        } catch (Exception e) {
            logger.log(Level.FINEST, e, "{0} failed to build weave config for file");
            return;
        }
        JarData jarData = getWeaveJar(file, weaveConfig);
        logger.log(Level.FINEST, "{0} adding analyzed jar from weave package: {1}", file, jarData);
        analyzedJars.accept(jarData);
    }

    @VisibleForTesting
    JarData getWeaveJar(File file, WeavePackageConfig weavePackageConfig) {
        String sha1Checksum = "UNKNOWN";
        try {
            sha1Checksum = ShaChecksums.computeSha(file);
        } catch (Exception ex) {
            logger.log(Level.FINEST, ex, "{0} Error getting weave file checksum", file);
        }

        Map<String, String> attributes = ImmutableMap.of(
                "weaveFile", weavePackageConfig.getSource(),
                JarCollectorServiceProcessor.SHA1_CHECKSUM_KEY, sha1Checksum);

        JarInfo info = new JarInfo(Float.toString(weavePackageConfig.getVersion()), attributes);
        return new JarData(weavePackageConfig.getName(), info);
    }
}
