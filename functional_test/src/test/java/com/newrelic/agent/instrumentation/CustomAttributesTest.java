/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

public class CustomAttributesTest {

    public static class ClassToAddCustomAttributes {
        public String read(String string, Long number) {
            return (string == null ? "" : string) + (number == null ? "" : number.toString());
        }
    }

    @BeforeClass
    public static void beforeClass() {
        ServiceFactory.getClassTransformerService().getClassTransformer().getClassNameFilter().addIncludeClass(
                CustomAttributesTest.class.getName().replace('.', '/') + "$ClassToAddCustomAttributes");
    }

    @Test
    public void testNullCustomAttribute() throws Exception {
        String className = ClassToAddCustomAttributes.class.getName();
        InstrumentTestUtils.createTransformerAndRetransformClass(className, "read",
                "(Ljava/lang/String;Ljava/lang/Long;)Ljava/lang/String;");

        String constructorMetric = MetricNames.SUPPORTABILITY_INIT + className + "/<init>()V";
        String methodMetric = MetricNames.SUPPORTABILITY_INIT + className
                + "/read(Ljava/lang/String;Ljava/lang/Long;)Ljava/lang/String;";
        Assert.assertEquals("test10", new CustomAttributesTest.ClassToAddCustomAttributes().read("test", 10L));
        Assert.assertEquals("test", new CustomAttributesTest.ClassToAddCustomAttributes().read("test", null));
        Assert.assertEquals("10", new CustomAttributesTest.ClassToAddCustomAttributes().read(null, 10L));
        Assert.assertEquals("", new CustomAttributesTest.ClassToAddCustomAttributes().read(null, null));

        Map<String, Integer> metricData = InstrumentTestUtils.getAndClearMetricData();
        Assert.assertNull(metricData.get(constructorMetric));
        Assert.assertEquals(Integer.valueOf(1), metricData.get(methodMetric));
    }

}
