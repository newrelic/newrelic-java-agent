/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.utilization;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AwsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

/**
 * Thin wrapper class over the JRE URL class to assist in testing
 */
public class AwsFargateMetadataFetcher {
    private final URL url;
    private final AwsConfig awsConfig;

    public AwsFargateMetadataFetcher(String metadataUrl, AwsConfig awsConfig) throws MalformedURLException {
        url = new URL(metadataUrl);
        this.awsConfig = awsConfig;
    }

    public InputStream openStream() throws IOException {
        URLConnection connection;

        if (awsConfig.isFargateMetadataEndpointProxyDisabled()) {
            Agent.LOG.info("AWS Fargate metadata endpoint proxy is DISABLED via configuration.");
            connection = url.openConnection(Proxy.NO_PROXY);
        } else {
            Agent.LOG.info("AWS Fargate metadata endpoint proxy is ENABLED.");
            connection = url.openConnection();
        }
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        return connection.getInputStream();
    }
}
