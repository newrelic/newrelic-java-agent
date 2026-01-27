/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.tracing.Granularity;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SamplerConfigCrossAgentTest {

    @Parameterized.Parameter(0)
    public String testName;

    @Parameterized.Parameter(1)
    public JSONObject testData;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() throws Exception {
        JSONArray tests = CrossAgentInput.readJsonAndGetTests("com/newrelic/agent/cross_agent_tests/samplers/sampler_configuration.json");
        List<Object[]> result = new LinkedList<>();
        for (Object test : tests) {
            JSONObject testObject = (JSONObject) test;
            String name = (String) testObject.get("test_name");
            result.add(new Object[]{name, testObject});
        }
        return result;
    }

    @Test
    public void testSamplerConfig() {
        JSONObject testConfig = (JSONObject) testData.get("config");
        Map<String, Object> config = buildConfigFromJSON(testConfig);
        DistributedTracingConfig dtConfig = new DistributedTracingConfig(config);
        JSONObject expectedSamplers = (JSONObject) testData.get("expected_samplers");

        //check full samplers
        assertExpectedSampler(expectedSamplers.get("full_root"), dtConfig, "root", Granularity.FULL);
        assertExpectedSampler(expectedSamplers.get("full_remote_parent_sampled"), dtConfig, "remote_parent_sampled", Granularity.FULL);
        assertExpectedSampler(expectedSamplers.get("full_remote_parent_not_sampled"), dtConfig, "remote_parent_not_sampled", Granularity.FULL);

        //check partial samplers
        assertExpectedSampler(expectedSamplers.get("partial_root"), dtConfig, "root", Granularity.PARTIAL);
        assertExpectedSampler(expectedSamplers.get("partial_remote_parent_sampled"), dtConfig, "remote_parent_sampled", Granularity.PARTIAL);
        assertExpectedSampler(expectedSamplers.get("partial_remote_parent_not_sampled"), dtConfig, "remote_parent_not_sampled", Granularity.PARTIAL);
    }

    private void assertExpectedSampler(Object expectedSampler, DistributedTracingConfig dtConfig, String samplerCase, Granularity granularity) {
        CoreTracingConfig ctConfig = granularity == Granularity.FULL ? dtConfig.getFullGranularityConfig() : dtConfig.getPartialGranularityConfig();
        SamplerConfig actualSampler = ctConfig.getSamplerConfigForCase(samplerCase);
        boolean samplerEnabled = ctConfig.isEnabled();

        if (expectedSampler == null) {
            assertFalse(samplerEnabled);
        } else {
            //common assertions for all sampler types
            assertTrue(samplerEnabled);
            JSONObject expectedSamplerProps = (JSONObject) expectedSampler;
            String expectedType = (String) expectedSamplerProps.get("type");
            assertEquals(expectedType, actualSampler.getSamplerType());

            //type-specific assertions: trace_id_ratio_based sampler
            if (expectedType.equals("trace_id_ratio_based")){
                float expectedRatio = ((Double) expectedSamplerProps.get("ratio")).floatValue();
                assertEquals(expectedRatio, actualSampler.getSamplerRatio(), 0.000001f);
            }

            //type-specific assertions: adaptive sampler
            if (expectedType.equals("adaptive")){
                Boolean isGlobalSampler = (Boolean) expectedSamplerProps.get("is_global_adaptive_sampler");
                Integer expectedTarget = expectedSamplerProps.containsKey("target") ? ((Long) expectedSamplerProps.get("target")).intValue() : null;
                if (isGlobalSampler) {
                    assertNull(actualSampler.getSamplingTarget());
                    if (expectedTarget != null) {
                        assertEquals(expectedTarget.intValue(), dtConfig.getAdaptiveSamplingTarget());
                    }
                } else {
                    assertNotNull(actualSampler.getSamplingTarget());
                    assertEquals(expectedTarget.intValue(), actualSampler.getSamplingTarget().intValue());
                }
            }

        }
    }

    private Map<String, Object> buildConfigFromJSON(JSONObject testSpec) {
        Map<String, Object> config = new HashMap<>();
        if (testSpec.containsKey("sampler")) {
            config.put("sampler", parseJSONAsMap((JSONObject) testSpec.get("sampler")));
        }
        return config;
    }

    private Map<String, Object> parseJSONAsMap(JSONObject json) {
        Map<String, Object> result = new HashMap<>();
        for (Object key : json.keySet()) {
            result.put((String) key, parseValue(json.get(key)));
        }
        return result;
    }

    private Object parseValue(Object value) {
        //warning, this only handles JSON objects and primitives!! If you get parsing error, update this to be able to handle json arrays.
        if (value instanceof JSONObject) {
            return parseJSONAsMap((JSONObject) value);
        } else {
            return value;
        }
    }
}
