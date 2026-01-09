/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.span;

import io.opentelemetry.api.trace.SpanKind;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class AttributeMapperTest {
    @Test
    public void getInstance_withValidJson_createsValidMapper() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();

        // 5 SpanKind enum values
        Map<SpanKind, Map<AttributeType, List<AttributeKey>>> mappings = attributeMapper.getMappings();
        assertEquals(5, mappings.size());

        // 14 AttributeType entries init'ed
        for (SpanKind spanKind : SpanKind.values()) {
            Map<AttributeType, List<AttributeKey>> attributeTypesBySpan = mappings.get(spanKind);
            assertEquals(14, attributeTypesBySpan.size());
        }

        Map<AttributeType, List<AttributeKey>> serverAttributes = mappings.get(SpanKind.SERVER);
        assertEquals(2, serverAttributes.get(AttributeType.Port).size()); // server.port, net.host.port
        assertEquals(3, serverAttributes.get(AttributeType.Host).size()); // server.address, net.host.name, user_agent.original
        assertEquals(3, serverAttributes.get(AttributeType.StatusCode).size()); // http.response.status_code, http.status_code, rpc.grpc.status_code
        assertEquals(3, serverAttributes.get(AttributeType.Method).size()); // http.request.method, http.method, rpc.method

        Map<AttributeType, List<AttributeKey>> clientAttributes = mappings.get(SpanKind.CLIENT);
        assertEquals(1, clientAttributes.get(AttributeType.DBName).size()); // db.name
        assertEquals(1, clientAttributes.get(AttributeType.DBOperation).size()); // db.operation
        assertEquals(1, clientAttributes.get(AttributeType.DBSystem).size()); // db.system
        assertEquals(1, clientAttributes.get(AttributeType.DBStatement).size()); // db.statement
        assertEquals(1, clientAttributes.get(AttributeType.DBTable).size()); // db.sql.table

        Map<AttributeType, List<AttributeKey>> consumerAttributes = mappings.get(SpanKind.CONSUMER);
        assertEquals(2, consumerAttributes.get(AttributeType.Queue).size()); // messaging.destination.name, messaging.destination

        Map<AttributeType, List<AttributeKey>> producerAttributes = mappings.get(SpanKind.PRODUCER);
        assertEquals(4, producerAttributes.get(AttributeType.Queue).size()); // messaging.destination.name, messaging.destination, aws_sqs, aws.region

        Map<AttributeType, List<AttributeKey>> internalAttributes = mappings.get(SpanKind.INTERNAL);
        for (AttributeType type : AttributeType.values()) {
            assertEquals(0, internalAttributes.get(type).size());
        }
    }

    @Test
    public void findProperOtelKey_returnsProperKey() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();
        Set<String> otelKeys = new HashSet<>();
        otelKeys.add("key1");
        otelKeys.add("key2");
        otelKeys.add("key3");
        otelKeys.add("server.port");    // Should find this in the mapper

        assertEquals("server.port", attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Port, otelKeys));
    }

    @Test
    public void findProperOtelKey_returnsEmptyString_whenRequestedKeyNotFound() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();
        Set<String> otelKeys = new HashSet<>();
        otelKeys.add("key1");
        otelKeys.add("key2");
        otelKeys.add("key3");

        assertEquals("", attributeMapper.findProperOtelKey(SpanKind.SERVER, AttributeType.Port, otelKeys));
    }

    @Test
    public void attributeKeyClass_properlyParsesSemanticConventionField() {
        AttributeMapper attributeMapper = AttributeMapper.getInstance();

        Map<SpanKind, Map<AttributeType, List<AttributeKey>>> attributes = attributeMapper.getMappings();

        AttributeKey attribute = attributes.get(SpanKind.SERVER).get(AttributeType.Port).get(0);
        assertEquals("server.port", attribute.getKey());
        assertEquals(1, attribute.getSemanticConventions().length);
        assertEquals("HTTP-Server:1.23", attribute.getSemanticConventions()[0]);

        attribute = attributes.get(SpanKind.SERVER).get(AttributeType.Host).get(0);
        assertEquals("server.address", attribute.getKey());
        assertEquals(1, attribute.getSemanticConventions().length);
        assertEquals("HTTP-Server:1.23", attribute.getSemanticConventions()[0]);

        attribute = attributes.get(SpanKind.SERVER).get(AttributeType.Host).get(2);
        assertEquals("user_agent.original", attribute.getKey());
        assertEquals(1, attribute.getSemanticConventions().length);
    }
}
