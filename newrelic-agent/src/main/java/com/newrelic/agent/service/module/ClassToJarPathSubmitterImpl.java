/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.api.agent.Logger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * This class takes URLs that represent the location of a specific class. If the location is determined
 * to be a jar file, that jar is submitted to the jar collector analysis queue, only if this jar hasn't
 * been submitted before.
 */
public class ClassToJarPathSubmitterImpl implements ClassToJarPathSubmitter{
    public static final ClassToJarPathSubmitter NO_OP_INSTANCE = new NoOpClassToJarPathSubmitter();
    private final JarAnalystFactory jarAnalystFactory;
    private final ExecutorService executorService;
    private final Logger logger;
    private final Set<String> seenPaths = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final AtomicInteger classSeenCount = new AtomicInteger(0);

    public ClassToJarPathSubmitterImpl(JarAnalystFactory jarAnalystFactory, ExecutorService executorService, Logger logger) {
        this.jarAnalystFactory = jarAnalystFactory;
        this.executorService = executorService;
        this.logger = logger;
    }

    @Override
    public void processUrl(URL url) {
        if (url != null) {
            classSeenCount.incrementAndGet();
            try {
                addSingleURL(url);
            } catch (MalformedURLException exception) {
                logger.log(Level.FINEST, exception, "{0} unable to process url", url);
            }
        }
    }

    private void addSingleURL(URL url) throws MalformedURLException {
        if (JarCollectorServiceProcessor.JAR_PROTOCOL.equals(url.getProtocol())) {
            addJarProtocolURL(url);
        } else if (url.getFile().endsWith(JarCollectorServiceProcessor.JAR_EXTENSION)) {
            addURLEndingWithJar(url);
        } else {
            addOtherURL(url);
        }
    }

    private void addOtherURL(URL url) throws MalformedURLException {
        int jarIndex = url.getFile().lastIndexOf(JarCollectorServiceProcessor.JAR_EXTENSION);
        if (jarIndex > 0) {
            String path = url.getFile().substring(0, jarIndex + JarCollectorServiceProcessor.JAR_EXTENSION.length());

            if (seenPaths.add(path)) {
                URL finalUrl = new URL(url.getProtocol(), url.getHost(), path);
                submitJarUrlForAnalysis(finalUrl);
            }
        }
    }

    private void addJarProtocolURL(URL url) throws MalformedURLException {
        String path = url.getFile();
        int index = path.lastIndexOf(JarCollectorServiceProcessor.JAR_EXTENSION);
        if (index > 0) {
            path = path.substring(0, index + JarCollectorServiceProcessor.JAR_EXTENSION.length());
        }

        if (seenPaths.add(path)) {
            submitJarUrlForAnalysis(new URL(path));
        }
    }

    private void addURLEndingWithJar(URL url) {
        if (seenPaths.add(url.getFile())) {
            submitJarUrlForAnalysis(url);
        }
    }

    private void submitJarUrlForAnalysis(URL finalUrl) {
        executorService.submit(jarAnalystFactory.createURLAnalyzer(finalUrl));
        logger.log(Level.FINEST, "{0} offered to analysis queue; {1} paths seen and {2} classes seen.", finalUrl, seenPaths.size(), classSeenCount.get());
    }

    private static class NoOpClassToJarPathSubmitter implements ClassToJarPathSubmitter {
        @Override
        public void processUrl(URL url) {
            //No-op
        }
    }
}
