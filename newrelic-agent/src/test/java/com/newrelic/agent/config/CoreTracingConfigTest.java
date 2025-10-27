package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import com.newrelic.agent.config.coretracing.CoreTracingConfig;
import com.newrelic.agent.config.coretracing.PartialGranularityConfig;
import com.newrelic.agent.config.coretracing.SamplerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class CoreTracingConfigTest {
    private final Map<String, Object> samplerConfigProps = new HashMap<>();
    private final Map<String, Object> traceIdRatioBasedConfigProps = new HashMap<>();
    private final Map<String, Object> ratioConfigProps = new HashMap<>();
    private final Map<String, Object> nullValueConfigProps = new HashMap<>();
    private final Map<String, Object> localConfigProps = new HashMap<>();
    private final Map<String, Object> fullGranularityProps = new HashMap<>();
    private final Map<String, Object> partialGranularityProps = new HashMap<>();

    private DistributedTracingConfig distributedTracingConfig;

    @Before
    public void setup() {
        resetConfig();

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()));
    }

    public void resetConfig() {
        localConfigProps.clear();
        samplerConfigProps.clear();
        traceIdRatioBasedConfigProps.clear();
        ratioConfigProps.clear();
        nullValueConfigProps.clear();
        distributedTracingConfig = null;
    }

    @Test
    public void testDefaultConfigValues() {
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);
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
        samplerConfigProps.put(SamplerConfig.ADAPTIVE_SAMPLING_TARGET, 9);
        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.DEFAULT);
        ratioConfigProps.put(SamplerConfig.RATIO, 0.07);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, traceIdRatioBasedConfigProps);
        nullValueConfigProps.put(SamplerConfig.ALWAYS_OFF, null);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, nullValueConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        //Should have picked up the new adaptive sampling target
        assertEquals(9, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity should be enabled and obey all of the options that were set
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.07f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        //partial granularity should be disabled
        Assert.assertFalse(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
    }

    @Test
    public void testOnlyFullAndPartialGranularitySamplersSet() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      full_granularity:
         *        root:
         *          trace_id_ratio_based:
         *            ratio: 0.25f
         *        remote_parent_sampled:
         *          always_on:
         *      partial_granularity:
         *        enabled: true
         *        type: compact
         *        root: default
         *        remote_parent_not_sampled: always_off
         *
         */

        //full granularity
        samplerConfigProps.put(CoreTracingConfig.FULL_GRANULARITY, fullGranularityProps);
        fullGranularityProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        ratioConfigProps.put(SamplerConfig.RATIO, 0.25f);
        fullGranularityProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, nullValueConfigProps);
        nullValueConfigProps.put(SamplerConfig.ALWAYS_ON, null);

        //partial granularity
        samplerConfigProps.put(CoreTracingConfig.PARTIAL_GRANULARITY, partialGranularityProps);
        partialGranularityProps.put(CoreTracingConfig.ENABLED, true);
        partialGranularityProps.put(PartialGranularityConfig.TYPE, PartialGranularityConfig.COMPACT);
        partialGranularityProps.put(SamplerConfig.ROOT, SamplerConfig.DEFAULT);
        partialGranularityProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_OFF);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

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
        assertEquals(PartialGranularityConfig.COMPACT, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }

    @Test
    public void testFullGranularityShouldMergeWithBaseSamplerSettings() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        trace_id_ratio_based:
         *          ratio: 0.25f
         *      remote_parent_not_sampled: always_off
         *      full_granularity:
         *        root:
         *          adaptive:
         *              sampling_target: 110
         *        remote_parent_sampled:
         *          always_on:
         */

        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);
        ratioConfigProps.put(SamplerConfig.RATIO, 0.25);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_OFF);

        Map<String, Object> rootSamplerProps = new HashMap<>();
        Map<String, Object> adaptiveSamplerProps = new HashMap<>();
        rootSamplerProps.put(SamplerConfig.ADAPTIVE, adaptiveSamplerProps);
        adaptiveSamplerProps.put(SamplerConfig.SAMPLING_TARGET, 110);
        samplerConfigProps.put(CoreTracingConfig.FULL_GRANULARITY, fullGranularityProps);
        fullGranularityProps.put(SamplerConfig.ROOT, rootSamplerProps);
        fullGranularityProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, nullValueConfigProps);
        nullValueConfigProps.put(SamplerConfig.ALWAYS_ON, null);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        //all the assertions!
        assertEquals(120, distributedTracingConfig.getAdaptiveSamplingTarget());
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(110, (int) distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplingTarget());
        assertEquals(SamplerConfig.ALWAYS_ON, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

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

        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.ALWAYS_ON);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, SamplerConfig.ALWAYS_ON);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_ON);
        samplerConfigProps.put(CoreTracingConfig.PARTIAL_GRANULARITY, partialGranularityProps);
        partialGranularityProps.put(CoreTracingConfig.ENABLED, true);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(PartialGranularityConfig.PARTIAL_GRANULARITY_DEFAULT_TYPE, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplingTarget());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        assertNull(distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplingTarget());
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
         *      full_granularity:
         *        enabled: true
         *        root:
         *          trace_id_ratio_based:
         *            ratio: 0.2
         *        remote_parent_sampled:
         *          adaptive:
         *            sampling_target: 20
         *        remote_parent_not_sampled:
         *          always_off
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

        // Base sampler config
        samplerConfigProps.put(SamplerConfig.ADAPTIVE_SAMPLING_TARGET, 10);
        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        ratioConfigProps.put(SamplerConfig.RATIO, 0.1);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, SamplerConfig.ALWAYS_OFF);
        samplerConfigProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_ON);

        // Full granularity config
        Map<String, Object> fullGranularityRootRatioProps = new HashMap<>();
        Map<String, Object> fullGranularityRootTraceIdProps = new HashMap<>();
        Map<String, Object> fullGranularityRemoteParentSampledProps = new HashMap<>();
        Map<String, Object> fullGranularityRemoteParentSampledAdaptiveProps = new HashMap<>();

        fullGranularityProps.put(CoreTracingConfig.ENABLED, true);
        fullGranularityRootRatioProps.put(SamplerConfig.RATIO, 0.2);
        fullGranularityRootTraceIdProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, fullGranularityRootRatioProps);
        fullGranularityProps.put(SamplerConfig.ROOT, fullGranularityRootTraceIdProps);
        fullGranularityRemoteParentSampledAdaptiveProps.put(SamplerConfig.SAMPLING_TARGET, 20);
        fullGranularityRemoteParentSampledProps.put(SamplerConfig.ADAPTIVE, fullGranularityRemoteParentSampledAdaptiveProps);
        fullGranularityProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, fullGranularityRemoteParentSampledProps);
        fullGranularityProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_OFF);
        samplerConfigProps.put(CoreTracingConfig.FULL_GRANULARITY, fullGranularityProps);

        // Partial granularity config
        Map<String, Object> partialGranularityRootRatioProps = new HashMap<>();
        Map<String, Object> partialGranularityRootTraceIdProps = new HashMap<>();
        Map<String, Object> partialGranularityRemoteParentSampledRatioProps = new HashMap<>();
        Map<String, Object> partialGranularityRemoteParentSampledTraceIdProps = new HashMap<>();

        partialGranularityProps.put(CoreTracingConfig.ENABLED, true);
        partialGranularityProps.put(PartialGranularityConfig.TYPE, PartialGranularityConfig.ESSENTIAL);
        partialGranularityRootRatioProps.put(SamplerConfig.RATIO, 0.4);
        partialGranularityRootTraceIdProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, partialGranularityRootRatioProps);
        partialGranularityProps.put(SamplerConfig.ROOT, partialGranularityRootTraceIdProps);
        partialGranularityRemoteParentSampledRatioProps.put(SamplerConfig.RATIO, 1.0);
        partialGranularityRemoteParentSampledTraceIdProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, partialGranularityRemoteParentSampledRatioProps);
        partialGranularityProps.put(SamplerConfig.REMOTE_PARENT_SAMPLED, partialGranularityRemoteParentSampledTraceIdProps);
        partialGranularityProps.put(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, SamplerConfig.ALWAYS_OFF);
        samplerConfigProps.put(CoreTracingConfig.PARTIAL_GRANULARITY, partialGranularityProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then: Validate all configuration values

        // Adaptive sampling target
        assertEquals(10, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity assertions
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.2f, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(20, (int) distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        // Partial granularity assertions
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(PartialGranularityConfig.ESSENTIAL, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.4f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
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
         *      full_granularity:
         *        root: always_off
         *        remote_parent_not_sampled: always_on
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
        props.setProperty("newrelic.config.distributed_tracing.sampler.full_granularity.root", "always_off");
        props.setProperty("newrelic.config.distributed_tracing.sampler.full_granularity.remote_parent_not_sampled", "always_on");
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

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        //all the assertions!
        //top level configs
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(9, distributedTracingConfig.getAdaptiveSamplingTarget());
        //full granularity configs
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.25f, distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ALWAYS_ON, distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
        //partial granularity configs
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(PartialGranularityConfig.REDUCED, distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(0.8f, distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerRatio(), 0.0f);
        assertEquals(SamplerConfig.ADAPTIVE, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(15, (int) distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplingTarget());
        assertEquals(SamplerConfig.ALWAYS_OFF, distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }

    @Test
    public void testEnvironmentVariablesWithFullGranularityOverrides() {
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
         * Full granularity settings (conflicting overrides):
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_ROOT=always_off
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_REMOTE_PARENT_SAMPLED=trace_id_ratio_based
         * NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_REMOTE_PARENT_SAMPLED_TRACE_ID_RATIO_BASED_RATIO=0.1
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

                // Full granularity overrides (conflicting with base settings)
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_ROOT", "always_off")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_REMOTE_PARENT_SAMPLED",
                        "trace_id_ratio_based")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_FULL_GRANULARITY_REMOTE_PARENT_SAMPLED_TRACE_ID_RATIO_BASED_RATIO",
                        "0.1")

                // Partial granularity settings
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_ENABLED", "true")
                .put("NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_PARTIAL_GRANULARITY_TYPE", "reduced")
                .build();

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade(environmentVars)
        ));

        // When: Config is created from environment variables
        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then: All environment variable settings are applied correctly
        assertTrue(distributedTracingConfig.isEnabled());
        assertEquals(75, distributedTracingConfig.getAdaptiveSamplingTarget());

        // Full granularity assertions - full_granularity settings should override base sampler settings
        assertTrue(distributedTracingConfig.getFullGranularityConfig().isEnabled());
        assertEquals(SamplerConfig.ALWAYS_OFF,
                distributedTracingConfig.getFullGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED,
                distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(0.1f,
                distributedTracingConfig.getFullGranularityConfig().getRemoteParentSampledSampler().getSamplerRatio(),
                0.0f);
        assertEquals(SamplerConfig.ALWAYS_OFF,
                distributedTracingConfig.getFullGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());

        // Partial granularity assertions - enabled via env var
        assertTrue(distributedTracingConfig.getPartialGranularityConfig().isEnabled());
        assertEquals(PartialGranularityConfig.REDUCED,
                distributedTracingConfig.getPartialGranularityConfig().getType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRootSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRemoteParentSampledSampler().getSamplerType());
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE,
                distributedTracingConfig.getPartialGranularityConfig().getRemoteParentNotSampledSampler().getSamplerType());
    }
}