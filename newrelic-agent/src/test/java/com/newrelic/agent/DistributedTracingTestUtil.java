/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.newrelic.agent.tracing.Granularity;

import java.util.HashMap;
import java.util.Map;

public class DistributedTracingTestUtil {

    public static boolean isSampledPriorityForGranularity(float priority, Granularity granularity) {
        return granularity == Granularity.FULL ? priority >= 2.0f : priority >= 1.0f && priority <= 2.0f;
    }

    /**
     * The distributed tracing configuration is extremely complicated.
     * This builder provides a more user-friendly way to build a map of local configuration settings in tests.
     */
    public static class DTConfigMapBuilder {
        private final Map<String, Object> dtConfig = new HashMap<>();
        private final Map<String, Object> mainConfig = new HashMap<>();
        private final Map<String, Object> samplerConfig = new HashMap<>();
        private final Map<String, Object> fullGranularityConfig = new HashMap<>();
        private final Map<String, Object> partialGranularityConfig = new HashMap<>();

        /**
         * @return The distributed tracing config map. Can be passed to the constructor of DistributedTracingConfig.
         */
        public Map<String, Object> buildDtConfig() {
            return dtConfig;
        }

        /**
         * @return The main (agent) config map. Can be passed as the parameter for AgentConfigImpl.createAgentConfig.
         */
        public Map<String, Object> buildMainConfig(){
            return mainConfig;
        }

        public DTConfigMapBuilder withDTSetting(String key, Object val){
            getOrCreateDTSettings().put(key, val);
            return this;
        }

        public DTConfigMapBuilder withSamplerSetting(String key, Object val){
            getOrCreateSamplerSettings().put(key, val);
            return this;
        }

        public DTConfigMapBuilder withSamplerSetting(String samplerCase, String samplerType, String samplerSuboption, Object suboptionValue){
            Map<String, Object> suboptionMap = buildSuboptionMap(samplerType, samplerSuboption, suboptionValue);
            return withSamplerSetting(samplerCase, suboptionMap);
        }

        public DTConfigMapBuilder withFullGranularitySetting(String key, Object val){
            getOrCreateFullGranularitySettings().put(key, val);
            return this;
        }

        public DTConfigMapBuilder withFullGranularitySetting(String samplerCase, String samplerType, String samplerSuboption, Object suboptionValue){
            Map<String, Object> suboptionMap = buildSuboptionMap(samplerType, samplerSuboption, suboptionValue);
            return withFullGranularitySetting(samplerCase, suboptionMap);
        }

        public DTConfigMapBuilder withPartialGranularitySetting(String key, Object val){
            getOrCreatePartialGranularitySettings().put(key, val);
            return this;
        }

        public DTConfigMapBuilder withPartialGranularitySetting(String samplerCase, String samplerType, String samplerSuboption, Object suboptionValue){
            Map<String, Object> suboptionMap = buildSuboptionMap(samplerType, samplerSuboption, suboptionValue);
            return withPartialGranularitySetting(samplerCase, suboptionMap);
        }

        private Map<String, Object> getOrCreateDTSettings() {
            mainConfig.putIfAbsent("distributed_tracing", dtConfig);
            return dtConfig;
        }

        private Map<String, Object> getOrCreateSamplerSettings() {
            Map<String, Object> dtSettings = getOrCreateDTSettings();
            dtSettings.putIfAbsent("sampler", samplerConfig);
            return samplerConfig;
        }

        private Map<String, Object> getOrCreateFullGranularitySettings() {
            Map<String, Object> samplerSettings = getOrCreateSamplerSettings();
            samplerSettings.putIfAbsent("full_granularity", fullGranularityConfig);
            return fullGranularityConfig;
        }

        private Map<String, Object> getOrCreatePartialGranularitySettings() {
            Map<String, Object> samplerSettings = getOrCreateSamplerSettings();
            samplerSettings.putIfAbsent("partial_granularity", partialGranularityConfig);
            return partialGranularityConfig;
        }

        private Map<String, Object> buildSuboptionMap(String samplerType, String samplerSuboption, Object suboptionValue) {
            Map<String, Object> samplerTypeSettings = new HashMap<>();
            Map<String, Object> samplerSuboptionSettings = new HashMap<>();
            samplerSuboptionSettings.put(samplerSuboption, suboptionValue);
            samplerTypeSettings.put(samplerType, samplerSuboptionSettings);
            return samplerTypeSettings;
        }
    }
}
