/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.jmx;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.jmx.create.JmxGet;
import com.newrelic.agent.jmx.values.JavaLangJmxMetrics;
import com.newrelic.agent.service.ServiceFactory;

public class JmxServiceStartTest {

    @Test
    public void testStartConfig() {
        JmxService jmxService = ServiceFactory.getJmxService();
        List<JmxGet> configurations = jmxService.getConfigurations();
        JavaLangJmxMetrics metrics = new JavaLangJmxMetrics();
        int extCountInJmxYml = 3;
        Assert.assertEquals(configurations.toString(), configurations.size(), metrics.getFrameworkMetrics().size()
                + extCountInJmxYml);
        // verify two of the java lang object names are present
        boolean isObjectNameIndex0 = false;
        boolean isObjectNameIndex1 = false;
        for (JmxGet current : configurations) {
            if (metrics.getFrameworkMetrics().get(0).getObjectNameString().equals(current.getObjectNameString())) {
                isObjectNameIndex0 = true;
            } else if (metrics.getFrameworkMetrics().get(1).getObjectNameString().equals(current.getObjectNameString())) {
                isObjectNameIndex1 = true;
            }
        }
        Assert.assertTrue("Some of the java lang metrics are missing from the start up.", isObjectNameIndex0);
        Assert.assertTrue("Some of the java lang metrics are missing from the start up.", isObjectNameIndex1);
    }
}
