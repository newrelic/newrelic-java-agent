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

    final String[] EXPECTED_FOOTER_PROPERTIES = { "\"applicationID\":\"45047\"", "\"applicationTime\":[0-9]+",
            "\"beacon\":\"staging-beacon-2.newrelic.com\"", "\"queueTime\":[0-9]+", "\"licenseKey\":\"3969ca217b\"",
            "\"transactionName\":\"DxIJAw==\"", "\"agent\":\"js-agent.newrelic.com\\\\nr-248.min.js\"",
            "\"errorBeacon\":\"staging-jserror.newrelic.com\"", };

    final String[] EXPECTED_FOOTER_PROPERTIES_EMPTY_AGENT = { "\"applicationID\":\"45047\"",
            "\"applicationTime\":[0-9]+", "\"beacon\":\"staging-beacon-2.newrelic.com\"", "\"queueTime\":[0-9]+",
            "\"licenseKey\":\"3969ca217b\"", "\"transactionName\":\"DxIJAw==\"", "\"agent\":\"\"",
            "\"errorBeacon\":\"staging-jserror.newrelic.com\"", };

    // Check the start and end sequences of the RUM footer and add the matched strings to the
    // list of all matched strings for this test.
    public static void checkFooter(String toCheck, List<String> matched) {
        Assert.assertTrue(toCheck.startsWith(BrowserFooter.FOOTER_START_SCRIPT));
        matched.add(BrowserFooter.FOOTER_START_SCRIPT);
        Assert.assertTrue(toCheck.endsWith(BrowserFooter.FOOTER_END));
        matched.add(BrowserFooter.FOOTER_END);
    }

    // Check that value contains the JSON specified in the found in the array of expected strings,
    // plus the RUM footer start and end strings, and nothing else.
    public static void checkStrings(String toCheck, List<String> expectedFooterProperties, List<String> matched) {

        for (String toMatch : expectedFooterProperties) {
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
        Pattern expect = Pattern.compile(expectedFooterProperties.size() == 1 ? "\\{\\}" : "\\{[,]+\\}");
        Assert.assertTrue("Unexpected content: " + toCheck, expect.matcher(toCheck).matches());
    }

    // Validate the userParams in the value, then call checkStrings on the *remaining* values.
    public static void checkStringsAndUserParams(String toCheck, List<String> expectedFooterProperties,
            List<String> expectedUserAttributes, List<String> expectedAgentAttributes, List<String> matched) {

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
        toCheck = toCheck.replace(attributes, "");

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

        // Finally we can checkStrings on the non-attribute key, value pairs.
        checkStrings(toCheck, expectedFooterProperties, matched);
    }

    public void setupManager(boolean captureParams, boolean setSslForHttpToTrue) {
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

    @Test
    public void testHeader() throws Exception {
        setupManager(false, false);
        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());
    }

    @Test
    public void testHeaderWithNonce() throws Exception {
        setupManager(false, false);
        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        String expectedValue = "\n<script type=\"text/javascript\" nonce=\"123ABC\">" + LOADER + "</script>";
        Assert.assertEquals(expectedValue, beaconConfig.getBrowserTimingHeader("123ABC"));
    }

    @Test
    public void testFooterWithNonce() throws Exception {
        setupManager(true, false);
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

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);

        String value = beaconConfig.getBrowserTimingFooter(bts, "ABC123");
        List<String> matched = new ArrayList<>(2);

        String expectedStartScript = "\n<script type=\"text/javascript\" nonce=\"ABC123\">" + BrowserFooter.FOOTER_JS_START;

        Assert.assertTrue(value.startsWith(expectedStartScript));
        matched.add(expectedStartScript);
        Assert.assertTrue(value.endsWith(BrowserFooter.FOOTER_END));
        matched.add(BrowserFooter.FOOTER_END);

        final List<String> expectedFooterProperties = Arrays.asList(EXPECTED_FOOTER_PROPERTIES);
        // The whole point to the tricky code in checkStrings(), above, is that these key:value
        // pairs do not have to come back in the same order that they were added in, above.
        final String[] USER_ATTRIBUTES = { "\"theInt\":11", "\"theDouble\":11.22", "\"theLong\":22",
                "\"theString\":\"abc123\"", "\"theShort\":1" };
        final List<String> expectedUserAttributes = Arrays.asList(USER_ATTRIBUTES);
        checkStringsAndUserParams(value, expectedFooterProperties, expectedUserAttributes, null, matched);
    }

    @Test
    public void testRumDisabled() throws Exception {
        setupManager(false, false);
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);

        // off
        try {
            Map<String, Object> beaconSettings = createBeaconSettings(false);
            BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
            beaconConfig.getBrowserTimingHeader();
            Assert.fail("An exception should have been thrown when rum is disabled");
        } catch (Exception e) {
            // we should go into here
        }

        // back on
        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());
        Assert.assertTrue(beaconConfig.getBrowserTimingFooter(bts).startsWith(
                "\n<script type=\"text/javascript\">window.NREUM||"));
    }

    @Test
    public void testFooterNoCaptureParams() throws Exception {
        setupManager(false, false);
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

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        // The value looks something like this but with no line breaks:
        // <script type="text/javascript">window.NREUM||(NREUM={});NREUM.info={
        // "applicationID":"45047","applicationTime":4045,"beacon":"staging-beacon-2.newrelic.com","queueTime":0,
        // "licenseKey":"3969ca217b","transactionName":"DxIJAw==","agent":"js-agent.newrelic.com\nr-248.min.js",
        // "errorBeacon":"staging-jserror.newrelic.com"}</script>

        String value = beaconConfig.getBrowserTimingFooter(bts);

        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);
        List<String> expectedFooterProperties = Arrays.asList(EXPECTED_FOOTER_PROPERTIES);
        checkStrings(value, expectedFooterProperties, matched);
    }

    @Test
    public void testFooterCaptureAtts() throws Exception {
        setupManager(true, false);
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

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        String value = beaconConfig.getBrowserTimingFooter(bts);
        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);

        final List<String> expectedFooterProperties = Arrays.asList(EXPECTED_FOOTER_PROPERTIES);
        // The whole point to the tricky code in checkStrings(), above, is that these key:value
        // pairs do not have to come back in the same order that they were added in, above.
        final String[] USER_ATTRIBUTES = { "\"theInt\":11", "\"theDouble\":11.22", "\"theLong\":22",
                "\"theString\":\"abc123\"", "\"theShort\":1" };
        final List<String> expectedUserAttributes = Arrays.asList(USER_ATTRIBUTES);
        checkStringsAndUserParams(value, expectedFooterProperties, expectedUserAttributes, null, matched);
    }

    @Test
    public void testFooterCaptureAttsOneAndSsl() throws Exception {
        setupManager(true, true);
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        tx.getUserAttributes().put("product", "daProduct");

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        String value = beaconConfig.getBrowserTimingFooter(bts);
        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);

        final ArrayList<String> expectedFooterProperties = new ArrayList<>();
        expectedFooterProperties.addAll(Arrays.asList(EXPECTED_FOOTER_PROPERTIES));
        expectedFooterProperties.add("\"sslForHttp\":true");
        final String[] USER_ATTRIBUTES = { "\"product\":\"daProduct\"" };
        final List<String> expectedUserAttributes = Arrays.asList(USER_ATTRIBUTES);
        checkStringsAndUserParams(value, expectedFooterProperties, expectedUserAttributes, null, matched);
    }

    @Test
    public void testFooterCaptureAgnetAndUserAttsOneAndSsl() throws Exception {
        setupManager(true, true);
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);
        tx.getUserAttributes().put("product", "daProduct");
        tx.getAgentAttributes().put("jvmbb.thread", "123abc");
        // this one will get ignored
        tx.getAgentAttributes().put("jvm.thread", "123abc");

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        String value = beaconConfig.getBrowserTimingFooter(bts);
        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);

        ArrayList<String> expectedFooterProperties = new ArrayList<>();
        expectedFooterProperties.addAll(Arrays.asList(EXPECTED_FOOTER_PROPERTIES));
        expectedFooterProperties.add("\"sslForHttp\":true");

        String[] USER_ATTRIBUTES = { "\"product\":\"daProduct\"" };
        List<String> expectedUserAttributes = Arrays.asList(USER_ATTRIBUTES);

        String[] AGENT_ATTRIBUTES = { "\"jvmbb.thread\":\"123abc\"" };
        List<String> expectedAgentAttributes = Arrays.asList(AGENT_ATTRIBUTES);

        checkStringsAndUserParams(value, expectedFooterProperties, expectedUserAttributes, expectedAgentAttributes,
                matched);
    }

    @Test
    public void testFooterCaptureParamsNoParams() throws Exception {
        setupManager(true, false);
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);

        Map<String, Object> beaconSettings = createBeaconSettings(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        String value = beaconConfig.getBrowserTimingFooter(bts);
        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);

        List<String> expectedFooterProperties = Arrays.asList(EXPECTED_FOOTER_PROPERTIES);
        checkStrings(value, expectedFooterProperties, matched);
    }

    private Map<String, Object> createMap() {
        return new HashMap<>();
    }

    private Map<String, Object> createBeaconSettings(boolean isRumEnabled) {
        Map<String, Object> beaconSettings = createMap();
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

    private Map<String, Object> createBeaconSettingsEmtpyAgentFile(boolean isRumEnabled) {
        Map<String, Object> beaconSettings = createMap();
        beaconSettings.put(BrowserConfig.BROWSER_KEY, "3969ca217b");
        beaconSettings.put(BrowserConfig.BROWSER_LOADER_VERSION, "248");
        if (isRumEnabled) {
            beaconSettings.put(BrowserConfig.JS_AGENT_LOADER, LOADER);
            beaconSettings.put(BrowserConfig.JS_AGENT_FILE, "");
        }
        beaconSettings.put(BrowserConfig.BEACON, "staging-beacon-2.newrelic.com");
        beaconSettings.put(BrowserConfig.ERROR_BEACON, "staging-jserror.newrelic.com");
        beaconSettings.put(BrowserConfig.APPLICATION_ID, "45047");
        return beaconSettings;
    }

    @Test
    public void testHeaderNoAgentFile() throws Exception {
        setupManager(false, false);
        Map<String, Object> beaconSettings = createBeaconSettingsEmtpyAgentFile(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());
    }

    @Test
    public void testRumDisableNoAgentFile() throws Exception {
        setupManager(false, false);
        Transaction tx = Transaction.getTransaction();
        BasicRequestRootTracer tracer = createDispatcherTracer();
        tx.getTransactionActivity().tracerStarted(tracer);
        TransactionNamePriority expectedPriority = TransactionNamePriority.FILTER_NAME;
        PriorityTransactionName ptn = PriorityTransactionName.create("name", null, expectedPriority);
        tx.setPriorityTransactionName(ptn);

        // off
        try {
            Map<String, Object> beaconSettings = createBeaconSettingsEmtpyAgentFile(false);
            BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
            beaconConfig.getBrowserTimingHeader();
            Assert.fail("An exception should have been thrown when rum is disabled");
        } catch (Exception e) {
            // we should go into here
        }

        // back on
        Map<String, Object> beaconSettings = createBeaconSettingsEmtpyAgentFile(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());
        Assert.assertTrue(beaconConfig.getBrowserTimingFooter(bts).startsWith(
                "\n<script type=\"text/javascript\">window.NREUM||"));
    }

    @Test
    public void testFooterNoCaptureParamsNoAgentFile() throws Exception {
        setupManager(false, false);
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

        Map<String, Object> beaconSettings = createBeaconSettingsEmtpyAgentFile(true);
        BrowserConfig beaconConfig = BrowserConfig.createBrowserConfig("appName", beaconSettings);
        BrowserTransactionState bts = BrowserTransactionStateImpl.create(tx);
        Assert.assertEquals(HEADER, beaconConfig.getBrowserTimingHeader());

        // The value looks something like this but with no line breaks:
        // <script type="text/javascript">window.NREUM||(NREUM={});NREUM.info={
        // "applicationID":"45047","applicationTime":4045,"beacon":"staging-beacon-2.newrelic.com","queueTime":0,
        // "licenseKey":"3969ca217b","transactionName":"DxIJAw==","agent":"",
        // "errorBeacon":"staging-jserror.newrelic.com"}</script>

        String value = beaconConfig.getBrowserTimingFooter(bts);

        List<String> matched = new ArrayList<>(15);
        checkFooter(value, matched);
        List<String> expectedFooterProperties = Arrays.asList(EXPECTED_FOOTER_PROPERTIES_EMPTY_AGENT);
        checkStrings(value, expectedFooterProperties, matched);
    }

}
