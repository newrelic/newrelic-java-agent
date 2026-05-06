/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.micronaut.http.client3;

import com.newrelic.api.agent.NewRelic;
import io.micronaut.http.HttpRequest;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

public class Utils {

    public static <T> boolean isFlux(Publisher<T> pub) {
        return (pub instanceof Flux);
    }

    public static <T> boolean isMono(Publisher<T> pub) {
        return (pub instanceof Mono);
    }

    public static URI getRequestURI(HttpRequest<?> request) {
        InetSocketAddress serverAddress = request.getServerAddress();
        URI reqURI = request.getUri();

        String scheme = reqURI != null ? reqURI.getScheme() : "http";
        String host = serverAddress.getHostName();
        int port = serverAddress.getPort();
        String path = request.getPath();

        URI uri = null;

        try {
            uri = new URI(scheme, null, host, port, path, null, null);
        } catch (URISyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Error getting URI");
        }

        if (uri == null) {
            uri = URI.create(scheme + "://UnknownHost");
        }

        return uri;
    }
}
