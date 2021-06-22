/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.external;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.GenericParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.newrelic.agent.bridge.external.ExternalMetrics.*;

/**
 * Utility class for sending External events.
 */
public class ExternalEvents {

    // External call event name
    public static final String EXTERNAL_CALL_EVENT = "ExternalCallEvent";

    // External call types
    public static final String HTTP_EXTERNAL = "http";
    public static final String MESSAGE_PRODUCE_EXTERNAL = "messageProduce";
    public static final String MESSAGE_CONSUME_EXTERNAL = "messageConsume";
    public static final String DATASTORE_EXTERNAL = "datastore";
    public static final String GENERIC_EXTERNAL = "generic";
    public static final String UNKNOWN_EXTERNAL = "unknown";

    // External call attributes
    public static final String EXTERNAL_CALL_TYPE = "externalCallType";
    public static final String LIBRARY = "library";
    public static final String URI = "uri";
    public static final String PROCEDURE = "procedure";
    public static final String INBOUND_RESPONSE_HEADERS = "inboundResponseHeaders";
    public static final String EXTENDED_INBOUND_RESPONSE_HEADERS = "extendedInboundResponseHeaders";
    public static final String OUTBOUND_HEADERS = "outboundHeaders";
    public static final String INBOUND_HEADERS = "inboundHeaders";
    public static final String DESTINATION_TYPE = "destinationType";
    public static final String DESTINATION_NAME = "destinationName";
    public static final String PRODUCT = "product";
    public static final String COLLECTION = "collection";
    public static final String OPERATION = "operation";
    public static final String INSTANCE_HOST = "instanceHost";
    public static final String INSTANCE_PORT = "instancePort";
    public static final String DATABASE_NAME = "databaseName";
    public static final String PATH_OR_ID = "pathOrId";
    public static final String SLOW_QUERY = "slowQuery";

    // FIXME we should probably null check attributes before adding them to the map
    public static Map<String, Object> getExternalParametersMap(ExternalParameters externalParameters) {
        if (externalParameters instanceof HttpParameters) {
            return getHttpParametersMap((HttpParameters) externalParameters);
        } else if (externalParameters instanceof DatastoreParameters) {
            return getDatastoreParametersMap((DatastoreParameters) externalParameters);
        } else if (externalParameters instanceof MessageConsumeParameters) {
            return getMessageConsumeParametersMap((MessageConsumeParameters) externalParameters);
        } else if (externalParameters instanceof MessageProduceParameters) {
            return getMessageProduceParametersMap((MessageProduceParameters) externalParameters);
        } else if (externalParameters instanceof GenericParameters) {
            return getGenericParametersMap((GenericParameters) externalParameters);
        } else {
            // This case shouldn't really be possible
            return Collections.singletonMap(EXTERNAL_CALL_TYPE, UNKNOWN_EXTERNAL);
        }
    }

    private static HashMap<String, Object> getHttpParametersMap(HttpParameters httpParameters) {
        HashMap<String, Object> attributes = new HashMap<>();
//            HttpParameters
//                    .library(LIBRARY)
//                    .uri(URI.create(uri))
//                    .procedure(PROCEDURE)
//                    .inboundHeaders(inboundHeaders)
//                    .build();
        attributes.put(EXTERNAL_CALL_TYPE, HTTP_EXTERNAL);
        attributes.put(LIBRARY, httpParameters.getLibrary());
        attributes.put(URI, sanitizeURI(httpParameters.getUri()));
        attributes.put(PROCEDURE, httpParameters.getProcedure());

        InboundHeaders extendedInboundResponseHeaders = httpParameters.getExtendedInboundResponseHeaders();
        if (extendedInboundResponseHeaders != null) {
            attributes.put(EXTENDED_INBOUND_RESPONSE_HEADERS, extendedInboundResponseHeaders);
        } else {
            attributes.put(INBOUND_RESPONSE_HEADERS, httpParameters.getInboundResponseHeaders());
        }
        return attributes;
    }

