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

/**
 * Thin wrapper class over the JRE URL class to assist in testing
 */
public class AwsFargateMetadataFetcher {
    private final URL url;

    public AwsFargateMetadataFetcher(String metadataUrl) throws MalformedURLException {
        url = new URL(metadataUrl);
    }

    public InputStream openStream() throws IOException {
        return (url == null ? null : url.openStream());
    }
}
