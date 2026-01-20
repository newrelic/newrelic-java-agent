package com.newrelic.agent.config;

import com.newrelic.agent.attributes.CrossAgentInput;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        System.out.println("I'm here!");
        for (Object test : tests) {
            JSONObject testObject = (JSONObject) test;
            String name = (String) testObject.get("test_name");
            System.out.println("name: " + name);
            result.add(new Object[]{name, testObject});
        }
        return result;
    }

    @Test
    public void testSamplerConfig() {

        System.out.println("name: " + testName + " data: " + testData);
        JSONObject testConfig = (JSONObject) testData.get("config");
        Map<String, Object> config = buildConfigFromJSON(testConfig);
        DistributedTracingConfig dtConfig = new DistributedTracingConfig(config);
        JSONObject expectedSamplers = (JSONObject) testData.get("expected_samplers");

        assertExpectedSampler(expectedSamplers.get("full_root"), dtConfig.getFullGranularityConfig().getRootSampler(), dtConfig.getFullGranularityConfig().isEnabled());
        assertExpectedSampler(expectedSamplers.get("full_remote_parent_sampled"), dtConfig.getFullGranularityConfig().getRemoteParentSampledSampler(), dtConfig.getFullGranularityConfig().isEnabled());
        assertExpectedSampler(expectedSamplers.get("full_remote_parent_not_sampled"), dtConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler(), dtConfig.getFullGranularityConfig().isEnabled());
        assertExpectedSampler(expectedSamplers.get("partial_root"), dtConfig.getPartialGranularityConfig().getRootSampler(), dtConfig.getPartialGranularityConfig().isEnabled());
        assertExpectedSampler(expectedSamplers.get("partial_remote_parent_sampled"), dtConfig.getPartialGranularityConfig().getRemoteParentSampledSampler(), dtConfig.getPartialGranularityConfig().isEnabled());
        assertExpectedSampler(expectedSamplers.get("partial_remote_parent_not_sampled"), dtConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler(), dtConfig.getPartialGranularityConfig().isEnabled());

        System.out.println("Done!");
    }

    private void assertExpectedSampler(Object expectedSampler, SamplerConfig actualSampler, boolean samplerEnabled) {
        if (expectedSampler == null) {
            Assert.assertFalse(samplerEnabled);
        } else {
            JSONObject expectedSamplerProps = (JSONObject) expectedSampler;

            String expectedType = (String) expectedSamplerProps.get("type");
            Assert.assertEquals(expectedType, actualSampler.getSamplerType());


            if (expectedSamplerProps.containsKey("ratio")){
                Float expectedRatio = ((Double) expectedSamplerProps.get("ratio")).floatValue();
                Assert.assertEquals(expectedRatio, actualSampler.getSamplerRatio(), 0.000001f);
            }

            if (expectedSamplerProps.containsKey("is_global_adaptive_sampler")){
                //if it's not the global adaptive sampler, then target should be null. Otherwise, it should be a number.
                Boolean isGlobalSampler = (Boolean) expectedSamplerProps.get("is_global_adaptive_sampler");
                if (isGlobalSampler) {
                    Assert.assertNull(actualSampler.getSamplingTarget());
                } else {
                    Assert.assertNotNull(actualSampler.getSamplingTarget());
                }
            }

//            if (expectedSamplerProps.containsKey("target")){
//                Integer expectedTarget = ((Long) expectedSamplerProps.get("target")).intValue();
//                //broken bc need to account for the case that it's a global sampler. move downwards to combine with is_global setting.
//                Assert.assertEquals(expectedTarget, actualSampler.getSamplingTarget());
//            }
        }
    }

    private Map<String, Object> buildConfigFromJSON(JSONObject testSpec) {
        Map<String, Object> config = new HashMap<>();

        if (testSpec.containsKey("sampler")){
            JSONObject samplerSpec =  (JSONObject) testSpec.get("sampler");

            //base sampler settings
            Map<String, Object> samplerConfig = new HashMap<>(getSamplerSettings(samplerSpec));
            samplerConfig.putAll(getPropertyIfPresent(samplerSpec, "adaptive_sampling_target"));

            //full granularity setting
            if (samplerSpec.containsKey("full_granularity")){
                JSONObject fgSpec = (JSONObject) samplerSpec.get("full_granularity");
                Map<String, Object> fgSettings = new HashMap<>(getPropertyIfPresent(fgSpec, "enabled"));
                samplerConfig.put("full_granularity", fgSettings);
            }

            //partial granularity settings
            if(samplerSpec.containsKey("partial_granularity")){
                JSONObject pgSpec = (JSONObject) samplerSpec.get("partial_granularity");
                Map<String, Object> pgSettings = new HashMap<>(getSamplerSettings(pgSpec));
                pgSettings.putAll(getPropertyIfPresent(pgSpec, "enabled"));
                samplerConfig.put("partial_granularity", pgSettings);

            }

            config.put("sampler", samplerConfig);
        }

        return config;
    }

    private Map<String, Object> getSamplerSettings(JSONObject specObj){
        Map<String, Object> samplers = new HashMap<>();
        String[] samplerCases = {"root", "remote_parent_sampled", "remote_parent_not_sampled"};
        for (String samplerCase : samplerCases) {
            if (specObj.containsKey(samplerCase)){
                Object rootSampler = specObj.get(samplerCase);
                if (rootSampler instanceof JSONObject) {
                    JSONObject rootSamplerSpec = (JSONObject) rootSampler;
                    Map<String, Object> rootSamplerSettings = new HashMap<>();
                    for (Object samplerType : rootSamplerSpec.keySet()) {
                        Map<String, Object> suboptions = new HashMap<>();
                        Object suboptionSpec = rootSamplerSpec.get(samplerType);
                        if (suboptionSpec instanceof JSONObject) {
                            JSONObject suboptionSpecObj = (JSONObject) suboptionSpec;
                            for (Object suboption : suboptionSpecObj.keySet()) {
                                suboptions.put((String) suboption, suboptionSpecObj.get(suboption));
                            }
                        } else {
                            throw new RuntimeException(samplerType + " is not a JSON object");
                        }
                        rootSamplerSettings.put((String) samplerType, suboptions);
                        samplers.put(samplerCase, rootSamplerSettings);
                    }
                } else if (rootSampler instanceof String) {
                    samplers.put(samplerCase, rootSampler);
                }
            }
        }
        return samplers;
    }

    private Map<String, Object> getPropertyIfPresent(JSONObject specObj, String propertyName) {
       Map<String, Object> propertySetting = new HashMap<>();
       if (specObj.containsKey(propertyName)){
           propertySetting.put(propertyName, specObj.get(propertyName));
       }
       return propertySetting;
    }


}