    private static HashMap<String, Object> getDatastoreParametersMap(DatastoreParameters datastoreParameters) {
        HashMap<String, Object> attributes = new HashMap<>();
//            DatastoreParameters
//                    .product(PRODUCT) // the datastore vendor
//                    .collection(COLLECTION) // the name of the collection (or table for SQL databases)
//                    .operation(OPERATION) // the operation being performed, e.g. "SELECT" or "UPDATE" for SQL databases
//                    .instance(HOST, PORT) // the datastore instance information - generally can be found as part of the connection
//                    .databaseName(DB_NAME) // may be null, indicating no keyspace for the command
//                    .slowQuery(RAW_QUERY, QUERY_CONVERTER) // report slow raw query, obfuscating if transaction_tracer.record_sql=obfuscated
//                    .build();
        attributes.put(EXTERNAL_CALL_TYPE, DATASTORE_EXTERNAL);
        attributes.put(PRODUCT, datastoreParameters.getProduct());
        attributes.put(COLLECTION, datastoreParameters.getCollection());
        attributes.put(OPERATION, datastoreParameters.getOperation());
        attributes.put(INSTANCE_HOST, datastoreParameters.getHost());
        attributes.put(INSTANCE_PORT, datastoreParameters.getPort());
        attributes.put(DATABASE_NAME, datastoreParameters.getDatabaseName());
        attributes.put(PATH_OR_ID, datastoreParameters.getPathOrId());
//            attributes.put(SLOW_QUERY, datastoreParameters.get());
        return attributes;
    }

    private static HashMap<String, Object> getMessageConsumeParametersMap(MessageConsumeParameters messageConsumeParameters) {
        HashMap<String, Object> attributes = new HashMap<>();
//            MessageConsumeParameters.library("JMS")
//                    .destinationType(DestinationType.NAMED_QUEUE)
//                    .destinationName(destinationName)
//                    .inboundHeaders(new InboundMessageWrapper(message))
//                    .build();
        attributes.put(EXTERNAL_CALL_TYPE, MESSAGE_CONSUME_EXTERNAL);
        attributes.put(LIBRARY, messageConsumeParameters.getLibrary());
        attributes.put(DESTINATION_TYPE, messageConsumeParameters.getDestinationType());
        attributes.put(DESTINATION_NAME, messageConsumeParameters.getDestinationName());
        attributes.put(INBOUND_HEADERS, messageConsumeParameters.getInboundHeaders());
        return attributes;
    }

    private static HashMap<String, Object> getMessageProduceParametersMap(MessageProduceParameters messageProduceParameters) {
        HashMap<String, Object> attributes = new HashMap<>();
//            MessageProduceParameters
//                    .library("JMS")
//                    .destinationType(DestinationType.NAMED_QUEUE)
//                    .destinationName(destinationName)
//                    .outboundHeaders(new OutboundMessageWrapper(message))
//                    .build();
        attributes.put(EXTERNAL_CALL_TYPE, MESSAGE_PRODUCE_EXTERNAL);
        attributes.put(LIBRARY, messageProduceParameters.getLibrary());
        attributes.put(DESTINATION_TYPE, messageProduceParameters.getDestinationType());
        attributes.put(DESTINATION_NAME, messageProduceParameters.getDestinationName());
        attributes.put(OUTBOUND_HEADERS, messageProduceParameters.getOutboundHeaders());
        return attributes;
    }

    private static HashMap<String, Object> getGenericParametersMap(GenericParameters genericParameters) {
        HashMap<String, Object> attributes = new HashMap<>();
//            GenericParameters
//                    .library(LIBRARY)
//                    .uri(URI.create(uri))
//                    .procedure(PROCEDURE)
//                    .build();
        attributes.put(EXTERNAL_CALL_TYPE, GENERIC_EXTERNAL);
        attributes.put(LIBRARY, genericParameters.getLibrary());
        attributes.put(URI, sanitizeURI(genericParameters.getUri()));
        attributes.put(PROCEDURE, genericParameters.getProcedure());
        return attributes;
    }

}