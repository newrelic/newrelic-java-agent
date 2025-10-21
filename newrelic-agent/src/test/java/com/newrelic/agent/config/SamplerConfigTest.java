/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableMap;
import com.newrelic.agent.SaveSystemPropertyProviderRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class SamplerConfigTest {
    private final Map<String, Object> samplerConfigProps = new HashMap<>();
    private final Map<String, Object> traceIdRatioBasedConfigProps = new HashMap<>();
    private final Map<String, Object> ratioConfigProps = new HashMap<>();
    private final Map<String, Object> nullValueConfigProps = new HashMap<>();
    private final Map<String, Object> localConfigProps = new HashMap<>();

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

        Assert.assertTrue(distributedTracingConfig.isEnabled());

        // If no explicit config is set, all samplers should default to ADAPTIVE with no sampling rate
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentSampledSamplerConfig = distributedTracingConfig.getRemoteParentSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampledSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, remoteParentSampledSamplerConfig.getSamplerType());
        Assert.assertNull(remoteParentSampledSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentNotSampledSamplerConfig = distributedTracingConfig.getRemoteParentNotSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampledSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, remoteParentNotSampledSamplerConfig.getSamplerType());
        Assert.assertNull(remoteParentNotSampledSamplerConfig.getSamplerRatio());
    }

    /*
     * Test with local config values for all three samplers.
     */

    @Test
    public void testValidLocalConfigValues() {
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

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentSampledSamplerConfig = distributedTracingConfig.getRemoteParentSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampledSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, remoteParentSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        assertEquals(Float.valueOf(0.07F), remoteParentSampledSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentNotSampledSamplerConfig = distributedTracingConfig.getRemoteParentNotSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampledSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, remoteParentNotSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(remoteParentNotSampledSamplerConfig.getSamplerRatio());
    }

    /*
     * The following tests cover various permutations of local config values for the "root" sampler only.
     * The other samplers are not tested here since they follow the same pattern.
     */

    @Test
    public void testValidLocalConfigVariationOne() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: default
         */
        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.DEFAULT);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationTwo() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        default:
         */
        nullValueConfigProps.put(SamplerConfig.DEFAULT, null);
        samplerConfigProps.put(SamplerConfig.ROOT, nullValueConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationThree() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        trace_id_ratio_based:
         *          ratio: 0.07
         */
        ratioConfigProps.put(SamplerConfig.RATIO, 0.07);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, rootSamplerConfig.getSamplerType());
        assertEquals(Float.valueOf(0.07F), rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationFour() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        always_off:
         */
        nullValueConfigProps.put(SamplerConfig.ALWAYS_OFF, null);
        samplerConfigProps.put(SamplerConfig.ROOT, nullValueConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationFive() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: always_off
         */
        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.ALWAYS_OFF);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationSix() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        always_on:
         */
        nullValueConfigProps.put(SamplerConfig.ALWAYS_ON, null);
        samplerConfigProps.put(SamplerConfig.ROOT, nullValueConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_on sampling type
        assertEquals(SamplerConfig.ALWAYS_ON, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidLocalConfigVariationSeven() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: always_on
         */
        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.ALWAYS_ON);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_on sampling type
        assertEquals(SamplerConfig.ALWAYS_ON, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidLocalConfigVariationOne() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: foo
         */
        samplerConfigProps.put(SamplerConfig.ROOT, "foo");

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidLocalConfigVariationTwo() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        trace_id_ratio_based:
         *          ratio:
         */
        ratioConfigProps.put(SamplerConfig.RATIO, null);
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidLocalConfigVariationThree() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        trace_id_ratio_based:
         *          ratio: foo
         */
        ratioConfigProps.put(SamplerConfig.RATIO, "foo");
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, ratioConfigProps);
        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidLocalConfigVariationFour() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root:
         *        trace_id_ratio_based:
         */
        traceIdRatioBasedConfigProps.put(SamplerConfig.TRACE_ID_RATIO_BASED, null);
        samplerConfigProps.put(SamplerConfig.ROOT, traceIdRatioBasedConfigProps);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidLocalConfigVariationFive() {
        // Given

        /*
         *  distributed_tracing:
         *    sampler:
         *      root: trace_id_ratio_based
         */
        samplerConfigProps.put(SamplerConfig.ROOT, SamplerConfig.TRACE_ID_RATIO_BASED);

        // Local config props
        localConfigProps.put(SamplerConfig.SAMPLER_CONFIG_ROOT, samplerConfigProps);

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    /*
     * Test with system property config values for all three samplers.
     */

    @Test
    public void testValidSystemProperties() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.adaptive_sampling_target", String.valueOf(9));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "default");
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_sampled", "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_sampled.trace_id_ratio_based.ratio", String.valueOf(0.07));
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_not_sampled", "always_off");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentSampledSamplerConfig = distributedTracingConfig.getRemoteParentSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampledSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, remoteParentSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        assertEquals(Float.valueOf(0.07F), remoteParentSampledSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentNotSampledSamplerConfig = distributedTracingConfig.getRemoteParentNotSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampledSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, remoteParentNotSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(remoteParentNotSampledSamplerConfig.getSamplerRatio());
    }

    /*
     * The following tests cover various permutations of system property config values for the "root" sampler only.
     * The other samplers are not tested here since they follow the same pattern.
     */

    @Test
    public void testValidSystemPropertiesVariationOne() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "default");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidSystemPropertiesVariationTwo() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.root.trace_id_ratio_based.ratio", String.valueOf(0.07));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, rootSamplerConfig.getSamplerType());
        assertEquals(Float.valueOf(0.07F), rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidSystemPropertiesVariationThree() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "always_off");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidSystemPropertiesVariationFour() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "always_on");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_on sampling type
        assertEquals(SamplerConfig.ALWAYS_ON, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidSystemPropertiesVariationOne() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "foo");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidSystemPropertiesVariationTwo() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.root.trace_id_ratio_based.ratio", "");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidSystemPropertiesVariationThree() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "trace_id_ratio_based");
        props.setProperty("newrelic.config.distributed_tracing.sampler.root.trace_id_ratio_based.ratio", "foo");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidSystemPropertiesVariationFour() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root.trace_id_ratio_based", "");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidSystemPropertiesVariationFive() {
        // Given

        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "trace_id_ratio_based");

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                new SaveSystemPropertyProviderRule.TestEnvironmentFacade()
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    /*
     * Test with environment variable config values for all three samplers.
     */

    @Test
    public void testValidEnvironmentVariables() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ADAPTIVE_SAMPLING_TARGET", String.valueOf(9),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "default",
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_SAMPLED", "trace_id_ratio_based",
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_SAMPLED_TRACE_ID_RATIO_BASED_RATIO", String.valueOf(0.07),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_NOT_SAMPLED", "always_off"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentSampledSamplerConfig = distributedTracingConfig.getRemoteParentSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampledSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, remoteParentSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        assertEquals(Float.valueOf(0.07F), remoteParentSampledSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentNotSampledSamplerConfig = distributedTracingConfig.getRemoteParentNotSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampledSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, remoteParentNotSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(remoteParentNotSampledSamplerConfig.getSamplerRatio());
    }

    /*
     * The following tests cover various permutations of environment variable config values for the "root" sampler only.
     * The other samplers are not tested here since they follow the same pattern.
     */

    @Test
    public void testValidEnvironmentVariablesVariationOne() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "default"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidEnvironmentVariablesVariationTwo() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "trace_id_ratio_based",
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO", String.valueOf(0.07)
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, rootSamplerConfig.getSamplerType());
        assertEquals(Float.valueOf(0.07F), rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidEnvironmentVariablesVariationThree() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "always_off"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testValidEnvironmentVariablesVariationFour() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "always_on"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_on sampling type
        assertEquals(SamplerConfig.ALWAYS_ON, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidEnvironmentVariablesVariationOne() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "foo"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidEnvironmentVariablesVariationTwo() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "trace_id_ratio_based",
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO", ""
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidEnvironmentVariablesVariationThree() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "trace_id_ratio_based",
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED_RATIO", "foo"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidEnvironmentVariablesVariationFour() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT_TRACE_ID_RATIO_BASED", ""
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    @Test
    public void testInvalidEnvironmentVariablesVariationFive() {
        // Given

        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_ROOT", "trace_id_ratio_based"
        ));
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(),
                environmentFacade
        ));

        distributedTracingConfig = new DistributedTracingConfig(localConfigProps);

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the default sampling type
        assertEquals(SamplerConfig.DEFAULT_SAMPLER_TYPE, rootSamplerConfig.getSamplerType());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());
    }

    /*
     * Test with local config, system property, and environment variable values for all three samplers.
     * Ensure that precedence is: environment variables > system properties > local config.
     */

    @Test
    public void testConfigPrecedence() {
        // Given

        // set environment variable config
        SaveSystemPropertyProviderRule.TestEnvironmentFacade environmentFacade = new SaveSystemPropertyProviderRule.TestEnvironmentFacade(ImmutableMap.of(
                "NEW_RELIC_DISTRIBUTED_TRACING_ENABLED", String.valueOf(true),
                "NEW_RELIC_DISTRIBUTED_TRACING_SAMPLER_REMOTE_PARENT_SAMPLED_TRACE_ID_RATIO_BASED_RATIO", String.valueOf(0.09)
        ));

        // set system property config
        Properties props = new Properties();
        props.setProperty("newrelic.config.distributed_tracing.enabled", String.valueOf(true));
        props.setProperty("newrelic.config.distributed_tracing.sampler.root", "always_off");
        props.setProperty("newrelic.config.distributed_tracing.sampler.remote_parent_sampled.trace_id_ratio_based.ratio", String.valueOf(0.08));

        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new SaveSystemPropertyProviderRule.TestSystemProps(props),
                environmentFacade
        ));

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

        // set local config props
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

        // Then
        SamplerConfig rootSamplerConfig = distributedTracingConfig.getRootSamplerConfig();
        assertEquals(SamplerConfig.ROOT, rootSamplerConfig.getSampler());
        // Should use the always_off sampling type, local config overridden by system property
        assertEquals(SamplerConfig.ALWAYS_OFF, rootSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(rootSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentSampledSamplerConfig = distributedTracingConfig.getRemoteParentSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_SAMPLED, remoteParentSampledSamplerConfig.getSampler());
        // Should use the trace_id_ratio_based sampling type
        assertEquals(SamplerConfig.TRACE_ID_RATIO_BASED, remoteParentSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        // local config and system property are both overridden by environment variable
        assertEquals(Float.valueOf(0.09F), remoteParentSampledSamplerConfig.getSamplerRatio());

        SamplerConfig remoteParentNotSampledSamplerConfig = distributedTracingConfig.getRemoteParentNotSampledSamplerConfig();
        assertEquals(SamplerConfig.REMOTE_PARENT_NOT_SAMPLED, remoteParentNotSampledSamplerConfig.getSampler());
        // Should use the always_off sampling type
        assertEquals(SamplerConfig.ALWAYS_OFF, remoteParentNotSampledSamplerConfig.getSamplerType());
        assertEquals(9, rootSamplerConfig.getAdaptiveSamplingTarget());
        Assert.assertNull(remoteParentNotSampledSamplerConfig.getSamplerRatio());
    }
}