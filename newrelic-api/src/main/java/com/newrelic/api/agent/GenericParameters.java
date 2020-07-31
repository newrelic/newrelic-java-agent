/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import java.net.URI;

/**
 * Parameters required to report a basic external call using {@link TracedMethod}'s reportAsExternal. A fluent builder
 * is provided to allow for easy usage and management of this API.
 *
 * @since 3.36.0
 */
public class GenericParameters implements ExternalParameters {

    /**
     * The name of the framework being used to make the connection.
     */
    private final String library;

    /**
     * The external URI for the call.
     */
    private final URI uri;

    /**
     * The HTTP method or Java method for the call.
     */
    private final String procedure;

    protected GenericParameters(String library, URI uri, String procedure) {
        this.library = library;
        this.uri = uri;
        this.procedure = procedure;
    }

    protected GenericParameters(GenericParameters genericParameters) {
        this.library = genericParameters.library;
        this.uri = genericParameters.uri;
        this.procedure = genericParameters.procedure;
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
     * @return the URI for the call.
     * @since 3.36.0
     */
    public URI getUri() {
        return this.uri;
    }

    /**
     * Returns the HTTP method or Java method for the call.
     *
     * @return the HTTP Method or Java method called.
     * @since 3.36.0
     */
    public String getProcedure() {
        return procedure;
    }

    protected static class Builder implements UriParameter, ProcedureParameter, Build {
        private String library;
        private URI uri;
        private String procedure;

        public Builder(String library) {
            this.library = library;
        }

        public ProcedureParameter uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Build procedure(String procedure) {
            this.procedure = procedure;
            return this;
        }

        public GenericParameters build() {
            return new GenericParameters(library, uri, procedure);
        }
    }

    /**
     * Set the name of the library.
     *
     * @param library the name of the library
     * @return the next builder interface
     */
    public static UriParameter library(String library) {
        return new GenericParameters.Builder(library);
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
         * Set the HTTP method or Java method for the call.
         *
         * @param procedure the HTTP method or Java method for the call
         * @return the next builder interface
         */
        Build procedure(String procedure);
    }

    public interface Build {

        /**
         * Build the final {@link GenericParameters} for the API call.
         *
         * @return the completed GenericParameters object
         */
        GenericParameters build();
    }

}
