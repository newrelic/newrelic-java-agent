/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.browser;

import com.google.common.collect.Sets;
import com.newrelic.agent.Transaction;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class JavaScriptAgentConfigTest {
    @Test
    public void getConfigString_returnsValidJsonString() throws Exception {
        BrowserTransactionStateTest.createServiceManager();
        JavaScriptAgentConfig javaScriptAgentConfig = new JavaScriptAgentConfig("appName", "beacon", "browserKey",
                "errorBeacon", "payloadScript", "appId");

        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();

        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        String json = javaScriptAgentConfig.getConfigString(bts);

        Assert.assertEquals("{\"errorBeacon\":\"errorBeacon\",\"licenseKey\":\"browserKey\",\"agent\":\"payloadScript\"," +
                "\"beacon\":\"beacon\",\"applicationTime\":0,\"applicationID\":\"appId\",\"transactionName\":\"\",\"queueTime\":0}", json);
    }

    @Test
    public void testUserAttributes() throws Exception {
        BrowserTransactionStateTest.createServiceManager();
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Map<String, Object> atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        tx.getUserAttributes().put("one", 1L);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        Map<String, Object> user = (Map<String, Object>) atts.get("u");
        Assert.assertNotNull(user);
        Assert.assertEquals(1, user.size());
        Assert.assertEquals(1L, user.get("one"));

        tx.getUserAttributes().put("two", 5.44);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        user = (Map<String, Object>) atts.get("u");
        Assert.assertEquals(1, atts.size());
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.size());
        Assert.assertEquals(5.44, user.get("two"));

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        tx.getUserAttributes().put("one", "abc123");
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        user = (Map<String, Object>) atts.get("u");
        Assert.assertNotNull(user);
        Assert.assertEquals(1, user.size());
        Assert.assertEquals("abc123", user.get("one"));

        tx.getUserAttributes().put("two", 989);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        user = (Map<String, Object>) atts.get("u");
        Assert.assertNotNull(user);
        Assert.assertEquals(2, user.size());
        Assert.assertEquals(989, user.get("two"));

        tx.getUserAttributes().put("three", "hello");
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        user = (Map<String, Object>) atts.get("u");
        Assert.assertNotNull(user);
        Assert.assertEquals(3, user.size());
        Assert.assertEquals("hello", user.get("three"));

        Transaction.clearTransaction();
    }

    @Test
    public void testAgentAttributes() throws Exception {
        BrowserTransactionStateTest.createServiceManager();
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Map<String, Object> atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        tx.getAgentAttributes().put("one", 1L);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        Map<String, Object> agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(1, agentAtts.size());
        Assert.assertEquals(1L, agentAtts.get("one"));

        tx.getAgentAttributes().put("two", 5.44);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(2, agentAtts.size());
        Assert.assertEquals(5.44, agentAtts.get("two"));

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        tx.getAgentAttributes().put("one", "abc123");
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(1, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("one"));

        tx.getAgentAttributes().put("two", 989);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(2, agentAtts.size());
        Assert.assertEquals(989, agentAtts.get("two"));

        tx.getAgentAttributes().put("three", "hello");
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(3, agentAtts.size());
        Assert.assertEquals("hello", agentAtts.get("three"));

        Transaction.clearTransaction();
        tx = Transaction.getTransaction();
        bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(0, bts.getAgentAttributes().size());

        Map<String, String> requests = new HashMap<>();
        requests.put("one", "abc123");
        requests.put("two", "333");
        tx.getPrefixedAgentAttributes().put("request.parameters.", requests);
        tx.getAgentAttributes().put("three", 44);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(1, agentAtts.size());
        // no request attributes because they are off be default
        Assert.assertEquals(44, agentAtts.get("three"));
        Assert.assertEquals(0, bts.getUserAttributes().size());

        Transaction.clearTransaction();
    }

    @Test
    public void testRequestAgentAttributesOn() throws Exception {
        BrowserTransactionStateTest.createServiceManager(Sets.newHashSet("request.parameters.*"),
                Collections.<String> emptySet());
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Map<String, Object> atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        Map<String, String> requests = new HashMap<>();
        requests.put("one", "abc123");
        requests.put("two", "333");
        tx.getPrefixedAgentAttributes().put("request.parameters.", requests);
        tx.getAgentAttributes().put("three", 44);
        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(1, atts.size());
        Map<String, Object> agentAtts = (Map<String, Object>) atts.get("a");
        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(3, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("request.parameters.one"));
        Assert.assertEquals("333", agentAtts.get("request.parameters.two"));
        Assert.assertEquals(44, agentAtts.get("three"));
        Assert.assertEquals(0, bts.getUserAttributes().size());

        Transaction.clearTransaction();
    }

    @Test
    public void testGetAttributes() throws Exception {
        BrowserTransactionStateTest.createServiceManager(Sets.newHashSet("request.parameters.*"), Sets.newHashSet(
                "request.parameters.bacon", "user.three"));
        Transaction tx = Transaction.getTransaction();
        Transaction.clearTransaction();
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Map<String, Object> atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(0, atts.size());

        // user
        tx.getUserAttributes().put("one", 1L);
        tx.getUserAttributes().put("two", 2.22);
        tx.getUserAttributes().put("user.three", "three");
        // agent
        Map<String, String> requests = new HashMap<>();
        requests.put("one", "abc123");
        requests.put("two", "ringing");
        requests.put("bacon", "bacon");

        tx.getPrefixedAgentAttributes().put("request.parameters.", requests);
        tx.getAgentAttributes().put("one", 44);
        tx.getAgentAttributes().put("two", 44.44);

        atts = JavaScriptAgentConfig.getAttributes(bts);
        Assert.assertEquals(2, atts.size());
        Map<String, Object> userAtts = (Map<String, Object>) atts.get("u");
        Map<String, Object> agentAtts = (Map<String, Object>) atts.get("a");

        Assert.assertNotNull(userAtts);
        Assert.assertEquals(2, userAtts.size());
        Assert.assertEquals(1L, userAtts.get("one"));
        Assert.assertEquals(2.22, (Double) userAtts.get("two"), .001);

        Assert.assertNotNull(agentAtts);
        Assert.assertEquals(4, agentAtts.size());
        Assert.assertEquals("abc123", agentAtts.get("request.parameters.one"));
        Assert.assertEquals("ringing", agentAtts.get("request.parameters.two"));
        Assert.assertEquals(44, agentAtts.get("one"));
        Assert.assertEquals(44.44, (Double) agentAtts.get("two"), .001);

        Transaction.clearTransaction();
    }
}
