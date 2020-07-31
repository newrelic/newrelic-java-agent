/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class URISupport {
    /**
     * Gets the uri as a string without any query parameters.
     *
     * @param theUri The uri to convert.
     * @return The uri minus the query parameters.
     */
    public static String getURI(URI theUri) {
        if (theUri == null) {
            return "";
        }
        return getURI(theUri.getScheme(), theUri.getHost(), theUri.getPort(), theUri.getPath());
    }

    /**
     * Takes in a URL and returns the associated uri minus the query parameters.
     *
     * @param theUrl The URL to be converted.
     * @return The converted URI.
     */
    public static String getURI(URL theUrl) {
        if (theUrl == null) {
            return "";
        }
        try {
            return getURI(theUrl.toURI());
        } catch (URISyntaxException e) {
            return getURI(theUrl.getProtocol(), theUrl.getHost(), theUrl.getPort(), theUrl.getPath());
        }
    }

    public static String getURI(String scheme, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme);
            sb.append("://");
        }
        if (host != null) {
            sb.append(host);
            if (port >= 0) {
                sb.append(":");
                sb.append(port);
            }
        }
        if (path != null) {
            sb.append(path);
        }
        return sb.toString();
    }
}
