/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.utilization;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Thin wrapper class over the JRE URL class to assist in testing
 */
public class AwsFargateMetadataFetcher {
    private final URL url;

    public AwsFargateMetadataFetcher(String metadataUrl) throws MalformedURLException {
        url = new URL(metadataUrl);
    }

    public InputStream openStream() throws IOException {
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        return connection.getInputStream();
    }
}
