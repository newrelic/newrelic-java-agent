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
    private static final String MAPPING_RESOURCE = "attribute-mappings.json";
    private static final String INSTRUMENTATION_JAR_LOCATION = "instrumentation/opentelemetry-sdk-extension-autoconfigure-1.28.0-1.0";

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
                        JSONArray rootArray = parseJsonResourceFromAgentJar(INSTRUMENTATION_JAR_LOCATION, MAPPING_RESOURCE);

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

    private static JSONArray parseJsonResourceFromAgentJar(String instrumentationJarLocation, String jsonResourceName) {
        try (JarFile instrumentationJarFile = new JarFile(EmbeddedJarFilesImpl.INSTANCE.getJarFileInAgent(instrumentationJarLocation))) {
            JarEntry jsonFileJarEntry = instrumentationJarFile.getJarEntry(jsonResourceName);

            if (jsonFileJarEntry != null) {
                StringBuilder jsonStringBuilder = new StringBuilder();
                try (InputStream inputStream = instrumentationJarFile.getInputStream(jsonFileJarEntry)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        jsonStringBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                    }
                }
                String jsonString = jsonStringBuilder.toString();

                JSONParser parser = new JSONParser();
                return (JSONArray) parser.parse(jsonString);
            } else {
                NewRelic.getAgent().getLogger().log(Level.SEVERE, "OTel AttributeMapper: JarEntry `{0}` not found in the {1} jar file", jsonResourceName, instrumentationJarLocation);
            }
        } catch (IOException e) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE, "OTel AttributeMapper: IOException constructing JarFile: {0}", e.getMessage());
        } catch (ParseException e) {
            NewRelic.getAgent().getLogger().log(Level.SEVERE, "OTel AttributeMapper: ParseException parsing attribute mapping JSON: {0}", e.getMessage());
        }
        return null;
    }
}
