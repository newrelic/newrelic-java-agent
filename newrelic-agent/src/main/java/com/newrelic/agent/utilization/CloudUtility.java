/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsWorks;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.function.Function;

public class CloudUtility {

    // Spec: All characters should be in the following character class: over U+007F
    private static final int MIN_CHAR_CODEPOINT = "\u007F".codePointAt(0);
    private final Function<Integer, CloseableHttpClient> httpClientCreator;

    public CloudUtility() {
        this(CloudUtility::configureHttpClient);
    }

    CloudUtility(Function<Integer, CloseableHttpClient> httpClientCreator) {
        this.httpClientCreator = httpClientCreator;
    }

    public String httpGet(String url, int requestTimeoutMillis, String... headers) throws IOException {
        HttpGet httpGet = new HttpGet(url);

        return makeHttpRequest(httpGet, requestTimeoutMillis, headers);
    }

    public String httpPut(String url, int requestTimeoutMillis, String... headers) throws IOException {
        HttpPut httpPut = new HttpPut(url);

        return makeHttpRequest(httpPut, requestTimeoutMillis, headers);
    }

    private String makeHttpRequest(HttpUriRequest request, int requestTimeoutMillis, String[] headers)
            throws IOException {
        try (CloseableHttpClient httpclient = httpClientCreator.apply(requestTimeoutMillis)) {
            for (String header : headers) {
                String[] parts = header.split(":");
                request.addHeader(parts[0].trim(), parts[1].trim());
            }

            CloseableHttpResponse response = httpclient.execute(request);
            // status code should be in the 200s
            if (response.getStatusLine().getStatusCode() <= HttpStatus.SC_MULTI_STATUS) {
                return EntityUtils.toString(response.getEntity(), "UTF-8");
            }
        } catch (ConnectTimeoutException | UnknownHostException | SocketTimeoutException | SocketException ignored) {
            // we expect these values in situations where there is no cloud provider, or
            // we're on a different cloud provider than expected.
        }
        return null;
    }

    private static CloseableHttpClient configureHttpClient(int requestTimeoutInMillis) {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(requestTimeoutInMillis).setSoKeepAlive(true).build());
        RequestConfig.Builder requestBuilder = RequestConfig.custom().setConnectTimeout(requestTimeoutInMillis)
                .setConnectionRequestTimeout(requestTimeoutInMillis).setSocketTimeout(requestTimeoutInMillis);
        builder.setDefaultRequestConfig(requestBuilder.build());
        builder.setHostnameVerifier(new StrictHostnameVerifier());
        return builder.build();
    }

    public void recordError(String metricName) {
        ServiceFactory.getStatsService().doStatsWork(StatsWorks.getIncrementCounterWork(metricName, 1), metricName);
    }

    /**
     * @param value UTF-8 encoded String.
     * @return true if the value is invalid, false if it is valid.
     */
    boolean isInvalidValue(String value) {
        /*
         * No value should be longer than 255 bytes when encoded as a UTF-8 string.
         *
         * characters should be in the following character class: over U+007F or [0-9a-zA-Z_ ./-]. Notice that the space
         * character is in that character class, and is thus allowed.
         *
         * Note several common punctuation marks are not valid. These include equals (=), exclamation (!), reverse solidus (\), and comma (,).
         * These values are all uncommon in the generated IDs that we pull from instance metadata, so this should be an acceptable limitation.
         */
        if (value == null) {
            return true;
        }

        if (value.getBytes().length > 255) {
            return true;
        }

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'z') {
                continue;
            }

            if (c >= 'A' && c <= 'Z') {
                continue;
            }

            if (c == ' ' || c == '_' || c == '.' || c == '/' || c == '-') {
                continue;
            }

            if (c > MIN_CHAR_CODEPOINT) {
                continue;
            }

            // Invalid character
            return true;
        }

        return false;
    }
}
