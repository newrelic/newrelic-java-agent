/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.tracing.samplers;

import com.newrelic.agent.DistributedTracingTestUtil.DTConfigMapBuilder;
import com.newrelic.agent.MockServiceManager;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.config.ConfigServiceFactory;
import com.newrelic.agent.config.DistributedTracingConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracing.Granularity;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.newrelic.agent.tracing.DistributedTraceServiceImpl.SamplerCase;
import static org.junit.Assert.*;

public class SamplerManagerTest {
    private static MockServiceManager serviceManager;

    @Before
    public void setupServiceManager(){
        Map<String, Object> config = new HashMap<>();
        config.put(AgentConfigImpl.APP_NAME, "Test");

        ConfigService configService = ConfigServiceFactory.createConfigService(
                AgentConfigImpl.createAgentConfig(config),
                Collections.<String, Object>emptyMap());

        serviceManager = new MockServiceManager(configService);
    }

    @Test
    public void testSamplerManagerBuildsSamplersOfAllTypes(){
        //Create one of each sampler type and verify that the samplerManager picked it up.
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("root", "bleh") //invalid! This should revert to using the global adaptive sampler
                .withSamplerSetting("remote_parent_sampled", "adaptive", "sampling_target",  20)
                .withSamplerSetting("remote_parent_not_sampled", "always_on")
                .withPartialGranularitySetting("root", "always_off")
                .withPartialGranularitySetting("remote_parent_sampled", "trace_id_ratio_based", "ratio", .12)
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());

        Sampler fullRoot = manager.getSampler(Granularity.FULL, SamplerCase.ROOT);
        assertEquals(SamplerType.ADAPTIVE, fullRoot.getType());
        assertTrue(((AdaptiveSampler) fullRoot).isShared());

