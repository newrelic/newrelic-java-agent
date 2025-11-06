/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Creates the parameters to report a message that was pulled from a message queue. This should be used with
 * {@link TracedMethod#reportAsExternal(ExternalParameters)}. A fluent builder is provided to allow for easy usage and
 * management of this API.
 *
 * @since 3.36.0
 */
public class MessageConsumeParameters implements ExternalParameters {

    private final String library;
    private final String otelLibrary;
    private final DestinationType destinationType;
    private final String destinationName;
    private final InboundHeaders inboundHeaders;
    private final String cloudAccountId;
    private final String cloudRegion;
    private final String host;
    private final Integer port;

    private MessageConsumeParameters(Builder builder) {
        this.library = builder.library;
        this.otelLibrary = builder.otelLibrary;
        this.destinationType = builder.destinationType;
        this.destinationName = builder.destinationName;
        this.inboundHeaders = builder.inboundHeaders;
        this.cloudAccountId = builder.cloudAccountId;
        this.cloudRegion = builder.cloudRegion;
        this.host = builder.host;
        this.port = builder.port;
    }

    @Deprecated
    protected MessageConsumeParameters(String library, DestinationType destinationType, String destinationName,
            InboundHeaders inboundHeaders) {
        this.library = library;
        this.otelLibrary = null;
        this.destinationType = destinationType;
        this.destinationName = destinationName;
        this.inboundHeaders = inboundHeaders;
        this.cloudAccountId = null;
        this.cloudRegion = null;
        this.host = null;
        this.port = null;
    }

    protected MessageConsumeParameters(MessageConsumeParameters messageConsumeParameters) {
        this.library = messageConsumeParameters.library;
        this.otelLibrary = messageConsumeParameters.otelLibrary;
        this.destinationType = messageConsumeParameters.destinationType;
        this.destinationName = messageConsumeParameters.destinationName;
        this.inboundHeaders = messageConsumeParameters.inboundHeaders;
        this.cloudAccountId = messageConsumeParameters.cloudAccountId;
        this.cloudRegion = messageConsumeParameters.cloudRegion;
        this.host = messageConsumeParameters.host;
        this.port = messageConsumeParameters.port;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public InboundHeaders getInboundHeaders() {
        return inboundHeaders;
    }

    public String getCloudAccountId() {
        return cloudAccountId;
    }

    public String getCloudRegion() {
        return cloudRegion;
    }

    public String getLibrary() {
        return library;
    }

    public String getOtelLibrary() {
        return otelLibrary;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    protected static class Builder implements DestinationTypeParameter, DestinationNameParameter,
            InboundHeadersParameter, Build {
        private String library;
        private String otelLibrary;
        private DestinationType destinationType;
        private String destinationName;
        private InboundHeaders inboundHeaders;
        private String cloudAccountId;
        private String cloudRegion;
        private String host;
        private Integer port;

        public Builder(String library) {
            this.library = library;
        }

        public DestinationTypeParameter otelLibrary(String otelLibrary) {
            this.otelLibrary = otelLibrary;
            return this;
        }

        public DestinationNameParameter destinationType(DestinationType destinationType) {
            this.destinationType = destinationType;
            return this;
        }

        public InboundHeadersParameter destinationName(String destinationName) {
            this.destinationName = destinationName;
            return this;
        }

        public Build inboundHeaders(InboundHeaders inboundHeaders) {
            this.inboundHeaders = inboundHeaders;
            return this;
        }

        public Build cloudAccountId(String cloudAccountId) {
            this.cloudAccountId = cloudAccountId;
            return this;
        }

        public Build cloudRegion(String cloudRegion) {
            this.cloudRegion = cloudRegion;
            return this;
        }

        public Build instance(String host, Integer port) {
            this.host = host;
            this.port = port;
            return this;
        }

        public MessageConsumeParameters build() {
            return new MessageConsumeParameters(this);
        }
    }

    /**
     * Set the name of the library.
     *
     * @param library the name of the library
     * @return the next builder interface
     */
    public static DestinationTypeParameter library(String library) {
        return new MessageConsumeParameters.Builder(library);
    }

    /**
     * Set the name of the library. And the OTEL known name of that library.
     *
     * @param library the name of the library
     * @param otelLibrary the OTEL known name of the library
     * @return the next builder interface
     */
    public static DestinationTypeParameter library(String library, String otelLibrary) {
        return new MessageConsumeParameters.Builder(library)
                .otelLibrary(otelLibrary);
    }

    public interface DestinationTypeParameter {

        /**
         * Set the destination type of the external call.
         *
         * @param destinationType the destination type of the external call
         * @return the next builder interface
         */
        DestinationNameParameter destinationType(DestinationType destinationType);
    }

    public interface DestinationNameParameter {

        /**
         * Set the destination name of the external call.
         *
         * @param destinationName the destination name of the external call
         * @return the next builder interface
         */
        InboundHeadersParameter destinationName(String destinationName);
    }

    public interface InboundHeadersParameter {

        /**
         * Set the inbound headers on the external call.
         *
         * @param inboundHeaders the inbound headers for the external call
         * @return the completed HttpParameters object
         */
        Build inboundHeaders(InboundHeaders inboundHeaders);
    }

    public interface Build {

        /**
         * Set the cloud provider's account id for the message source.
         * This method is optional and can be bypassed by calling build directly.
         */
        Build cloudAccountId(String cloudAccountId);

        /**
         * Set the cloud provider's region for the message source.
         * This method is optional and can be bypassed by calling build directly.
         */
        Build cloudRegion(String region);

        /**
         * Set the host and port of the message broker.
         * This method is optional and can be bypassed by calling build directly.
         *
         * @param host The host where the message broker is located
         * @param port The port for the connection to the message broker
         * @return the next builder interface
         */
        Build instance(String host, Integer port);

        /**
         * Build the final {@link MessageConsumeParameters} for the API call.
         *
         * @return the completed GenericParameters object
         */
        MessageConsumeParameters build();
    }

}
