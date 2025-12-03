package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.DistributedTracingConfigTestUtil.DTConfigMapBuilder;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import com.newrelic.agent.tracing.samplers.AdaptiveSampler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CoreTracingConfigTest {

    private DistributedTracingConfig distributedTracingConfig;

    @Before
    public void setup() {
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()));
    }

    @Test
    public void testDefaultConfigValues() {
        distributedTracingConfig = new DistributedTracingConfig(new HashMap<>());
        assertTrue(distributedTracingConfig.isEnabled());
        //full granularity samplers should all be adaptive by default.
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        //the default samplers should have a sampling target of null (instructing them to use the shared adaptive sampler)
        assertNull(distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplingTarget());
        assertNull(distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertNull(distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplingTarget());
        //full granularity is enabled by default
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        //partial granularity is disabled by default
        Assert.assertFalse(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        //adaptive sampling target is 120 by default
        assertEquals(120, distributedTracingConfig.getAdaptiveSamplingTarget());
    }

    @Test
    public void testOnlyBaseSamplerConfigSet() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      adaptive_sampling_target: 9
         *      root: default
         *      remote_parent_sampled:
         *        trace_id_ratio_based:
         *          ratio: 0.07
         *      remote_parent_not_sampled:
         *        always_off:
         */

        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("adaptive_sampling_target", 9)
                .withSamplerSetting("root", "default")
                .withSamplerSetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 0.07)
                .withSamplerSetting("remote_parent_not_sampled", "always_off", "", "")
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        //Should have picked up the new adaptive sampling target
        assertEquals(9, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity should be enabled and obey all of the options that were set
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.07f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        //partial granularity should be disabled
        assertFalse(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
    }

    @Test
    public void testOnlyFullAndPartialGranularitySamplersSet() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *     root:
         *       trace_id_ratio_based:
         *         ratio: 0.25f
         *     remote_parent_sampled:
         *        always_on:
         *      partial_granularity:
         *        enabled: true
         *        type: compact
         *        root: default
         *        remote_parent_not_sampled: always_off
         *
         */

        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.25)
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withPartialGranularitySetting("enabled", true)
                .withPartialGranularitySetting("type", "compact")
                .withPartialGranularitySetting("root", "default")
                .withPartialGranularitySetting("remote_parent_not_sampled", "always_off")
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        //now all the tests!

        //no adaptive sampling target was specified, so it should equal the default (120)
        assertEquals(120, distributedTracingConfig.getAdaptiveSamplingTarget());

        //full granularity assertions
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.25f, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_ON, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        //partial granularity assertions
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(Transaction.PartialSampleType.COMPACT, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }

    @Test
    public void testPartialGranularityEnabledDefaults() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: always_on
         *      remote_parent_sampled: always_on
         *      remote_parent_not_sampled: always_on
         *      partial_granularity:
         *        enabled: true
         *        //no type
         *        //no samplers
         */

        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("root", "always_on")
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withSamplerSetting("remote_parent_not_sampled", "always_on")
                .withPartialGranularitySetting("enabled", true)
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(Transaction.PartialSampleType.ESSENTIAL, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplingTarget());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplingTarget());
    }

    @Test
    public void testRatiosAreAdditiveWhenLayered(){
        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.25)
                .withSamplerSetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 0.1)
                .withSamplerSetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.33)
                .withPartialGranularitySetting("enabled", true)
                .withPartialGranularitySetting("root", "trace_id_ratio_based", "ratio", 0.4)
                .withPartialGranularitySetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 0.57)
                .withPartialGranularitySetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.15)
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        //full granularity assertions
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.25f, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.1f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertEquals(0.33f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerRatio(), 0.0f);

        //partial granularity assertions
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.65f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.00001f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.67f, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.00001f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertEquals(0.48f, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerRatio(), 0.00001f);
    }

    @Test
    public void testLayeredPartialRatiosDoNotAdjustWhenFullDisabled(){
        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.25)
                .withSamplerSetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 0.1)
                .withSamplerSetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.33)
                .withFullGranularitySetting("enabled", false)
                .withPartialGranularitySetting("enabled", true)
                .withPartialGranularitySetting("root", "trace_id_ratio_based", "ratio", 0.4)
                .withPartialGranularitySetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 0.57)
                .withPartialGranularitySetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.15)
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        //full granularity assertions
        assertFalse(distributedTracingConfig.getFullGranularityConfig().isEnabled());

        //partial granularity assertions
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.4f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.00001f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.57f, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.00001f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertEquals(0.15f, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerRatio(), 0.00001f);
    }

    @Test
    public void testGiantExampleFromLocalConfig(){
        // This example is lifted directly from the spec.
        /*
         *  distributed_tracing:
         *    sampler:
         *      adaptive_sampling_target: 10
         *      root:
         *        trace_id_ratio_based:
         *          ratio: 0.1
         *      remote_parent_sampled:
         *        always_off
         *      remote_parent_not_sampled:
         *        always_on
         *      partial_granularity:
         *        enabled: true
         *        type: "essential"
         *        root:
         *          trace_id_ratio_based:
         *            ratio: .4
         *        remote_parent_sampled:
         *          trace_id_ratio_based:
         *            ratio: 1.0
         *        remote_parent_not_sampled:
         *          always_off
         */

        Map<String, Object> dtSettings = new DTConfigMapBuilder()
                .withSamplerSetting("adaptive_sampling_target", 10)
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.1)
                .withSamplerSetting("remote_parent_sampled", "always_off")
                .withSamplerSetting("remote_parent_not_sampled", "always_on")
                .withPartialGranularitySetting("enabled", true)
                .withPartialGranularitySetting("type", "essential")
                .withPartialGranularitySetting("root", "trace_id_ratio_based", "ratio", 0.4)
                .withPartialGranularitySetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", 1.0)
                .withPartialGranularitySetting("remote_parent_not_sampled", "always_off")
                .buildDtConfig();

        distributedTracingConfig = new DistributedTracingConfig(dtSettings);

        // Then: Validate all configuration values

        // Adaptive sampling target
        assertEquals(10, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity assertions
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.1f, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.ALWAYS_ON, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        // Partial granularity assertions
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(Transaction.PartialSampleType.ESSENTIAL, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.5f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(1.0f, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }

    @Test
    public void testGiantExampleFromSysProps() {
        // This example has a little bit of everything. Overlapping properties, different samplers, you name it.

        /*
         *  distributed_tracing:
         *    sampler:
         *      adaptive_sampling_target: 9
         *      root: default
         *      remote_parent_sampled:
         *        trace_id_ratio_based:
         *          ratio: 0.25f
         *      partial_granularity:
         *        enabled: true
         *        type: reduced
         *        root:
         *          trace_id_ratio_based:
         *            ratio: 0.8
         *        remote_parent_sampled:
         *          adaptive:
         *            sampling_target: 15
         *        remote_parent_not_sampled: always_off
         */

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.adaptive_sampling_target", String.valueOf(9));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "default");
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_sampled", "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_sampled.trace_id_ratio_based.ratio", String.valueOf(0.25));
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.enabled",  String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.type",  "reduced");
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.root",  "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.root.trace_id_ratio_based.ratio",  String.valueOf(0.8));
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.remote_parent_sampled",  "adaptive");
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.remote_parent_sampled.adaptive.sampling_target", String.valueOf(15));
        props.setProperty("newrelic.config.distributed_tracing.sampler.partial_granularity.remote_parent_not_sampled",  "always_off");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(new HashMap<>());

        //all the assertions!
        //top level configs
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(9, distributedTracingConfig.getAdaptiveSamplingTarget());
        //full granularity configs
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertNull(distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplingTarget());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.25f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertNull(distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplingTarget());
        //partial granularity configs
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(Transaction.PartialSampleType.REDUCED, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.8f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(15, (int) distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }

    @Test
    public void testEnvironmentVariables() {
        // Given: Environment variables with conflicting base sampler and full_granularity settings
        /*
         * Environment Variables (with conflicts):
         *
         * Base sampler settings:
         * NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ADAPTIVE_SAMPLING_TARGET=75
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT=trace_id_ratio_based
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO=0.5
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_SAMPLED=always_on
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_NOT_SAMPLED=always_off
         *
         * Partial granularity settings:
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_ENABLED=true
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_TYPE=reduced
         *
         * Expected behavior: full_granularity settings should override conflicting base sampler settings
         */

        Map<String, String> environmentVars = ImmutableMap.<String, String>builder()
                // Base distributed tracing config
                .put("NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", "true")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ADAPTIVE_SAMPLING_TARGET", "75")

                // Base sampler settings
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "trace_id_ratio_based")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO", "0.5")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_SAMPLED", "always_on")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_NOT_SAMPLED", "always_off")

                // Partial granularity settings
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_ENABLED", "true")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_TYPE", "reduced")
                .build();

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(environmentVars)
        ));

        // When: Config is created from environment variables
        distributedTracingConfig = new DistributedTracingConfig(new HashMap<>());

        // Then: All environment variable settings are applied correctly
        assertTrue(distributedTracingConfig.isEnabled());
        assertEquals(75, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity assertions
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED,
                distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.5f,
                distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerRatio(),
                0.0f);
        assertEquals(SamplerConfig.ALWAYS_ON,
                distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.ALWAYS_OFF,
                distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        // Partial granularity assertions - enabled via env var
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(Transaction.PartialSampleType.REDUCED,
                distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }
}