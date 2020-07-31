/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.service.module;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.newrelic.agent.MockConfigService;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.JarCollectorConfig;
import com.newrelic.agent.service.ServiceFactory;

public class JarCollectorServiceImplTest {

    private static JarCollectorServiceImpl createService() {
        AgentConfig config = Mockito.mock(AgentConfig.class);
        JarCollectorConfig jarConfig = Mockito.mock(JarCollectorConfig.class);
        Mockito.when(jarConfig.isEnabled()).thenReturn(true);
        Mockito.when(config.getJarCollectorConfig()).thenReturn(jarConfig);
        Mockito.when(config.getValue("jar_collector.skip_temp_jars", true)).thenReturn(true);

        ServiceFactory.setServiceManager(new MockServiceManager().setConfigService(Mockito.spy(new MockConfigService(
                config))));

        return new JarCollectorServiceImpl();
    }

    @Test
    public void test() throws MalformedURLException {

        JarCollectorServiceImpl service = createService();

        service.addUrls(new URL(
                "file:/Users/roger/Documents/integration_tests_webapps/resin-3.1.12/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.0.1.jar!/org/apache/commons/httpclient/HttpVersion.class"));
        service.addUrls(new URL(
                "file:/Users/roger/Documents/integration_tests_webapps/resin-3.1.12/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.0.1.jar!/org/apache/commons/httpclient/Dude.class"));

        Assert.assertEquals(
                new URL(
                        "file:/Users/roger/Documents/integration_tests_webapps/resin-3.1.12/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.0.1.jar"),
                service.getQueuedJars().get(
                        "/Users/roger/Documents/integration_tests_webapps/resin-3.1.12/webapps/java_test_webapp/WEB-INF/lib/commons-httpclient-3.0.1.jar"));

        Assert.assertEquals(service.getQueuedJars().toString(), 1, service.getQueuedJars().size());
    }

    @Test
    public void jarProtocol() throws MalformedURLException {

        JarCollectorServiceImpl service = createService();

        // jboss sends us complex urls like this
        service.addUrls(new URL(
                "jar:file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/"));

        Assert.assertEquals(
                service.getQueuedJars().toString(),
                new URL(
                        "file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar"),
                service.getQueuedJars().get(
                        "file:/Users/sdaubin/servers/jboss-as-7.1.1.Final/modules/org/apache/xerces/main/xercesImpl-2.9.1-jbossas-1.jar!/"));
    }
}
