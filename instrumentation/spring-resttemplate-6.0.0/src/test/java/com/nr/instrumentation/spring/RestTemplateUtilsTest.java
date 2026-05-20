package com.nr.instrumentation.spring;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = "com.nr.instrumentation.spring")
public class RestTemplateUtilsTest {

    @ClassRule
    public static HttpServerRule  server = new HttpServerRule();

    @Test
    public void testRestTemplateCreatesTransactionWithCorrectNaming() {

        makeRestTemplateCall();

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Collection<String> txNames = introspector.getTransactionNames();

        assertEquals("Should have 2 transactions", 2, introspector.getFinishedTransactionCount()); // client and server
        assertEquals(2, txNames.size());

        String clientTxName = findClientTransactionName(txNames);

        assertNotNull("Client transaction should exist", clientTxName);
        assertTrue("Transaction name should contain method name", clientTxName.contains("makeRestTemplateCall"));

    }

    @Test
    public void testUnknownHostHandling() {
        try {
            makeUnknownHostCall();
            fail("should throw an exception for an unknown host");
        } catch (Exception e) {
            // Expected to have an exception thrown
        }

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());
    }

    @Trace(dispatcher = true)
    private void makeRestTemplateCall() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.getForObject(server.getEndPoint().toString(), String.class);
        } catch (Exception e) {
            // Ignoring this exception
        }
    }

    @Trace(dispatcher = true)
    private void makeUnknownHostCall() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getForObject("http://notarealhost/", String.class);
    }

    private String findClientTransactionName(Collection<String> txNames) {
        for (String txName : txNames) {
            if (txName.contains("RestTemplateUtilsTest")) {
                return txName;
            }
        }
        return null;
    }

}
