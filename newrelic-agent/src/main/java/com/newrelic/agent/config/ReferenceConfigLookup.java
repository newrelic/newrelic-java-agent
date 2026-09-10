/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Builds a lookup map from stripped env var keys (e.g. {@code jfr_enabled}) to their canonical
 * dot-notation config keys (e.g. {@code jfr.enabled}) by parsing the bundled reference YAML.
 */
class ReferenceConfigLookup {

    private static final String REFERENCE_YAML_RESOURCE = "/reference-newrelic.yml";
    private static final String COMMON_SECTION = "common";

    private ReferenceConfigLookup() {}

    // This static Holder class is used to insure on-demand, thread safe initialization.
    // This only loads the Map and Set if either of the getter methods are accessed.
    private static class Holder {
        static final Map<String, String> ENV_VAR_KEY_TO_CONFIG_KEY = buildLookupMap();
        static final Set<String> KNOWN_CONFIG_KEYS =
                Collections.unmodifiableSet(new HashSet<>(ENV_VAR_KEY_TO_CONFIG_KEY.values()));
    }

    static Map<String, String> getEnvVarKeyToConfigKeyMap() {
        return Holder.ENV_VAR_KEY_TO_CONFIG_KEY;
    }

    static Set<String> getKnownConfigKeys() {
        return Holder.KNOWN_CONFIG_KEYS;
    }

    private static Map<String, String> buildLookupMap() {
        try (InputStream stream = ReferenceConfigLookup.class.getResourceAsStream(REFERENCE_YAML_RESOURCE)) {
            if (stream == null) {
                Agent.LOG.log(Level.WARNING, "Could not find {0} on the classpath; env var config " +
                        "key mapping will be unavailable. This only affects Fleet Control integration - " +
                        "the agent will continue to function correctly.", REFERENCE_YAML_RESOURCE);
                return Collections.emptyMap();
            }

            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Map<String, Object> parsed = yaml.load(stream);
            if (parsed == null) {
                Agent.LOG.log(Level.WARNING, "Reference config file {0} is empty. This only affects " +
                        "Fleet Control integration - the agent will continue to function correctly.", REFERENCE_YAML_RESOURCE);
                return Collections.emptyMap();
            }

            Object commonSection = parsed.get(COMMON_SECTION);
            if (!(commonSection instanceof Map)) {
                Agent.LOG.log(Level.WARNING, "Reference config file {0} has no '{1}' section. This only " +
                        "affects Fleet Control integration - the agent will continue to function correctly.", REFERENCE_YAML_RESOURCE, COMMON_SECTION);
                return Collections.emptyMap();
            }

            Map<String, String> result = new HashMap<>();
            flattenKeys("", (Map<String, Object>) commonSection, result);
            return Collections.unmodifiableMap(result);
        } catch (Exception e) {
            Agent.LOG.log(Level.WARNING, "Failed to build env var config key lookup map from {0}: {1}. " +
                    "This only affects Fleet Control integration - the agent will continue to function correctly.", REFERENCE_YAML_RESOURCE, e.toString());
            return Collections.emptyMap();
        }
    }

    private static void flattenKeys(String prefix, Map<String, Object> map, Map<String, String> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String dotKey = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flattenKeys(dotKey, (Map<String, Object>) entry.getValue(), result);
            } else {
                // stripped env var key: replace dots and dashes with underscores (mirrors SystemPropertyProvider)
                String strippedEnvKey = dotKey.replaceAll("[.-]", "_");
                result.put(strippedEnvKey, dotKey);
            }
        }
    }
}
