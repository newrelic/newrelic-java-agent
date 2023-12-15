/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionService;
import com.newrelic.agent.attributes.AttributesService;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.trace.TransactionTraceService;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.servlet.BasicRequestRootTracer;
import com.newrelic.agent.tracers.servlet.MockHttpRequest;
import com.newrelic.agent.tracers.servlet.MockHttpResponse;
import com.newrelic.agent.transaction.PriorityTransactionName;
import com.newrelic.agent.util.Obfuscator;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* (non-javadoc)
 * Note: the "beacon" was a predecessor technology for correlated transaction traces with the browser.
 * Some appearances of the term could be changed to "browser" now.
 */
public class BrowserConfigTest {

    private static final String LOADER = "window.NREUM||(NREUM={}),__nr_require=function a(b,c,d){function e(f){if(!c[f]){var g=c[f]={exports:{}};b[f][0].call(g.exports,function(a){var c=b[f][1][a];return e(c?c:a)},g,g.exports,a,b,c,d)}return c[f].exports}for(var f=0;f<d.length;f++)e(d[f]);return e}({\"4O2Y62\":[function(a,b){function c(a,b){var c=d[a];return c?c.apply(this,b):(e[a]||(e[a]=[]),e[a].push(b),void 0)}var d={},e={};b.exports=c,c.queues=e,c.handlers=d},{}],handle:[function(a,b){b.exports=a(\"4O2Y62\")},{}],\"SvQ0B+\":[function(a,b){function c(a){if(a===window)return 0;if(e.call(a,\"__nr\"))return a.__nr;try{return Object.defineProperty(a,\"__nr\",{value:d,writable:!1,configurable:!1,enumerable:!1}),d}catch(b){return a.__nr=d,d}finally{d+=1}}var d=1,e=Object.prototype.hasOwnProperty;b.exports=c},{}],id:[function(a,b){b.exports=a(\"SvQ0B+\")},{}],YLUGVp:[function(a,b){function c(){var a=m.info=NREUM.info,b=m.proto=\"http\"+(a.sslForHttp?\"s\":\"s\"==l[4]?\"s\":\"\")+\"://\";if(a&&a.agent&&a.licenseKey&&a.applicationID){f(\"mark\",[\"onload\",e()]);var c=h.createElement(\"script\");c.src=b+a.agent,h.body.appendChild(c)}}function d(){\"complete\"===h.readyState&&f(\"mark\",[\"domContent\",e()])}function e(){return(new Date).getTime()}var f=a(\"handle\"),g=window,h=g.document,i=\"readystatechange\",j=\"addEventListener\",k=\"attachEvent\",l=(\"\"+location).split(\"?\")[0],m=b.exports={offset:e(),origin:l};h[j]?(h[j](i,d,!1),g[j](\"load\",c,!1)):(h[k](\"on\"+i,d),g[k](\"onload\",c)),f(\"mark\",[\"firstbyte\",e()])},{handle:\"4O2Y62\"}],loader:[function(a,b){b.exports=a(\"YLUGVp\")},{}]},{},[\"YLUGVp\"]);";
    private static final String HEADER = "\n<script type=\"text/javascript\">" + LOADER + "</script>";
    private static final String APP_NAME = "daApp";
    private static final String LICENSE_KEY = "asdfghjkloiuytrewq1234567890zxcvbnm";
    private static final String SCRIPT_START = "\n<script type=\"text/javascript\"";
    private static final String JAVASCRIPT_AGENT_CONFIG_PREFIX = "window.NREUM||(NREUM={});NREUM.info=";
    private static final String END_SCRIPT = "</script>";

    private static final String[] USER_ATTRIBUTES = { "\"theInt\":11", "\"theDouble\":11.22", "\"theLong\":22",
            "\"theString\":\"abc123\"", "\"theShort\":1" };

    @Test
    public void getBrowserAgentScript_withNonce_returnsValidScript() throws Exception {
        setupServiceManager(true, true);
        Transaction tx = createTransaction();

        Map<String, Object> beaconSettings = createBrowserConfig(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);

        String value = beaconConfig.getBrowserAgentScript(bts, "ABC123");

        Assert.assertTrue(value.contains("nonce=\"ABC123\""));
        Assert.assertTrue(value.startsWith(SCRIPT_START));
        Assert.assertTrue(value.endsWith(END_SCRIPT));
        Assert.assertTrue(value.contains(JAVASCRIPT_AGENT_CONFIG_PREFIX));

        checkStringsAndUserParams(value, Arrays.asList(USER_ATTRIBUTES), null);
    }

