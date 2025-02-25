/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.stats.StatsService;
import com.newrelic.agent.stats.StatsWorks;
import com.newrelic.agent.transport.HostConnectException;
import com.newrelic.agent.transport.HttpClientWrapper;
import com.newrelic.agent.transport.ReadResult;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static com.newrelic.agent.transport.DataSenderImpl.GZIP_ENCODING;

public class ApacheHttpClientWrapper implements HttpClientWrapper, Resource {
    private final ApacheProxyManager proxyManager;
    private PoolingHttpClientConnectionManager connectionManager;
    private CloseableHttpClient httpClient;
    private SSLContext sslContext;
    private final int defaultTimeoutInMillis;

    public ApacheHttpClientWrapper(ApacheProxyManager proxyManager, SSLContext sslContext, int defaultTimeoutInMillis) {
        this.proxyManager = proxyManager;
        this.connectionManager = createHttpClientConnectionManager(sslContext);
        this.httpClient = createHttpClient(defaultTimeoutInMillis);

        this.sslContext = sslContext;
        this.defaultTimeoutInMillis = defaultTimeoutInMillis;

        System.out.println("JGB Registering Core Resource: ApacheHttpClientWrapper");
        Core.getGlobalContext().register(this);
    }

    private static final String USER_AGENT_HEADER_VALUE = initUserHeaderValue();

    private static String initUserHeaderValue() {
        String arch = "unknown";
        String javaVersion = "unknown";
        try {
            arch = System.getProperty("os.arch");
            javaVersion = System.getProperty("java.version");
        } catch (Exception ignored) {
        }
        return MessageFormat.format("NewRelic-JavaAgent/{0} (java {1} {2})", Agent.getVersion(), javaVersion, arch);
    }

