/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.net.URI;

/**
 * Use to report an HTTP external call with cross application tracing.
 *
 * @since 3.36.0
 */
public class HttpParameters implements ExternalParameters {

    /**
     * The name of the framework being used to make the connection.
     */
    private final String library;

    /**
     * The external URI for the call.
     */
    private final URI uri;

    /**
     * The HTTP method for the call.
     */
    private final String procedure;

    private final Integer statusCode;

    private final String statusText;

    /**
     * The headers from the response of the external call.
     */
    private final InboundHeaders inboundResponseHeaders;

    /**
     * The headers from the response of the external call.
     */
    private final ExtendedInboundHeaders extendedInboundResponseHeaders;

    protected HttpParameters(String library, URI uri, String procedure, InboundHeaders inboundHeaders) {
        this(library, uri, procedure, inboundHeaders, null);
    }

    protected HttpParameters(String library, URI uri, String procedure, InboundHeaders inboundHeaders, ExtendedInboundHeaders extendedInboundHeaders) {
        this.library = library;
        this.uri = uri;
        this.procedure = procedure;
        this.statusCode = null;
        this.statusText = null;
        this.inboundResponseHeaders = inboundHeaders;
        this.extendedInboundResponseHeaders = extendedInboundHeaders;
    }

    protected HttpParameters(HttpParameters httpParameters) {
        this.library = httpParameters.library;
        this.uri = httpParameters.uri;
        this.procedure = httpParameters.procedure;
        this.statusCode = httpParameters.statusCode;
        this.statusText = httpParameters.statusText;
        this.inboundResponseHeaders = httpParameters.inboundResponseHeaders;
        this.extendedInboundResponseHeaders = null;
    }

    protected HttpParameters(Builder builder) {
        this.library = builder.library;
        this.uri = builder.uri;
        this.procedure = builder.procedure;
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.inboundResponseHeaders = builder.inboundHeaders;
        this.extendedInboundResponseHeaders = builder.extendedInboundHeaders;
    }

    /**
     * Returns the name of the framework used to make the connection.
     *
     * @return the Framework name
     * @since 3.36.0
     */
    public String getLibrary() {
        return library;
    }

    /**
     * Returns the URI for the external call.
     *
     * @return the URI for the call
     * @since 3.36.0
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Returns the HTTP method for the call.
     *
     * @return the HTTP Method called
     * @since 3.36.0
     */
    public String getProcedure() {
        return procedure;
    }

    /**
     * Returns the HTTP status code for the call.
     *
     * @return the status code for the call, null if not available
     * @since 6.5.0
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the HTTP reason message for the call.
     *
     * @return the text of the reason message, null if not available
     * @since 6.5.0
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * Returns the headers from the response of the external call.
     *
     * @return the response headers
     * @since 3.36.0
     */
    public InboundHeaders getInboundResponseHeaders() {
        return inboundResponseHeaders;
    }

    /**
     * Returns the headers from the response of the external call.
     *
     * @return the response headers
     * @since 5.8.0
     */
    public InboundHeaders getExtendedInboundResponseHeaders() {
        return extendedInboundResponseHeaders;
    }

    protected static class Builder implements UriParameter, ProcedureParameter, InboundHeadersParameter, Build {
        private String library;
        private URI uri;
        private String procedure;
        private Integer statusCode;
        private String statusText;
        private InboundHeaders inboundHeaders;
        private ExtendedInboundHeaders extendedInboundHeaders;

        public Builder(String library) {
            this.library = library;
        }

        public ProcedureParameter uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public InboundHeadersParameter procedure(String procedure) {
            this.procedure = procedure;
            return this;
        }

        public Build inboundHeaders(InboundHeaders inboundHeaders) {
            this.inboundHeaders = inboundHeaders;
            return this;
        }

        @Override
        public Build extendedInboundHeaders(ExtendedInboundHeaders extendedInboundHeaders) {
            this.extendedInboundHeaders = extendedInboundHeaders;
            return this;
        }

        public Build noInboundHeaders() {
            return this;
        }

        @Override
        public Build status(Integer statusCode, String statusText) {
            this.statusCode = statusCode;
            if (statusText != null && !statusText.isEmpty()) {
                this.statusText = statusText;
            }
            return this;
        }

        public HttpParameters build() {
            return new HttpParameters(this);
        }
    }

    /**
     * Set the name of the library.
     *
     * @param library the name of the library
     * @return the next builder interface
     */
    public static UriParameter library(String library) {
        return new HttpParameters.Builder(library);
    }

    public interface UriParameter {

        /**
         * Set the URI of the external call.
         *
         * @param uri the URI of the external call
         * @return the next builder interface
         */
        ProcedureParameter uri(URI uri);
    }

    public interface ProcedureParameter {

        /**
         * Set the HTTP method for the call.
         *
         * @param procedure the HTTP method for the call
         * @return the next builder interface
         */
        InboundHeadersParameter procedure(String procedure);
    }

    public interface InboundHeadersParameter {

        /**
         * Set the inbound headers on the HTTP call.
         *
         * @param inboundHeaders the inbound headers for the HTTP call
         * @return the completed HttpParameters object
         */
        Build inboundHeaders(InboundHeaders inboundHeaders);

        /**
         * Set the inbound headers on the HTTP call.
         *
         * @param extendedInboundHeaders the inbound headers for the HTTP call
         * @return the completed HttpParameters object
         */
        Build extendedInboundHeaders(ExtendedInboundHeaders extendedInboundHeaders);
        /**
         * No inbound headers.
         *
         * @return the completed HttpParameters object
         */
        Build noInboundHeaders();
    }

    public interface Build {

        /**
         * Set the status code/text of the HTTP response.
         * @return the builder in a buildable state.
         */
        Build status(Integer statusCode, String statusText);

        /**
         * Build the final {@link HttpParameters} for the API call.
         *
         * @return the completed GenericParameters object
         */
        HttpParameters build();
    }

}
