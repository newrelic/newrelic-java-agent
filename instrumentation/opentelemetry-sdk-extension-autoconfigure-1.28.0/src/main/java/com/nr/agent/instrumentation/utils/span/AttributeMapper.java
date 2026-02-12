/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.utils.span;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.bootstrap.EmbeddedJarFilesImpl;
import io.opentelemetry.api.trace.SpanKind;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public class AttributeMapper {
    private static volatile AttributeMapper instance;
    private final Map<SpanKind, Map<AttributeType, List<AttributeKey>>> mappings = new HashMap<>();

    private AttributeMapper() {
        for(SpanKind spanKind : SpanKind.values()) {
            Map<AttributeType, List<AttributeKey>> typeToKeysMap = new HashMap<>();
            for (AttributeType attributeType : AttributeType.values()) {
                typeToKeysMap.put(attributeType, new ArrayList<>());
            }
            this.mappings.put(spanKind, typeToKeysMap);
        }
    }

    public static AttributeMapper getInstance() {
        // Double-Checked Locking init
        if (instance == null) {
            synchronized (AttributeMapper.class) {
                if (instance == null) {
                    try {
                        JSONArray rootArray = parseJsonResourceString();

                        if (rootArray != null) {
                            instance = new AttributeMapper();

                            for (Object spanKindObj : rootArray) {
                                // Span kind and a list of attribute types
                                JSONObject spanKindObject = (JSONObject) spanKindObj;
                                SpanKind spanKind = SpanKind.valueOf((String) spanKindObject.get("spanKind"));
                                JSONArray jsonAttributeTypes = (JSONArray) spanKindObject.get("attributeTypes");

                                for (Object typeObj : jsonAttributeTypes) {
                                    JSONObject categoryObject = (JSONObject) typeObj;

                                    // Grab the attribute type (Port, Host, etc) and then iterate over the actual attribute keys
                                    AttributeType attributeType = AttributeType.valueOf((String) categoryObject.get("attributeType"));
                                    JSONArray jsonAttributes = (JSONArray) categoryObject.get("attributes");
                                    for (Object jsonAttribute : jsonAttributes) {
                                        JSONObject attribute = (JSONObject) jsonAttribute;
                                        instance.addAttributeMapping(spanKind, attributeType, new AttributeKey((String) attribute.get("name"), (String) attribute.get("version")));
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Should never happen...Maybe
                        NewRelic.getAgent().getLogger().log(Level.SEVERE, "OTel AttributeMapper: Unable to read OTel attribute mappings: {0}", e.getMessage());
                    }
                }
            }
        }

        return instance;
    }

    /**
     * Based on the SpanKind and type of attribute, search through the available OTel keys and find the correct key
     * since we don't know ahead of time what semantic convention version is in use and some keys vary based on
     * that version.
     *
     * @param spanKind the span kind (SERVER, CLIENT, PRODUCER, CONSUMER, INTERNAL)
     * @param type the "type" of key we're looking for: host, port, etc
     * @param otelKeys the available OTel keys to search through
     *
     * @return the available key String
     */
    public String findProperOtelKey(SpanKind spanKind, AttributeType type, Set<String> otelKeys) {
        List<AttributeKey> keys = mappings.get(spanKind).get(type);
        for (AttributeKey key : keys) {
            if (otelKeys.contains(key.getKey())) {
                return key.getKey();
            }
        }

        return "";
    }

    /**
     * Visible for testing
     *
     * @return the configured mappings: SpanKind --> Map<AttributeType, List<AttributeKey>>
     */
    Map<SpanKind, Map<AttributeType, List<AttributeKey>>> getMappings() {
        return mappings;
    }

    /**
     * Adds a new mapping to this mapper
     *
     * @param spanKind the SpanKind this mapping belongs to
     * @param attributeType the type (Port, Host...)
     * @param attributeKey the AttributeKey instance for this mapping
     */
    private void addAttributeMapping(SpanKind spanKind, AttributeType attributeType, AttributeKey attributeKey) {
        this.mappings.get(spanKind).get(attributeType).add(attributeKey);
    }

    private static JSONArray parseJsonResourceString() {
        try {
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(ATTRIBUTE_MAPPINGS_JSON);
        } catch (ParseException e) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE, "OTel AttributeMapper: ParseException parsing attribute mapping JSON: {0}", e.getMessage());
        }

        return null;
    }

    private static final String ATTRIBUTE_MAPPINGS_JSON =
            "[\n" +
            "  {\n" +
            "    \"spanKind\": \"SERVER\",\n" +
            "    \"attributeTypes\": [\n" +
            "      {\n" +
            "        \"attributeType\": \"Port\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.port\",\n" +
            "            \"version\": \"HTTP-Server:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.host.port\",\n" +
            "            \"version\": \"HTTP-Server:1.20\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Host\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.address\",\n" +
            "            \"version\": \"HTTP-Server:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.host.name\",\n" +
            "            \"version\": \"HTTP-Server:1.20\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"user_agent.original\",\n" +
            "            \"version\": \"\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"StatusCode\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.response.status_code\",\n" +
            "            \"version\": \"HTTP-Server:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"http.status_code\",\n" +
            "            \"version\": \"HTTP-Server:1.20\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"rpc.grpc.status_code\",\n" +
            "            \"version\": \"RPC-Server:1.20\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Method\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.request.method\",\n" +
            "            \"version\": \"HTTP-Server:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"http.method\",\n" +
            "            \"version\": \"HTTP-Server:1.20\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"rpc.method\",\n" +
            "            \"version\": \"RPC-Server:1.20\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"ExternalProcedure\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"code.function\",\n" +
            "            \"version\": \"\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Route\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.route\",\n" +
            "            \"version\": \"HTTP-Server:1.23,HTTP-Server:1.20\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.path\",\n" +
            "            \"version\": \"\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.full\",\n" +
            "            \"version\": \"\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.scheme\",\n" +
            "            \"version\": \"\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Component\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"rpc.system\",\n" +
            "            \"version\": \"RPC-Server:1.20\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"spanKind\": \"CONSUMER\",\n" +
            "    \"attributeTypes\": [\n" +
            "      {\n" +
            "        \"attributeType\": \"Queue\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"messaging.destination.name\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.destination\",\n" +
            "            \"version\": \"Messaging-Consumer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Host\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.address\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.name\",\n" +
            "            \"version\": \"Messaging-Consumer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Port\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.port\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.port\",\n" +
            "            \"version\": \"Messaging-Consumer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"RoutingKey\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"messaging.kafka.message.key\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.rabbitmq.destination.routing_key\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24,Messaging-Consumer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"spanKind\": \"CLIENT\",\n" +
            "    \"attributeTypes\": [\n" +
            "      {\n" +
            "        \"attributeType\": \"Route\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.route\",\n" +
            "            \"version\": \"HTTP-Server:1.23,HTTP-Server:1.20\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.path\",\n" +
            "            \"version\": \"\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.full\",\n" +
            "            \"version\": \"\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"url.scheme\",\n" +
            "            \"version\": \"\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"DBName\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"db.name\",\n" +
            "            \"version\": \"Redis-Client:1.17,Mongo-Client:1.24,Mongo-Client:1.17,DynamoDB-Client:1.17,DB-Client:1.24,DB-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"DBOperation\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"db.operation\",\n" +
            "            \"version\": \"Redis-Client:1.17,Mongo-Client:1.24,Mongo-Client:1.17,DynamoDB-Client:1.17,DB-Client:1.24,DB-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"DBSystem\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"db.system\",\n" +
            "            \"version\": \"Redis-Client:1.24,Redis-Client:1.17,Mongo-Client:1.24,Mongo-Client:1.17,DynamoDB-Client:1.17,DB-Client:1.24,DB-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"DBStatement\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"db.statement\",\n" +
            "            \"version\": \"Redis-Client:1.24,Redis-Client:1.17,Mongo-Client:1.24,Mongo-Client:1.17,DynamoDB-Client:1.17,DB-Client:1.24,DB-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"DBTable\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"db.sql.table\",\n" +
            "            \"version\": \"Redis-Client:1.24,Redis-Client:1.17,Mongo-Client:1.24,Mongo-Client:1.17,DynamoDB-Client:1.17,DB-Client:1.24,DB-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Host\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.address\",\n" +
            "            \"version\": \"Redis-Client:1.24,Mongo-Client:1.24,Mongo-Client:1.24,DB-Client:1.24,HTTP-Client:1.17,RPC-Client:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.name\",\n" +
            "            \"version\": \"Redis-Client:1.17,Mongo-Client:1.17,DB-Client:1.17,HTTP-Client:1.23,RPC-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Port\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.port\",\n" +
            "            \"version\": \"Redis-Client:1.24,Mongo-Client:1.24,DB-Client:1.24,HTTP-Client:1.17,RPC-Client:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.port\",\n" +
            "            \"version\": \"Redis-Client:1.17,Mongo-Client:1.17,DB-Client:1.17,HTTP-Client:1.23,RPC-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"StatusCode\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.status.code\",\n" +
            "            \"version\": \"HTTP-Server:1.23,HTTP-Server:1.20,HTTP-Client:1.17\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"http.response.status_code\",\n" +
            "            \"version\": \"HTTP-Client:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"rpc.grpc.status_code\",\n" +
            "            \"version\": \"RPC-Client:1.17,RPC-Client:1.23\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Method\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"http.method\",\n" +
            "            \"version\": \"HTTP-Client:1.17\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"http.request.method\",\n" +
            "            \"version\": \"HTTP-Client:1.23\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"rpc.method\",\n" +
            "            \"version\": \"RPC-Client:1.17,RPC-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"ExternalProcedure\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"code.function\",\n" +
            "            \"version\": \"\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Component\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"rpc.system\",\n" +
            "            \"version\": \"RPC-Client:1.17,RPC-Client:1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  {\n" +
            "    \"spanKind\": \"PRODUCER\",\n" +
            "    \"attributeTypes\": [\n" +
            "      {\n" +
            "        \"attributeType\": \"Queue\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"messaging.destination.name\",\n" +
            "            \"version\": \"Messaging-Consumer-1.24\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.destination\",\n" +
            "            \"version\": \"Messaging-Consumer-1.17\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"aws_sqs\",\n" +
            "            \"version\": \"SQS-Producer-1.17\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"aws.region\",\n" +
            "            \"version\": \"SQS-Producer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Host\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.address\",\n" +
            "            \"version\": \"Messaging-Producer-1.24,Messaging-Producer-1.30\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.name\",\n" +
            "            \"version\": \"Messaging-Producer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"Port\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"server.port\",\n" +
            "            \"version\": \"Messaging-Producer-1.24,Messaging-Producer-1.30\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"net.peer.port\",\n" +
            "            \"version\": \"Messaging-Producer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      {\n" +
            "        \"attributeType\": \"RoutingKey\",\n" +
            "        \"attributes\": [\n" +
            "          {\n" +
            "            \"name\": \"messaging.kafka.message.key\",\n" +
            "            \"version\": \"Messaging-Producer-1.24,Messaging-Producer-1.30\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.rabbitmq.destination.routing_key\",\n" +
            "            \"version\": \"Messaging-Producer-1.17,Messaging-Producer-1.24,Messaging-Producer-1.30\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.message.conversation_id\",\n" +
            "            \"version\": \"Messaging-Producer-1.17,Messaging-Producer-1.24,Messaging-Producer-1.30\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"messaging.destination\",\n" +
            "            \"version\": \"SQS-Producer-1.17\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "]";
}
