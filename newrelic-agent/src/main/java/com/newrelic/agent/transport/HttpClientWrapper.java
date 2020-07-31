/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import com.newrelic.agent.stats.StatsService;

import java.net.URL;
import java.util.Map;

/**
 * Implement this interface for your HTTP Client.
 */
public interface HttpClientWrapper {
    /**
     * Executes a request, mapping the response body and status code. Errors may be throws from this method,
     * based on the client implementation.
     *
     * @param request The POJO representing the request.
     * @param eventHandler A handler (may be null) to receive events when the request starts and returns control.
     * @return The HTTP Response encapsulated in a POJO.
     * @throws Exception For anything that didn't result in a standard http response with a status code and body.
     */
    ReadResult execute(HttpClientWrapper.Request request, ExecuteEventHandler eventHandler) throws Exception;

    void captureSupportabilityMetrics(StatsService statsService, String requestHost);

    void shutdown();

    interface ExecuteEventHandler {
        void requestStarted();
        void requestEnded();
    }

    enum Verb {
        PUT,
        POST
    }

    class Request {
        public URL getURL() {
            return url;
        }

        public Request setURL(URL url) {
            this.url = url;
            return this;
        }

        public Verb getVerb() {
            return verb;
        }

        public Request setVerb(Verb verb) {
            this.verb = verb;
            return this;
        }

        public String getEncoding() {
            return encoding;
        }

        public Request setEncoding(String encoding) {
            this.encoding = encoding;
            return this;
        }

        public byte[] getData() {
            return data;
        }

        public Request setData(byte[] data) {
            this.data = data;
            return this;
        }

        public Map<String, String> getRequestMetadata() {
            return requestMetadata;
        }

        public Request setRequestMetadata(Map<String, String> requestMetadata) {
            this.requestMetadata = requestMetadata;
            return this;
        }

        private URL url;
        private Verb verb;
        private String encoding;
        private byte[] data;
        private Map<String, String> requestMetadata;
    }
}
