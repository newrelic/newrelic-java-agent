/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.complexenv;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import org.junit.Test;

import com.newrelic.agent.instrumentation.InstrumentTestUtils;

public class ClassRetransformClassloaderTest {

    class ClassLoaderOtherObject extends URLClassLoader {

        public ClassLoaderOtherObject(URL[] urls) {
            super(urls);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return super.loadClass(name);
        }
    }

    @Test
    public void testGeneralClassLoader() throws Exception {
        String transMetric = InstrumentTestUtils.TRANS_PREFIX + ClassLoaderOtherObject.class.getName() + "/<init>";
        String methodMetric = InstrumentTestUtils.METHOD_PREFIX + ClassLoaderOtherObject.class.getName() + "/<init>";
        String transdMetricResource = InstrumentTestUtils.TRANS_PREFIX + ClassLoaderOtherObject.class.getName()
                + "/loadClass";
        String methodMetricResource = InstrumentTestUtils.METHOD_PREFIX + ClassLoaderOtherObject.class.getName()
                + "/loadClass";

        performClassLoaderWork("java.util.HashMap");
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetric, methodMetric));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassLoaderOtherObject.class.getName(), "<init>",
                "(Lcom/newrelic/agent/instrumentation/complexenv/ClassRetransformClassloaderTest;[Ljava/net/URL;)V");

        performClassLoaderWork("java.util.HashMap");
        InstrumentTestUtils.verifySingleMetrics(transMetric, methodMetric);

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassLoaderOtherObject.class.getName(), "loadClass",
                "(Ljava/lang/String;)Ljava/lang/Class;");

        performClassLoaderWork("java.util.HashMap");
        InstrumentTestUtils.verifySingleMetrics(transMetric, methodMetric, transdMetricResource, methodMetricResource);

    }

    private void performClassLoaderWork(String className) throws MalformedURLException, ClassNotFoundException {
        URL url = new URL("http", "localhost", "/tmp/");
        ClassLoaderOtherObject other = new ClassLoaderOtherObject(new URL[] { url });
        other.findResource(className);
        other.loadClass(className);
    }

    class ClassLoaderExceptionOtherObject extends URLClassLoader {

        public ClassLoaderExceptionOtherObject(URL[] urls) {
            super(urls);
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("The class could not be found.");
        }
    }

    @Test
    public void testClassLoaderNoSeeNewRelic() throws Exception {
        String transMetric = InstrumentTestUtils.TRANS_PREFIX + ClassLoaderExceptionOtherObject.class.getName()
                + "/<init>";
        String methodMetric = InstrumentTestUtils.METHOD_PREFIX + ClassLoaderExceptionOtherObject.class.getName()
                + "/<init>";
        String transdMetricResource = InstrumentTestUtils.TRANS_PREFIX
                + ClassLoaderExceptionOtherObject.class.getName() + "/loadClass";
        String methodMetricResource = InstrumentTestUtils.METHOD_PREFIX
                + ClassLoaderExceptionOtherObject.class.getName() + "/loadClass";

        performClassLoaderExceptionWork("java.util.HashMap");
        InstrumentTestUtils.verifyMetricNotPresent(Arrays.asList(transMetric, methodMetric));

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassLoaderExceptionOtherObject.class.getName(),
                "<init>",
                "(Lcom/newrelic/agent/instrumentation/complexenv/ClassRetransformClassloaderTest;[Ljava/net/URL;)V");

        performClassLoaderExceptionWork("java.util.HashMap");
        InstrumentTestUtils.verifySingleMetrics(transMetric, methodMetric);

        InstrumentTestUtils.createTransformerAndRetransformClass(ClassLoaderExceptionOtherObject.class.getName(),
                "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

        performClassLoaderExceptionWork("java.util.HashMap");
        InstrumentTestUtils.verifySingleMetrics(transMetric, methodMetric, transdMetricResource, methodMetricResource);

    }

    private void performClassLoaderExceptionWork(String className) throws MalformedURLException, ClassNotFoundException {
        URL url = new URL("http", "localhost", "/tmp/");
        ClassLoaderExceptionOtherObject other = new ClassLoaderExceptionOtherObject(new URL[] { url });
        try {
            other.findResource(className);
            other.loadClass(className);
        } catch (ClassNotFoundException e) {
            // this is generally thrown
        }
    }

}