    @Test
    public void getBrowserAgentScript_withoutNonce_returnsValidScript() throws Exception {
        setupServiceManager(true, true);
        Transaction tx = createTransaction();

        Map<String, Object> beaconSettings = createBrowserConfig(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);

        String value = beaconConfig.getBrowserAgentScript(bts);

        Assert.assertFalse(value.contains("nonce=\""));
        Assert.assertTrue(value.startsWith(SCRIPT_START));
        Assert.assertTrue(value.endsWith(END_SCRIPT));
        Assert.assertTrue(value.contains(JAVASCRIPT_AGENT_CONFIG_PREFIX));

        checkStringsAndUserParams(value, Arrays.asList(USER_ATTRIBUTES), null);
    }

    @Test(expected = Exception.class)
    public void getBrowserAgentScript_withRumDisabled_throwsException() throws Exception {
        BrowserConfig.createBrowserConfig("appName", createBrowserConfig(false));
    }

    private Transaction createTransaction() {
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        tx.getUserAttributes().put("theInt", 11);
        tx.getUserAttributes().put("theDouble", 11.22);
        tx.getUserAttributes().put("theLong", 22L);
        tx.getUserAttributes().put("theString", "abc123");
        tx.getUserAttributes().put("theShort", Short.parseShort("1"));

        return tx;
    }

    // Check that value contains the JSON specified in the found in the array of expected strings,
    // plus the RUM footer start and end strings, and nothing else.
    public static void checkStrings(String toCheck, List<String> expectedProperties, List<String> matched) {

        for (String toMatch : expectedProperties) {
            Pattern p = Pattern.compile(toMatch);
            Matcher m = p.matcher(toCheck);
            Assert.assertTrue("Not found: " + toMatch, m.find());

            String s = m.group(0).trim();
            Assert.assertTrue(s.length() > 0);
            matched.add(s);
        }

        // Check for unexpected content in the returned string by knocking all the matches out
        // and looking just for the correct separators. There should be one fewer commas in the
        // expected delimiters than the number of strings in the expected array.

        for (String s : matched) {
            toCheck = toCheck.replace(s, "");
        }

        // We want to make sure what's left is just the two curly braces and the commas that separated
        // the elements in the javascript. But we don't want to make a pattern that always makes all
        // commas optional. So we choose a distinct pattern when the number of expected footer properties
        // was one (so there were no commas).
        Pattern expect = Pattern.compile(expectedProperties.size() == 1 ? "\\{\\}" : "\\{[,]+\\}");
        Assert.assertTrue("Unexpected content: " + toCheck, expect.matcher(toCheck).matches());
    }

    // Validate the userParams in the value, then call checkStrings on the *remaining* values.
    private static void checkStringsAndUserParams(String toCheck, List<String> expectedUserAttributes,
            List<String> expectedAgentAttributes) {

        // Validate the attributes by grabbing the attribute, deobfuscating the string, and
        // then checking for expected values in the JSON.
        //
        // Initially, here, we are looking for something like this:
        // \"userAttributes\":\"GlEUFAgMHwgYTVNXHQAjFgkDHQkfThI=\"
        //
        // Note the leading comma in the pattern. We need to match on it and then
        // replace it away before calling checkStrings() or the assert on the last
        // line of checkStrings() will fail.
        Pattern p = Pattern.compile(",\"atts\":\"[A-Za-z0-9+/=]+\"");

        Matcher m = p.matcher(toCheck);
        Assert.assertTrue(m.find());
        String attributes = m.group(0).trim();

        attributes = attributes.replace("\"atts\":\"", "");
        attributes = attributes.replace("\"", "");
        attributes = attributes.replace(",", "");
        String deobfuscatedAttributes = Obfuscator.deobfuscateNameUsingKey(attributes, LICENSE_KEY.substring(0, 13));

        // Now we need to check each key:value pair in the "u" and "a" attribute collections separately.
        // Here is one example of what deobfuscatedAttributes might look like at this point:
        // {"u":{"theLong":22,"theInt":11,"theShort":1,"theDouble":11.22,"theString":"abc123"},"a":{"key_1":valX,"key_2",valY}}
        // Either or both of the "u" and "a" attributes may be absent, but there should be *something*, at least in
        // these tests.
        Assert.assertTrue(deobfuscatedAttributes != null && deobfuscatedAttributes.length() > 0);
        deobfuscatedAttributes.replace(" ", "");

        if (expectedUserAttributes != null) {
            // Get the "u" list if present
            Pattern getUList = Pattern.compile("\"u\":\\{.+?\\}");
            Matcher matchUList = getUList.matcher(deobfuscatedAttributes);
            Assert.assertTrue(matchUList.find());
            String uList = matchUList.group(0).trim();
            uList = uList.replace("\"u\":", "");
            // We don't need to accumulate matches, so the third arguments is throwaway.
            checkStrings(uList, expectedUserAttributes, new ArrayList<String>());
        }

        if (expectedAgentAttributes != null) {
            // Get the "a" list if present
            Pattern getAList = Pattern.compile("\"a\":\\{.+?\\}");
            Matcher matchAList = getAList.matcher(deobfuscatedAttributes);
            Assert.assertTrue(matchAList.find());
            String aList = matchAList.group(0).trim();
            aList = aList.replace("\"a\":", "");
            // We don't need to accumulate matches, so the third arguments is throwaway.
            checkStrings(aList, expectedAgentAttributes, new ArrayList<String>());
        }
    }

