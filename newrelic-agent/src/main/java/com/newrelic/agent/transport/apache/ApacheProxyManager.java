/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.api.agent.Logger;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.HttpContext;

import java.text.MessageFormat;
import java.util.logging.Level;

public class ApacheProxyManager {
    private final HttpHost proxy;
    private final Credentials proxyCredentials;
    private final Logger logger;

    public ApacheProxyManager(String proxyHost, Integer proxyPort, String proxyScheme, String proxyUser, String proxyPassword, Logger logger) {
        this.logger = logger;

        if (proxyHost != null && proxyPort != null) {
            logger.log(Level.FINE, MessageFormat.format("Using proxy host {0}:{1}", proxyHost, Integer.toString(proxyPort)));
            proxy = new HttpHost(proxyHost, proxyPort, proxyScheme);
            proxyCredentials = getProxyCredentials(proxyUser, proxyPassword);
        } else {
            proxy = null;
            proxyCredentials = null;
        }
    }

    private Credentials getProxyCredentials(final String proxyUser, final String proxyPass) {
        if (proxyUser != null && proxyPass != null) {
            logger.log(Level.INFO, MessageFormat.format("Setting Proxy Authenticator for user {0}", proxyUser));
            return new UsernamePasswordCredentials(proxyUser, proxyPass);
        }
        return null;
    }

    public HttpHost getProxy() {
        return proxy;
    }

    public HttpContext updateContext(HttpClientContext httpClientContext) {
        if (proxy != null && proxyCredentials != null) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(proxy), proxyCredentials);
            httpClientContext.setCredentialsProvider(credentialsProvider);
        }

        return httpClientContext;
    }
}
