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
    private final DestinationType destinationType;
    private final String destinationName;
    private final InboundHeaders inboundHeaders;
    private final String cloudResourceId;
    private final String host;
    private final Integer port;
    private final String amqpRoutingKey;
    private final String amqpQueue;

    protected MessageConsumeParameters(String library, DestinationType destinationType, String destinationName,
            InboundHeaders inboundHeaders, String cloudResourceId, String host, Integer port, String amqpRoutingKey, String amqpQueue) {
        this.library = library;
        this.destinationType = destinationType;
        this.destinationName = destinationName;
        this.inboundHeaders = inboundHeaders;
        this.cloudResourceId = cloudResourceId;
        this.host = host;
        this.port = port;
        this.amqpRoutingKey = amqpRoutingKey;
        this.amqpQueue = amqpQueue;
    }


    /**
     * This method creates a clone of a MessageConsumerParameters.
     * This class is immutable, so the original object can be reused.
     */
    @Deprecated
    protected MessageConsumeParameters(MessageConsumeParameters messageConsumeParameters) {
        this.library = messageConsumeParameters.library;
        this.destinationType = messageConsumeParameters.destinationType;
        this.destinationName = messageConsumeParameters.destinationName;
        this.inboundHeaders = messageConsumeParameters.inboundHeaders;
        this.cloudResourceId = messageConsumeParameters.cloudResourceId;
        this.host = messageConsumeParameters.host;
        this.port = messageConsumeParameters.port;
        this.amqpRoutingKey = messageConsumeParameters.amqpRoutingKey;
        this.amqpQueue = messageConsumeParameters.amqpQueue;
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

    public String getCloudResourceId() {
        return cloudResourceId;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getLibrary() {
        return library;
    }

    public String getAmqpRoutingKey() {
        return amqpRoutingKey;
    }

    public String getAmqpQueue() {
        return amqpQueue;
    }

    protected static class Builder implements DestinationTypeParameter, DestinationNameParameter,
            InboundHeadersParameter, Build {
        private String library;
        private DestinationType destinationType;
        private String destinationName;
        private InboundHeaders inboundHeaders;
        private String cloudResourceId;
        private String host;
        private Integer port;
        private String amqpRoutingKey;
        private String amqpQueue;

        public Builder(String library) {
            this.library = library;
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

        public Build cloudResourceId(String cloudResourceId) {
            this.cloudResourceId = cloudResourceId;
            return this;
        }

        public Build instance(String host, Integer port) {
            this.host = host;
            this.port = port;
            return this;
        }

        public Build amqp(String queue, String routingKey) {
            this.amqpQueue = queue;
            this.amqpRoutingKey = routingKey;
            return this;
        }

        public MessageConsumeParameters build() {
            return new MessageConsumeParameters(library, destinationType, destinationName, inboundHeaders,
                    cloudResourceId, host, port, amqpRoutingKey, amqpQueue);
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
         * Set the cloud provider's id for the message queue.
         * This method is optional and can be bypassed by calling build directly.
         */
        Build cloudResourceId(String cloudResourceId);

        /**
         * Set the host name and port number for the message queue.
         * This method is optional and can be bypassed by calling build directly.
         */
        Build instance(String host, Integer port);

        /**
         * If you are using the amqp protocol, this sets the routing key and queue.
         * This method is optional and can be bypassed by calling build directly.
         */
        Build amqp(String queue, String routingKey);

        /**
         * Build the final {@link MessageConsumeParameters} for the API call.
         *
         * @return the completed GenericParameters object
         */
        MessageConsumeParameters build();
    }

}
