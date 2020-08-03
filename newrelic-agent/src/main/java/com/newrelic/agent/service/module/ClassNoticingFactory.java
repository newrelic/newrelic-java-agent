/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.service.module;

import com.newrelic.agent.instrumentation.context.ClassMatchVisitorFactory;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.api.agent.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class ClassNoticingFactory implements ClassMatchVisitorFactory {
    private final JarAnalystFactory jarAnalystFactory;
    private final ExecutorService executorService;
    private final Logger logger;
    private final Set<String> seenPaths = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final AtomicInteger classSeenCount = new AtomicInteger(0);

    public ClassNoticingFactory(JarAnalystFactory jarAnalystFactory, ExecutorService executorService, Logger logger) {
        this.jarAnalystFactory = jarAnalystFactory;
        this.executorService = executorService;
        this.logger = logger;
    }

    @Override
    public ClassVisitor newClassMatchVisitor(ClassLoader loader, Class<?> classBeingRedefined,
            ClassReader reader, ClassVisitor cv, InstrumentationContext context) {
        if (null != context.getProtectionDomain()
                && null != context.getProtectionDomain().getCodeSource()
                && null != context.getProtectionDomain().getCodeSource().getLocation()) {
            addURL(context.getProtectionDomain().getCodeSource().getLocation());
        }
        return null;
    }

    /**
     * Adds a url which represents a jar or a directory.
     */
    public void addURL(URL url) {
        classSeenCount.incrementAndGet();
        try {
            addSingleURL(url);
        } catch (MalformedURLException exception) {
            logger.log(Level.FINEST, exception, "{0} unable to process url", url);
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
                executorService.submit(jarAnalystFactory.createURLAnalyzer(finalUrl));
                logger.log(Level.FINEST, "{0} offered to analysis queue; {1} paths seen and {2} classes seen.", finalUrl, seenPaths.size(),
                        classSeenCount.get());
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
            executorService.submit(jarAnalystFactory.createURLAnalyzer(new URL(path)));
            logger.log(Level.FINEST, "{0} offered to analysis queue; {1} paths seen and {2} classes seen.", url, seenPaths.size(), classSeenCount.get());
        }
    }

    private void addURLEndingWithJar(URL url) {
        if (seenPaths.add(url.getFile())) {
            executorService.submit(jarAnalystFactory.createURLAnalyzer(url));
            logger.log(Level.FINEST, "{0} offered to analysis queue; {1} paths seen and {2} classes seen.", url, seenPaths.size(), classSeenCount.get());
        }
    }
}
