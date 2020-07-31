/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import com.newrelic.agent.Agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class ExternalsUtil {

    /**
     * Reconstruct a URI, stripping out query parameters, user info, and fragment.
     *
     * @param uri uri to sanitize
     * @return reconstructed URI without userInfo, query parameters, or fragment.
     */
    public static URI sanitizeURI(URI uri) {
        try {
            if (uri == null || uri.getScheme() == null || uri.getHost() == null) {
                Agent.LOG.log(Level.FINE, "Invalid URI. URI parameter passed should include a valid scheme and host");
                return null;
            }

            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
