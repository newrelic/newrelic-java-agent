/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.agent.bridge.datastore.QueryConverter;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

import java.net.URI;

/**
 * @Deprecated Do not use. Use the builders on the subclasses of {@link com.newrelic.api.agent.ExternalParameters} instead.
 *
 * Creates the input parameter object for {@link TracedMethod}'s reportAsExternal API call. Use the static methods on
 * this factory to create the desired type of external request.
 *
 * @since 3.26.0
 */
@Deprecated
public final class ExternalParametersFactory {

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.GenericParameters#library(String)} instead.
     *
     * Creates the parameters for a basic external call. This should be used with {@link TracedMethod}'s
     * reportAsExternal method.
     *
     * @param library The name of the framework being used to make the connection.
     * @param uri The external URI for the call.
     * @param procedure The HTTP method or Java method for the call.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.26.0
     */
    @Deprecated
    public static ExternalParameters createForGenericExternal(String library, URI uri, String procedure) {
        return new GenericParameters(library, uri, procedure);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.HttpParameters#library(String)} instead.
     *
     * Creates the parameters for a HTTP external call which performs cross application tracing. This should be used
     * with {@link TracedMethod}'s reportAsExternal method.
     *
     * @param library The name of the framework being used to make the connection.
     * @param uri The external URI for the call.
     * @param procedure The HTTP method or Java method for the call.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.26.0
     */
    @Deprecated
    public static ExternalParameters createForHttp(String library, URI uri, String procedure) {
        return createForHttp(library, uri, procedure, null);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.HttpParameters#library(String)} instead.
     *
     * Creates the parameters for a HTTP external call which performs cross application tracing. This should be used
     * with {@link TracedMethod}'s reportAsExternal method.
     *
     * @param library The name of the framework being used to make the connection.
     * @param uri The external URI for the call.
     * @param procedure The HTTP method or Java method for the call.
     * @param inboundResponseHeaders The headers from the external response.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.26.0
     */
    @Deprecated
    public static ExternalParameters createForHttp(String library, URI uri, String procedure,
            InboundHeaders inboundResponseHeaders) {
        return new HttpParameters(library, uri, procedure, inboundResponseHeaders);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.DatastoreParameters#product(String)} instead.
     *
     * Creates the parameters for an external call to a datastore. This should be used with {@link TracedMethod}'s
     * reportAsExternal method.
     *
     * @param product The name of the vendor or driver.
     * @param collection The name of the collection or table.
     * @param operation The name of the datastore operation. This should be the primitive operation type accepted by the
     *                  datastore itself or the name of the API method in the client library.
     * @param host The external host.
     * @param port The external port.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.26.0
     */
    @Deprecated
    public static ExternalParameters createForDatastore(String product, String collection, String operation, String host,
            Integer port) {
        return new DatastoreParameters(product, collection, operation, host, port);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.DatastoreParameters#product(String)} instead.
     *
     * Creates the parameters for an external call to a datastore with additional slow query processing if the datastore
     * call is longer than the threshold. This should be used with {@link TracedMethod}'s reportAsExternal method.
     *
     * @param product The name of the vendor or driver.
     * @param collection The name of the collection or table.
     * @param operation The name of the datastore operation. This should be the primitive operation type accepted by the
     *                  datastore itself or the name of the API method in the client library.
     * @param host The external host.
     * @param port The external port.
     * @param rawQuery The raw query object used for transforming into a raw query String and obfuscated query String
     * @param queryConverter A converter to transform the rawQuery into a raw query String and obfuscated query String
     * @return The input parameters for the reportAsExternal method.
     * @since 3.27.0
     */
    @Deprecated
    public static <T> ExternalParameters createForDatastore(String product, String collection, String operation,
            String host, Integer port, T rawQuery, QueryConverter<T> queryConverter) {
        return new SlowQueryDatastoreParameters<>(new DatastoreParameters(product, collection, operation, host, port),
                rawQuery, queryConverter);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.DatastoreParameters#product(String)} instead.
     *
     * Creates the parameters for an external call to a datastore with additional slow query processing if the datastore
     * call is longer than the threshold. The slow query parameters also include input query information which allows
     * for the display of this information in addition to the slow query itself. An input query can be any query that
     * is used to generate the raw query that gets executed, for example when using Hibernate or an ORM.
     *
     * This should be used with {@link TracedMethod}'s reportAsExternal method.
     *
     * @param product The name of the vendor or driver.
     * @param collection The name of the collection or table.
     * @param operation The name of the datastore operation. This should be the primitive operation type accepted by the
     *                  datastore itself or the name of the API method in the client library.
     * @param host The external host.
     * @param port The external port.
     * @param rawQuery The raw query object used for transforming into a raw query String and obfuscated query String
     * @param queryConverter A converter to transform the rawQuery into a raw query String and obfuscated query String
     * @param inputQueryLabel The label used to display this input query in the UI
     * @param inputQuery The raw input query object used for transforming into a raw input query String and obfuscated
     *                   input query String
     * @param inputQueryConverter A converter to transform the rawInputQuery into a raw input query String and obfuscated
     *                            input query String
     * @return The input parameters for the reportAsExternal method.
     * @since 3.27.0
     */
    @Deprecated
    public static <T, I> ExternalParameters createForDatastore(String product, String collection, String operation,
            String host, Integer port, T rawQuery, QueryConverter<T> queryConverter, String inputQueryLabel,
            I inputQuery, QueryConverter<I> inputQueryConverter) {
        return new SlowQueryWithInputDatastoreParameters<>(new SlowQueryDatastoreParameters<>(
                new DatastoreParameters(product, collection, operation, host, port), rawQuery, queryConverter),
                inputQueryLabel, inputQuery, inputQueryConverter);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.DatastoreParameters#product(String)} instead.
     *
     * Creates the parameters for an external call to a datastore. This should be used with {@link TracedMethod}'s
     * reportAsExternal method.
     *
     * @param datastoreParameters The input parameters for the datastore call.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.33.0
     */
    @Deprecated
    public static ExternalParameters createForDatastore(DatastoreParameters datastoreParameters) {
        return datastoreParameters;
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.MessageProduceParameters#library(String)} instead.
     *
     * Creates the parameters to report a message that was sent to message queue. This should be used with {@link TracedMethod}'s
     * reportAsExternal method.
     *
     * @param destinationType Destination type of the message.
     * @param destinationName Name of the destination.
     * @param outboundHeaders The headers from the message.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.27.0
     */
    @Deprecated
    public static ExternalParameters createForMessageProduceOperation(String library, DestinationType destinationType,
            String destinationName, OutboundHeaders outboundHeaders) {
        return new MessageProduceParameters(library, destinationType, destinationName, outboundHeaders);
    }

    /**
     * @Deprecated Do not use. Use {@link com.newrelic.api.agent.MessageConsumeParameters#library(String)} instead.
     *
     * Creates the parameters to report a message consumed from message queue. Use this method to capture message (see {@link MessageInboundHeaders})
     * parameters. This should be used with {@link TracedMethod}'s reportAsExternal method.
     *
     * @param destinationType Destination type of the message.
     * @param destinationName Name of the destination.
     * @param inboundHeaders The headers from the message.
     * @return The input parameters for the reportAsExternal method.
     * @since 3.27.0
     */
    @Deprecated
    public static ExternalParameters createForMessageConsumeOperation(String library, DestinationType destinationType,
            String destinationName, InboundHeaders inboundHeaders) {
        return new MessageConsumeParameters(library, destinationType, destinationName, inboundHeaders);
    }

}
