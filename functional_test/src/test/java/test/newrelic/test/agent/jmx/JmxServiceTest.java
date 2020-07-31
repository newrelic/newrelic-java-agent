/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.jmx;

import com.newrelic.agent.extension.Extension;
import com.newrelic.agent.jmx.JmxService;
import com.newrelic.agent.jmx.create.JmxGet;
import com.newrelic.agent.jmx.values.GlassfishJmxValues;
import com.newrelic.agent.jmx.values.JettyJmxMetrics;
import com.newrelic.agent.jmx.values.ResinJmxValues;
import com.newrelic.agent.jmx.values.TomcatJmxValues;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JmxServiceTest {

    @Test
    public void testEachExtensionAddedOnlyOnce() {
        JmxService jmxService = ServiceFactory.getJmxService();
        List<JmxGet> configs = jmxService.getConfigurations();
        Assert.assertTrue(configs.size() > 0);

        // this checks to see if we have already seen the object name
        // really we should check attributes too, but this should be good enough
        // I just want to make sure the same extension is not loaded more than once
        Set<String> alreadySeen = new HashSet<>();
        for (JmxGet current : configs) {
            Assert.assertTrue("It looks like an extension file was loaded more than once.",
                    !alreadySeen.contains(current.getObjectNameString()));
            alreadySeen.add(current.getObjectNameString());
        }
    }

    @Test
    public void testAddingToConfig() {
        JmxService jmxService = ServiceFactory.getJmxService();
        List<JmxGet> configurations = jmxService.getConfigurations();
        int startupsize = configurations.size();
        TomcatJmxValues tomcat = new TomcatJmxValues();
        ResinJmxValues resin = new ResinJmxValues();
        jmxService.addJmxFrameworkValues(tomcat);
        jmxService.addJmxFrameworkValues(resin);

        String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        StatsEngine statsEngine = new StatsEngineImpl();
        jmxService.beforeHarvest(appName, statsEngine);

        int expectedSize = startupsize + tomcat.getFrameworkMetrics().size() + resin.getFrameworkMetrics().size();
        Assert.assertEquals("Failed on initial check: " + configurations.toString(), expectedSize,
                configurations.size());

        GlassfishJmxValues glassfish = new GlassfishJmxValues();
        jmxService.addJmxFrameworkValues(glassfish);
        jmxService.beforeHarvest(appName, statsEngine);

        int newExpectedSize = expectedSize + glassfish.getFrameworkMetrics().size();
        Assert.assertEquals("Failed on second check: " + configurations.toString(), newExpectedSize,
                configurations.size());

        JettyJmxMetrics jetty = new JettyJmxMetrics();
        jmxService.addJmxFrameworkValues(jetty);
        jmxService.beforeHarvest(appName, statsEngine);

        int thirdExpectedSize = newExpectedSize + jetty.getFrameworkMetrics().size();
        Assert.assertEquals("Failed on third check: " + configurations.toString(), thirdExpectedSize,
                configurations.size());

    }

    @Test
    public void process() {
        String appName = ServiceFactory.getConfigService().getDefaultAgentConfig().getApplicationName();
        StatsEngine statsEngine = new StatsEngineImpl();
        JmxService jmxService = ServiceFactory.getJmxService();
        jmxService.beforeHarvest(appName, statsEngine);
    }

    @Test
    public void checkSolr() {
        // bogus test to make sure our solr config is ok - this is only going to test the instrumentation.
        Extension solrExtension = ServiceFactory.getExtensionService().getInternalExtensions().get("Solr");
        Assert.assertNotNull(ServiceFactory.getExtensionService().getInternalExtensions().keySet().toString(),
                solrExtension);
    }

}