    private void setupServiceManager(boolean captureParams, boolean setSslForHttpToTrue) {
        MockServiceManager manager = new MockServiceManager();
        ServiceFactory.setServiceManager(manager);

        Map<String, Object> params = new HashMap<>();
        if (captureParams || setSslForHttpToTrue) {
            Map<String, Object> bProps = new HashMap<>();
            params.put("browser_monitoring", bProps);
            if (captureParams) {
                Map<String, Object> baProps = new HashMap<>();
                bProps.put("attributes", baProps);
                baProps.put("enabled", Boolean.TRUE);
            }
            if (setSslForHttpToTrue) {
                bProps.put("ssl_for_http", Boolean.TRUE);
            }
        }
        params.put("license_key", LICENSE_KEY);

        ImmutableMap<String, Object> distributedTracingSettings = ImmutableMap.<String, Object>builder()
                .put(DistributedTracingConfig.ENABLED, Boolean.FALSE)
                .build();

        params.put(AgentConfigImpl.DISTRIBUTED_TRACING, distributedTracingSettings);

        manager.setConfigService(ConfigServiceFactory.createConfigServiceUsingSettings(params));
        manager.setTransactionService(new TransactionService());
        manager.setTransactionTraceService(new TransactionTraceService());

        MockRPMServiceManager rpmServiceMgr = new MockRPMServiceManager();

        MockRPMService rpmService = new MockRPMService();
        rpmService.setApplicationName(APP_NAME);
        rpmService.setEverConnected(true);
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmServiceMgr.setRPMService(rpmService);
        manager.setRPMServiceManager(rpmServiceMgr);

        AttributesService attService = new AttributesService();
        manager.setAttributesService(attService);

        ServiceFactory.setServiceManager(manager);

        Transaction.clearTransaction();
    }

    private Map<String, Object> createBrowserConfig(boolean isRumEnabled) {
        Map<String, Object> beaconSettings = new HashMap<>();
        beaconSettings.put(BrowserConfig.BROWSER_KEY, "3969ca217b");
        beaconSettings.put(BrowserConfig.BROWSER_LOADER_VERSION, "248");
        if (isRumEnabled) {
            beaconSettings.put(BrowserConfig.JS_AGENT_LOADER, LOADER);
            beaconSettings.put(BrowserConfig.JS_AGENT_FILE, "js-agent.newrelic.com\nr-248.min.js");
        }
        beaconSettings.put(BrowserConfig.BEACON, "staging-beacon-2.newrelic.com");
        beaconSettings.put(BrowserConfig.ERROR_BEACON, "staging-jserror.newrelic.com");
        beaconSettings.put(BrowserConfig.APPLICATION_ID, "45047");
        return beaconSettings;
    }

    private BasicRequestRootTracer createDispatcherTracer() {
        Transaction tx = Transaction.getTransaction();
        MockHttpRequest httpRequest = new MockHttpRequest();
        MockHttpResponse httpResponse = new MockHttpResponse();
        ClassMethodSignature sig = new ClassMethodSignature(getClass().getName(), "dude", "()V");
        return new BasicRequestRootTracer(tx, sig, this, httpRequest, httpResponse);
    }
}