    private static PoolingHttpClientConnectionManager createHttpClientConnectionManager(SSLContext sslContext) {
        // Using the pooling manager here for thread safety.
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", sslContext != null ?
                                new SSLConnectionSocketFactory(sslContext) : SSLConnectionSocketFactory.getSocketFactory())
                        .build());

        // We only allow one connection at a time to the backend.
        // Anymore and the the agent hangs during the initial request to the connect endpoint.
        httpClientConnectionManager.setMaxTotal(1);
        httpClientConnectionManager.setDefaultMaxPerRoute(1);

        return httpClientConnectionManager;
    }

    private CloseableHttpClient createHttpClient(int requestTimeoutInMillis) {
        HttpClientBuilder builder = HttpClientBuilder.create()
                .setUserAgent(USER_AGENT_HEADER_VALUE)
                .setDefaultHeaders(Arrays.<Header>asList(
                        new BasicHeader("Connection", "Keep-Alive"),
                        new BasicHeader("CONTENT-TYPE", "application/json"),
                        new BasicHeader("ACCEPT-ENCODING", GZIP_ENCODING)))
                .setSSLHostnameVerifier(new DefaultHostnameVerifier())
                .setDefaultRequestConfig(RequestConfig.custom()
                        // Timeout in millis until a connection is established.
                        .setConnectTimeout(requestTimeoutInMillis)
                        // Timeout in millis when requesting a connection from the connection manager.
                        // This timeout should be longer than the connect timeout to avoid potential ConnectionPoolTimeoutExceptions.
                        .setConnectionRequestTimeout(requestTimeoutInMillis * 2)
                        // Timeout in millis for non-blocking socket I/O operations (aka max inactivity between two consecutive data packets).
                        .setSocketTimeout(requestTimeoutInMillis)
                        .build())
                .setDefaultSocketConfig(SocketConfig.custom()
                        // Timeout in millis for non-blocking socket I/O operations.
                        .setSoTimeout(requestTimeoutInMillis)
                        .setSoKeepAlive(true)
                        .build())
                .setConnectionManager(connectionManager);

        if (proxyManager.getProxy() != null) {
            builder.setProxy(proxyManager.getProxy());
        }

        return builder.build();
    }

    @Override
    public void shutdown() {
        connectionManager.closeIdleConnections(0, TimeUnit.SECONDS);
    }

    @Override
    public void captureSupportabilityMetrics(StatsService statsService, String requestHost) {
        Set<HttpRoute> routes = connectionManager.getRoutes();
        boolean willReuse = false;
        for (HttpRoute route : routes) {
            String hostName = route.getTargetHost().getHostName();
            if (hostName != null && hostName.equals(requestHost)) {
                statsService.doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_CONNECTION_REUSED, 1), MetricNames.SUPPORTABILITY_CONNECTION_REUSED);
                willReuse = true;
            }
        }
        if (!willReuse) {
            statsService.doStatsWork(StatsWorks.getIncrementCounterWork(MetricNames.SUPPORTABILITY_CONNECTION_NEW, 1), MetricNames.SUPPORTABILITY_CONNECTION_NEW);
        }
    }

    private HttpContext createContext() {
        return proxyManager.updateContext(HttpClientContext.create());
    }

    @Override
    public ReadResult execute(HttpClientWrapper.Request request, ExecuteEventHandler handler) throws Exception {
        HttpUriRequest apacheRequest = mapRequestToApacheRequest(request);

        if (handler != null) {
            handler.requestStarted();
        }
        logConnectionPoolStatus(apacheRequest);
        try (CloseableHttpResponse response = httpClient.execute(apacheRequest, createContext())) {
            if (handler != null) {
                handler.requestEnded();
            }
            logConnectionPoolStatus(apacheRequest);
            return mapResponseToResult(response);
        } catch (HttpHostConnectException hostConnectException) {
            throw new HostConnectException(hostConnectException.getHost().toString(), hostConnectException);
        }
    }

    private void logConnectionPoolStatus(HttpUriRequest apacheRequest) {
        if (Agent.isDebugEnabled()) {
            Agent.LOG.debug("Datasender HTTP connection pool status: "
                    + apacheRequest.getURI().getQuery().split("&")[0] + ", " + connectionManager.getTotalStats());
        }
    }

    private HttpUriRequest mapRequestToApacheRequest(Request request) throws URISyntaxException {
        RequestBuilder requestBuilder = null;
        switch (request.getVerb()) {
            case POST:
                requestBuilder = RequestBuilder.post();
                break;
            case PUT:
                requestBuilder = RequestBuilder.put();
                break;
        }

        requestBuilder
                .setUri(request.getURL().toURI())
                .setHeader(new BasicHeader("CONTENT-ENCODING", request.getEncoding()))
                .setEntity(new ByteArrayEntity(request.getData()));

        for (Map.Entry<String, String> entry : request.getRequestMetadata().entrySet()) {
            requestBuilder.addHeader(entry.getKey(), entry.getValue());
        }

        return requestBuilder.build();
    }

    private ReadResult mapResponseToResult(HttpResponse response) throws Exception {
        StatusLine statusLine = response.getStatusLine();

        if (statusLine == null) {
            throw new Exception("HttpClient returned null status line");
        }

        String proxyAuthenticateValue = getFirstProxyAuthenticateHeader(response);

        return ReadResult.create(
                statusLine.getStatusCode(),
                readResponseBody(response),
                proxyAuthenticateValue);
    }

    /**
     * Returns the first <a href="https://tools.ietf.org/html/rfc7235#section-4.3">Proxy-Authenticate</a> header
     * for indicating to the user that their proxy configuration isn't set up correctly.
     *
     * @param response The HttpResponse from the client
     * @return The value of the header.
     */
    private String getFirstProxyAuthenticateHeader(HttpResponse response) {
        String proxyAuthenticateValue = null;
        Header proxyAuthenticateHeader = response.getFirstHeader("Proxy-Authenticate");
        if (proxyAuthenticateHeader != null) {
            proxyAuthenticateValue = proxyAuthenticateHeader.getValue();
        }
        return proxyAuthenticateValue;
    }

    private String readResponseBody(HttpResponse response) throws Exception {
        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new Exception("The http response entity was null");
        }
        try (
                InputStream is = entity.getContent();
                BufferedReader in = getBufferedReader(response, is)
        ) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseBody.append(line);
            }
            return responseBody.toString();
        }
    }

    private BufferedReader getBufferedReader(HttpResponse response, InputStream is) throws IOException {
        Header encodingHeader = response.getFirstHeader("content-encoding");
        if (encodingHeader != null) {
            String encoding = encodingHeader.getValue();
            if (GZIP_ENCODING.equals(encoding)) {
                is = new GZIPInputStream(is);
            }
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        System.out.println("JGB stopping httpClient to collector");
        connectionManager.close();
        httpClient.close();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        connectionManager = createHttpClientConnectionManager(sslContext);
        httpClient = createHttpClient(defaultTimeoutInMillis);
        System.out.println("JGB started httpClient to collector");
    }

}
