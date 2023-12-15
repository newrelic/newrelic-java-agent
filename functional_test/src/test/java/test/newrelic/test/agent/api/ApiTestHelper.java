/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.api;

import com.newrelic.agent.MockRPMService;
import com.newrelic.agent.MockRPMServiceManager;
import com.newrelic.agent.browser.BrowserServiceImpl;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.errors.ErrorServiceImpl;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsServiceImpl;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Response;
import fi.iki.elonen.NanoHTTPD;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.Mockito;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/*
 * Helper class for ApiTest and CircuitBreakerApiTest
 */

public class ApiTestHelper {
    static final String LOADER = "window.NREUM||(NREUM={}),__nr_require=function a(b,c,d){function e(f){if(!c[f])"
            + "{var g=c[f]={exports:{}};b[f][0].call(g.exports,function(a){var c=b[f][1][a];return e(c?c:a)},g,g.exports,a,b,c,d)}return c[f].exports}"
            + "for(var f=0;f<d.length;f++)e(d[f]);return e}({\"4O2Y62\":[function(a,b){function c(a,b){var c=d[a];"
            + "return c?c.apply(this,b):(e[a]||(e[a]=[]),e[a].push(b),void 0)}var d={},e={};b.exports=c,c.queues=e,c.handlers=d},{}],"
            + "handle:[function(a,b){b.exports=a(\"4O2Y62\")},{}],\"SvQ0B+\":[function(a,b){function c(a){if(a===window)return 0;"
            + "if(e.call(a,\"__nr\"))return a.__nr;try{return Object.defineProperty(a,\"__nr\",{value:d,writable:!1,configurable:!1,enumerable:!1}),d}"
            + "catch(b){return a.__nr=d,d}finally{d+=1}}var d=1,e=Object.prototype.hasOwnProperty;b.exports=c},{}],"
            + "id:[function(a,b){b.exports=a(\"SvQ0B+\")},{}],YLUGVp:[function(a,b){function c(){var a=m.info=NREUM.info,"
            + "b=m.proto=\"http\"+(a.sslForHttp?\"s\":\"s\"==l[4]?\"s\":\"\")+\"://\";if(a&&a.agent&&a.licenseKey&&a.applicationID)"
            + "{f(\"mark\",[\"onload\",e()]);var c=h.createElement(\"script\");c.src=b+a.agent,h.body.appendChild(c)}}"
            + "function d(){\"complete\"===h.readyState&&f(\"mark\",[\"domContent\",e()])}function e(){return(new Date).getTime()}"
            + "var f=a(\"handle\"),g=window,h=g.document,i=\"readystatechange\",j=\"addEventListener\",k=\"attachEvent\",l=(\"\"+location).split(\"?\")[0],"
            + "m=b.exports={offset:e(),origin:l};h[j]?(h[j](i,d,!1),g[j](\"load\",c,!1)):(h[k](\"on\"+i,d),g[k](\"onload\",c)),"
            + "f(\"mark\",[\"firstbyte\",e()])},{handle:\"4O2Y62\"}],loader:[function(a,b){b.exports=a(\"YLUGVp\")},{}]},{},[\"YLUGVp\"]);";
    static final String JAVASCRIPT_AGENT_SCRIPT = "\n<script type=\"text/javascript\">window.NREUM||(NREUM={});NREUM.info={\"errorBeacon\":" +
            "\"staging-jserror.newrelic.com\",\"licenseKey\":\"abcd\",\"agent\":\"js-agent.newrelic.com\\nr-248.min.js\"," +
            "\"beacon\":\"staging-beacon-2.newrelic.com\",,\"applicationID\":\"100\"," +
            "\"transactionName\":\"MwADMBAECxUCAhIMDQpKNBYLSjAICA8JEgw=\",\"queueTime\":0};\n" +
            LOADER + "</script>";
    // From BeaconConfiguration
    public static final String BROWSER_KEY = "browser_key";
    public static final String BROWSER_LOADER_VERSION = "browser_monitoring.loader_version";
    public static final String JS_AGENT_LOADER = "js_agent_loader";
    public static final String JS_AGENT_FILE = "js_agent_file";
    public static final String BEACON = "beacon";
    public static final String ERROR_BEACON = "error_beacon";
    public static final String APPLICATION_ID = "application_id";

    // this comes from newrelic-agent/src/test/resources/com/newrelic/agent/config/newrelic.yml
    // the build.gradle calls that file out as the config file for running tests.
    static final String APP_NAME = "Unit Test";
    public ServiceManager serviceManager;
    public volatile TransactionStats tranStats;


    static void mockOutServiceManager() throws Exception {
        mockOutServiceManager(true, true, true, true, true, true);
    }

    static void mockOutServiceManager(boolean beacon, boolean errorBeacon, boolean jsAgentLoader, boolean jsAgentFile,
                                      boolean browserKey, boolean applicationId) {

        Map<String, Object> connectionResponse = new HashMap<>();

        if (jsAgentLoader) {
            connectionResponse.put(JS_AGENT_LOADER, LOADER);
            connectionResponse.put(BROWSER_LOADER_VERSION, "248");
        }
        if (jsAgentFile) {
            connectionResponse.put(JS_AGENT_FILE, "js-agent.newrelic.com\nr-248.min.js");
        }
        if (browserKey) {
            connectionResponse.put(BROWSER_KEY, "abcd");
        }
        if (applicationId) {
            connectionResponse.put(APPLICATION_ID, 100L);
        }
        if (beacon) {
            connectionResponse.put(BEACON, "staging-beacon-2.newrelic.com");
        }
        if (errorBeacon) {
            connectionResponse.put(ERROR_BEACON, "staging-jserror.newrelic.com");

        }
        mockOutServiceManager(connectionResponse);

    }

