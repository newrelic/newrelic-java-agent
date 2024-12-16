/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.language;

import com.google.common.annotations.VisibleForTesting;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;

import java.lang.reflect.Field;
import java.util.logging.Level;

import static com.newrelic.agent.stats.StatsWorks.getRecordMetricWork;
import static java.text.MessageFormat.format;

/**
 * A task designed to be run once to determine what source language libraries other than Java are available.
 */
public class SourceLibraryDetector implements Runnable {
    private static final String SCALA_VERSION_CLASS = "scala.util.Properties";
    private static final String SCALA_VERSION_METHOD = "versionNumberString";

    private static final String KOTLIN_VERSION_CLASS = "kotlin.KotlinVersion";
    private static final String KOTLIN_VERSION_FIELD = "CURRENT";

    private static final String CLOJURE_VERSION_CLASS = "clojure.core$clojure_version";
    private static final String CLOJURE_VERSION_METHOD = "invoke";

    private boolean done = false;

    @Override
    public void run() {
        if (done) {
            return;
        }

        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINER, "Obtaining source library language supportability metrics");
        }
        try {
            detectSourceLanguages();
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINER, e, "Unexpected error attempting to find source languages");
        } finally {
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }

    @VisibleForTesting
    void detectSourceLanguages() {
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        detectJava();
        detectScala3(systemClassLoader);
        detectScala(systemClassLoader);
        detectKotlin(systemClassLoader);
        detectClojure(systemClassLoader);
    }

    private void detectJava() {
        // this will report Java 8 as "1.8", Java 9.0.x as "9", Java 10.0.x as "10", and Java 11.0.x as "11"
        String javaVersion = System.getProperty("java.specification.version");
        recordSourceLanguageMetric("java", javaVersion);

        // this property is identical for AdoptOpenJDK and OpenJDK
        // "J9" can be found in IBM or Eclipse so don't check for it
        String jvmVendor = System.getProperty("java.vm.name").toLowerCase();
        if (jvmVendor.contains("hotspot")) {
            recordJvmVendorMetric("Oracle");
        } else if (jvmVendor.contains("ibm")) {
            recordJvmVendorMetric("IBM");
        } else if (jvmVendor.contains("azul") || jvmVendor.contains("zing") || jvmVendor.contains("zulu")) {
            recordJvmVendorMetric("Azul");
        } else if (jvmVendor.contains("eclipse")) {
            recordJvmVendorMetric("Eclipse");
        } else if (jvmVendor.contains("openjdk")) {
            recordJvmVendorMetric("OpenJDK");
        } else if (jvmVendor.contains("corretto")) {
            recordJvmVendorMetric("Corretto");
        } else {
            recordJvmVendorMetric("Other");
        }
    }

    private void detectScala3(ClassLoader systemClassLoader) {
        try {
            //scala3-library removed all methods referencing the version number, so we just capture the major version
            //by checking for a class introduced in the scala 3 API.
            Class<?> aClass = Class.forName("scala.deriving.Mirror", true, systemClassLoader);
            if (aClass != null) {
                recordSourceLanguageMetric("scala", "3.x");
            }
        } catch (ClassNotFoundException cnfe){
            Agent.LOG.log(Level.FINEST, format("Class ''{0}'' was not found; scala3 is not present in the classpath", "scala.deriving.Mirror"));
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, format("Exception occurred while trying to determine scala3 version", e.getMessage()));
        }
    }

    private void detectScala(ClassLoader systemClassLoader) {
        try {
            // Scala has a static versionNumberString method that can be invoked to get the version number
            Class<?> aClass = Class.forName(SCALA_VERSION_CLASS, true, systemClassLoader);
            if (aClass != null) {
                String version = (String) aClass.getMethod(SCALA_VERSION_METHOD).invoke(null);
                recordSourceLanguageMetric("scala", version);
            }
        } catch (ClassNotFoundException cfne) {
            Agent.LOG.log(Level.FINEST, format("Class ''{0}'' was not found; scala is not present in the classpath", SCALA_VERSION_CLASS));
        } catch (NoSuchMethodException nfme) {
            Agent.LOG.log(Level.FINEST,
                    format("Method ''{0}'' in class ''{1}'' was not found; cannot determine scala version", SCALA_VERSION_METHOD, SCALA_VERSION_CLASS));
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, format("Exception occurred while trying to determine scala version", e.getMessage()));
        }
    }

    private void detectKotlin(ClassLoader systemClassLoader) {
        try {
            Class<?> aClass = Class.forName(KOTLIN_VERSION_CLASS, true, systemClassLoader);
            if (aClass != null) {
                Field field = aClass.getField(KOTLIN_VERSION_FIELD);
                String version = field.get(null).toString();
                recordSourceLanguageMetric("kotlin", version);
            }
        } catch (ClassNotFoundException cfne) {
            Agent.LOG.log(Level.FINEST, format("Class ''{0}'' was not found; kotlin is not present in the classpath", KOTLIN_VERSION_CLASS));
        } catch (NoSuchFieldException nfme) {
            Agent.LOG.log(Level.FINEST,
                    format("Field ''{0}'' in class ''{1}'' was not found; cannot determine kotlin version", KOTLIN_VERSION_FIELD, KOTLIN_VERSION_CLASS));
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, format("Exception occurred while trying to determine kotlin version", e.getMessage()));
        }
    }

    private void detectClojure(ClassLoader systemClassLoader) {
        try {
            // Clojure has a version object that can be instantiated and invoked to get the version number
            Class<?> aClass = Class.forName(CLOJURE_VERSION_CLASS, true, systemClassLoader);
            if (aClass != null) {
                Object clojureVersion = aClass.newInstance();
                String version = aClass.getMethod(CLOJURE_VERSION_METHOD).invoke(clojureVersion).toString();
                recordSourceLanguageMetric("clojure", version);
            }
        } catch (ClassNotFoundException cfne) {
            Agent.LOG.log(Level.FINEST, format("Class ''{0}'' was not found; clojure is not present in the classpath", CLOJURE_VERSION_CLASS));
        } catch (NoSuchMethodException nfme) {
            Agent.LOG.log(Level.FINEST,
                    format("Method ''{0}'' in class ''{1}'' was not found; cannot determine clojure version", CLOJURE_VERSION_METHOD, CLOJURE_VERSION_CLASS));
        } catch (Exception e) {
            Agent.LOG.log(Level.FINEST, format("Exception occurred while trying to determine clojure version", e.getMessage()));
        }
    }

    private void recordSourceLanguageMetric(String language, String version) {
        ServiceFactory
                .getStatsService()
                .doStatsWork(getRecordMetricWork(format(MetricNames.SUPPORTABILITY_SOURCE_LANGUAGE_VERSION, language, version), 1),
                        MetricNames.SUPPORTABILITY_SOURCE_LANGUAGE_VERSION + " language: " + language );
    }

    private void recordJvmVendorMetric(String vendor) {
        ServiceFactory
                .getStatsService()
                .doStatsWork(getRecordMetricWork(format(MetricNames.SUPPORTABILITY_JVM_VENDOR, vendor), 1),
                        MetricNames.SUPPORTABILITY_JVM_VENDOR + " vendor: " + vendor  );
    }
}
