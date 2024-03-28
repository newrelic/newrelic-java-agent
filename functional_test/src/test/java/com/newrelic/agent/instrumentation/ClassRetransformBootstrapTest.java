/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.test.marker.Java11IncompatibleTest;
import com.newrelic.test.marker.Java17IncompatibleTest;
import com.newrelic.test.marker.Java21IncompatibleTest;
import com.newrelic.test.marker.Java22IncompatibleTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.activation.MimeType;
import java.util.HashMap;
import java.util.Map;

@Category({ Java11IncompatibleTest.class, Java17IncompatibleTest.class, Java21IncompatibleTest.class, Java22IncompatibleTest.class })
public class ClassRetransformBootstrapTest {

    /**
     * Instruments a core JDK method. Note that javax.activation.MimeType was added in the newrelic.yml to specifically
     * be allowed to be instrumented. Normally you are not allowed to instrument core JDK methods.
     */
    @Test
    public void testTransformBootstrapMethodOnStartup() throws Exception {
        String transMetricName = "OtherTransaction/Custom/javax.activation.MimeType/getParameter";
        String methodMetricName = "Java/javax.activation.MimeType/getParameter";

        // load class
        MimeType sample = new MimeType();
        sample.getParameter("hi");
        // verify
        Map<String, Integer> expected = new HashMap<>();
        expected.put(transMetricName, 1);
        expected.put(methodMetricName, 1);
        InstrumentTestUtils.verifyCountMetric(expected);

        // make retransformer
        InstrumentTestUtils.createTransformerAndRetransformClass("javax.activation.MimeType", "getParameter", "(Ljava/lang/String;)Z");

        // call method on class that was transformed
        sample = new MimeType();
        sample.getParameter("hi");
        sample.getParameter("hello");

        expected = new HashMap<>();
        expected.put(transMetricName, 2);
        expected.put(methodMetricName, 2);
        InstrumentTestUtils.verifyCountMetric(expected);

    }

}