    static void mockOutServiceManager(Map<String, Object> connectionResponse) {
        ServiceManager mgr = Mockito.spy(ServiceFactory.getServiceManager());
        ServiceFactory.setServiceManager(mgr);

        MockRPMServiceManager rpmServiceMgr = new MockRPMServiceManager();

        MockRPMService rpmService = new MockRPMService();
        rpmService.setErrorService(new ErrorServiceImpl(APP_NAME));
        rpmService.setApplicationName(APP_NAME);
        rpmServiceMgr.setRPMService(rpmService);

        Mockito.doReturn(rpmServiceMgr).when(mgr).getRPMServiceManager();

        BrowserServiceImpl beaconService = new BrowserServiceImpl();
        Mockito.doReturn(beaconService).when(mgr).getBrowserService();

        StatsService statsService = new StatsServiceImpl();
        Mockito.doReturn(statsService).when(mgr).getStatsService();

        AgentConfig config = AgentConfigImpl.createAgentConfig(connectionResponse);
        beaconService.connected(rpmService, config);
    }

    public static class ResponseWrapper extends DummyResponse {
        private final HttpServletResponse response;

        public ResponseWrapper(HttpServletResponse response) {
            this.response = response;
        }

        @Override
        public void setHeader(String name, String value) {
            response.setHeader(name, value);
        }

        @Override
        public String getContentType() {
            return response.getContentType();
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

    public static class RequestWrapper extends DummyRequest {
        private final HttpServletRequest request;

        public RequestWrapper(HttpServletRequest request) {
            this.request = request;
        }

        @Override
        public String getRequestURI() {
            return request.getRequestURI();
        }

        @Override
        public String getRemoteUser() {
            return request.getRemoteUser();
        }

        @Override
        public Enumeration getParameterNames() {
            return request.getParameterNames();
        }

        @Override
        public String[] getParameterValues(String name) {
            return request.getParameterValues(name);
        }

        @Override
        public Object getAttribute(String name) {
            return request.getAttribute(name);
        }

        @Override
        public String getCookieValue(String name) {
            for (Cookie c : request.getCookies()) {
                if (name.equals(c.getName())) {
                    return c.getValue();
                }
            }
            return null;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.HTTP;
        }
    }

    public static class InboundWrapper extends DummyRequest {
        private final CloseableHttpResponse responseHeaders;
        private final HeaderType headerType;

        public InboundWrapper(CloseableHttpResponse requestHeaders, HeaderType headerType) {
            this.responseHeaders = requestHeaders;
            this.headerType = headerType;
        }

        @Override
        public HeaderType getHeaderType() {
            return this.headerType;
        }

        @Override
        public String getHeader(String name) {
            return this.responseHeaders.getFirstHeader(name.toLowerCase()).getValue();
        }
    }

    public static class OutboundWrapper extends DummyResponse {
        private final HttpUriRequest delegate;
        private final HeaderType headerType;

        public OutboundWrapper(HttpUriRequest request, HeaderType headerType) {
            this.delegate = request;
            this.headerType = headerType;
        }

        public void setHeader(String name, String value) {
            this.delegate.addHeader(name, value);
        }

        public HeaderType getHeaderType() {
            return headerType;
        }
    }

    public static class CustomRequestWrapper extends DummyRequest {
        NanoHTTPD.IHTTPSession session;
        HeaderType headerType;

        public CustomRequestWrapper(NanoHTTPD.IHTTPSession session, HeaderType headerType) {
            this.session = session;
            this.headerType = headerType;
        }

        @Override
        public HeaderType getHeaderType() {
            return this.headerType;
        }

        @Override
        public String getHeader(String name) {
            return session.getHeaders().get(name);
        }

    }

    public static class CustomResponseWrapper extends DummyResponse {
        NanoHTTPD.Response response;
        HeaderType headerType;

        public CustomResponseWrapper(NanoHTTPD.Response response, HeaderType headerType) {
            this.response = response;
            this.headerType = headerType;
        }

        @Override
        public HeaderType getHeaderType() {
            return this.headerType;
        }

        @Override
        public void setHeader(String name, String value) {
            response.addHeader(name, value);
        }
    }

    public static class DummyResponse implements Response {

        HeaderType headerType;
        private boolean setHeader = false;

        public DummyResponse() {
        }


        public DummyResponse(HeaderType headerType) {
            this.headerType = headerType;
        }

        @Override
        public HeaderType getHeaderType() {
            return headerType;
        }

        @Override
        public void setHeader(String name, String value) {
            setHeader = true;
        }

        @Override
        public int getStatus() throws Exception {
            return 200;
        }

        @Override
        public String getStatusMessage() throws Exception {
            return "HTTP 200 OK";
        }

        @Override
        public String getContentType() {
            return null;
        }

        public boolean didSetHeader() {
            return setHeader;
        }
    }

    public static class DummyRequest extends ExtendedRequest {

        boolean headerGet = false;
        HeaderType headerType;

        public DummyRequest() {

        }

        public DummyRequest(HeaderType headerType) {
            this.headerGet = headerGet;
            this.headerType = headerType;
        }

        @Override
        public HeaderType getHeaderType() {
            return headerType;
        }

        @Override
        public String getHeader(String name) {
            headerGet = true;
            return "12345";
        }

        @Override
        public String getRequestURI() {
            return null;
        }

        @Override
        public String getRemoteUser() {
            return null;
        }

        @Override
        public Enumeration getParameterNames() {
            return null;
        }

        @Override
        public String[] getParameterValues(String name) {
            return new String[0];
        }

        @Override
        public Object getAttribute(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        public boolean didGetHeader() {
            return headerGet;
        }

        @Override
        public String getMethod() {
            return null;
        }
    }
}