        Sampler fullRemoteParentSampled = manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED);
        assertEquals(SamplerType.ADAPTIVE, fullRemoteParentSampled.getType());
        assertEquals(20, ((AdaptiveSampler) fullRemoteParentSampled).getTarget());
        assertFalse(((AdaptiveSampler) fullRemoteParentSampled).isShared());

        Sampler fullRemoteParentNotSampled = manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED);
        assertEquals(SamplerType.ALWAYS_ON, fullRemoteParentNotSampled.getType());

        Sampler partialRoot = manager.getSampler(Granularity.PARTIAL, SamplerCase.ROOT);
        assertEquals(SamplerType.ALWAYS_OFF, partialRoot.getType());

        Sampler partialRemoteParentSampled = manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED);
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, partialRemoteParentSampled.getType());
    }

    @Test
    public void testSamplerManagerSetsUpDefaultSamplers() {
        DistributedTracingConfig defaultConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig();
        SamplerManager manager = new SamplerManager(defaultConfig);

        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.FULL, SamplerCase.ROOT).getType());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.PARTIAL, SamplerCase.ROOT).getType());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        //the samplers should all be the shared instance
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.FULL, SamplerCase.ROOT)).isShared());
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED)).isShared());
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED)).isShared());
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.PARTIAL, SamplerCase.ROOT)).isShared());
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED)).isShared());
        assertTrue(((AdaptiveSampler) manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED)).isShared());

        //the sampling target should be the default, 120
        assertEquals(120, ((AdaptiveSampler) manager.getSampler(Granularity.FULL, SamplerCase.ROOT)).getTarget());
    }

    @Test
    public void testSamplerManagerSetsUpConfiguredSamplersForDefaultApp() {
        /*
        distributed_tracing:
          sampler:
            remote_parent_not_sampled: always_off
            root:
              trace_id_ratio_based:
                ratio: 0.6
            remote_parent_sampled: always_on
            partial_granularity:
              enabled: true
              type: reduced
              root:
                adaptive:
                  sampling_target: 15
              remote_parent_sampled: (adaptive)
              remote_parent_not_sampled:
                trace_id_ratio_based:
                  ratio: 0.1
         */

        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.6)
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withPartialGranularitySetting("enabled", "true")
                .withPartialGranularitySetting("type", "reduced")
                .withPartialGranularitySetting("root", "adaptive", "sampling_target", 15)
                .withPartialGranularitySetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.1)
                .buildMainConfig();

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());

        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(Granularity.FULL, SamplerCase.ROOT).getType());
        assertEquals(SamplerType.ALWAYS_ON, manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.ALWAYS_OFF, manager.getSampler(Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.PARTIAL, SamplerCase.ROOT).getType());
        assertEquals(15, ((AdaptiveSampler) manager.getSampler(Granularity.PARTIAL, SamplerCase.ROOT)).getTarget());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());
    }

    @Test
    public void testSamplerManagerSetsUpConfiguredSamplersForMultipleApps() {
        /*
        distributed_tracing:
          sampler:
            remote_parent_not_sampled: always_off
            root:
              trace_id_ratio_based:
                ratio: 0.6
            remote_parent_sampled: always_on
            partial_granularity:
              enabled: true
              type: reduced
              root:
                adaptive:
                  sampling_target: 15
              remote_parent_sampled: (adaptive)
              remote_parent_not_sampled:
                trace_id_ratio_based:
                  ratio: 0.1
         */

        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("remote_parent_not_sampled", "always_off")
                .withSamplerSetting("root", "trace_id_ratio_based", "ratio", 0.6)
                .withSamplerSetting("remote_parent_sampled", "always_on")
                .withPartialGranularitySetting("enabled", "true")
                .withPartialGranularitySetting("type", "reduced")
                .withPartialGranularitySetting("root", "adaptive", "sampling_target", 15)
                .withPartialGranularitySetting("remote_parent_not_sampled", "trace_id_ratio_based", "ratio", 0.1)
                .buildMainConfig();

        config.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, true);
        config.put("app_name", "test");

        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());


        String app1 = "app_one";
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(app1, Granularity.FULL, SamplerCase.ROOT).getType());
        assertEquals(SamplerType.ALWAYS_ON, manager.getSampler(app1, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.ALWAYS_OFF, manager.getSampler(app1, Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(app1, Granularity.PARTIAL, SamplerCase.ROOT).getType());
        assertEquals(15, ((AdaptiveSampler) manager.getSampler(app1, Granularity.PARTIAL, SamplerCase.ROOT)).getTarget());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(app1, Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(app1, Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        String app2 = "app_two";
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(app2, Granularity.FULL, SamplerCase.ROOT).getType());
        assertEquals(SamplerType.ALWAYS_ON, manager.getSampler(app2, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.ALWAYS_OFF, manager.getSampler(app2, Granularity.FULL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());

        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(app2, Granularity.PARTIAL, SamplerCase.ROOT).getType());
        assertEquals(15, ((AdaptiveSampler) manager.getSampler(app2, Granularity.PARTIAL, SamplerCase.ROOT)).getTarget());
        assertEquals(SamplerType.ADAPTIVE, manager.getSampler(app2, Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_SAMPLED).getType());
        assertEquals(SamplerType.TRACE_ID_RATIO_BASED, manager.getSampler(app2, Granularity.PARTIAL, SamplerCase.REMOTE_PARENT_NOT_SAMPLED).getType());
    }

    @Test
    public void autoAppNamingEnabledCreatesAppAwareAdaptiveSamplers() {
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("root", "adaptive", "sampling_target", 15)
                .withPartialGranularitySetting("enabled", "true")
                .buildMainConfig();

        config.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);

        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());

        String app1 = "app_one";
        String app2 = "app_two";

        //first check instance-level adaptive samplers. These should be different for different apps.
        Sampler app1FullRootSampler = manager.getSampler(app1, Granularity.FULL, SamplerCase.ROOT);
        Sampler app2FullRootSampler = manager.getSampler(app2, Granularity.FULL, SamplerCase.ROOT);
        assertNotSame("Different apps should have different adaptive sampler instances", app1FullRootSampler, app2FullRootSampler);

        //next check remote_parent_sampled and partial_granularity root samplers. These should both be the global adaptive sampler.
        Sampler app1FullRemoteParentSampledSampler = manager.getSampler(app1, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED);
        Sampler app1PartialRootSampler = manager.getSampler(app1, Granularity.PARTIAL, SamplerCase.ROOT);
        Sampler app2FullRemoteParentSampledSampler = manager.getSampler(app2, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED);
        Sampler app2PartialRootSampler = manager.getSampler(app2, Granularity.PARTIAL, SamplerCase.ROOT);

        //the global samplers should be the same within one app...
        assertSame("Each app should have a single global adaptive sampler", app1FullRemoteParentSampledSampler, app1PartialRootSampler);
        assertSame("Each app should have a single global adaptive sampler", app2FullRemoteParentSampledSampler, app2PartialRootSampler);
        //...but should be different for different apps.
        assertNotSame("Different apps should have different global adaptive samplers", app1FullRemoteParentSampledSampler, app2FullRemoteParentSampledSampler);
    }

    @Test
    public void autoAppNamingDisabledDoesNotCreateAppAwareSamplers(){
        //This test checks a behavior that should not happen in the wild: different app names coming in with auto app naming disabled.
        //If auto app naming is disabled, every transaction should have the same app name attached to it.
        //HOWEVER, it is still good to verify that this is guaranteed to work.
        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("root", "adaptive", "sampling_target", 15)
                .withPartialGranularitySetting("enabled", "true")
                .buildMainConfig();

        //auto app naming is disabled by default.
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);

        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());

        String app1 = "app_one";
        String app2 = "app_two";

        //first check instance-level adaptive samplers. These should be the same since auto app naming is off.
        Sampler app1FullRootSampler = manager.getSampler(app1, Granularity.FULL, SamplerCase.ROOT);
        Sampler app2FullRootSampler = manager.getSampler(app2, Granularity.FULL, SamplerCase.ROOT);
        assertSame("When auto app naming is disabled, sampler instances should not be app-aware", app1FullRootSampler, app2FullRootSampler);

        //next check remote_parent_sampled samplers. These should both be the global adaptive sampler.
        Sampler app1FullRemoteParentSampledSampler = manager.getSampler(app1, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED);
        Sampler app2FullRemoteParentSampledSampler = manager.getSampler(app2, Granularity.FULL, SamplerCase.REMOTE_PARENT_SAMPLED);

        //the global samplers should be the same too.
        assertSame("Different app names should have the same global adaptive sampler when auto app naming is disabled",
                app1FullRemoteParentSampledSampler,
                app2FullRemoteParentSampledSampler);
    }

    @Test
    public void setSharedSamplingTargetsUpdatesAllSharedAdaptiveSamplers(){
        int INITIAL_SAMPLING_TARGET = 70;

        Map<String, Object> config = new DTConfigMapBuilder()
                .withSamplerSetting("adaptive_sampling_target", INITIAL_SAMPLING_TARGET)
                .buildMainConfig();

        config.put(AgentConfigImpl.ENABLE_AUTO_APP_NAMING, true);
        AgentConfig agentConfig = AgentConfigImpl.createAgentConfig(config);
        ConfigService configService = ConfigServiceFactory.createConfigService(agentConfig, Collections.<String, Object>emptyMap());
        serviceManager.setConfigService(configService);
        SamplerManager manager = new SamplerManager(serviceManager.getConfigService().getDefaultAgentConfig().getDistributedTracingConfig());

        Sampler defaultSharedSampler = manager.getSampler(Granularity.FULL, SamplerCase.ROOT);
        List<String> apps = Arrays.asList("app_one", "app_two", "app_three", "app_four");
        List<Sampler> sharedSamplers = apps.stream()
                .map(appName -> manager.getSampler(appName, Granularity.FULL, SamplerCase.ROOT))
                .collect(Collectors.toList());

        //Check that the samplers all start out with the initially configured sampling target...
        assertEquals(INITIAL_SAMPLING_TARGET, ((AdaptiveSampler) defaultSharedSampler).getTarget());
        for (Sampler sampler : sharedSamplers) {
            assertEquals(INITIAL_SAMPLING_TARGET, ((AdaptiveSampler) sampler).getTarget());
        }

        int UPDATED_SAMPLING_TARGET = 55;
        manager.setSharedSamplingTargets(UPDATED_SAMPLING_TARGET);

        //...are correctly updated with the new sampling target...
        assertEquals(UPDATED_SAMPLING_TARGET, ((AdaptiveSampler) defaultSharedSampler).getTarget());
        for (Sampler sampler : sharedSamplers) {
            assertEquals(UPDATED_SAMPLING_TARGET, ((AdaptiveSampler) sampler).getTarget());
        }

        //...and are created thereafter with the new sampling target.
        String newlyCreatedApp = "app_five";
        Sampler newlyCreatedSampler = manager.getSampler(newlyCreatedApp, Granularity.FULL, SamplerCase.ROOT);
        assertEquals(UPDATED_SAMPLING_TARGET, ((AdaptiveSampler) newlyCreatedSampler).getTarget());
    }
}