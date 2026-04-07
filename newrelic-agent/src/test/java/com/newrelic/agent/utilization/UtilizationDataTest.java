/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.MockCoreService;
import com.newrelic.agent.utilization.AWS.AwsData;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class UtilizationDataTest {
    private final String invalidBootId = null;
    private final String validBootId = "969cbd19-1f02-49d3-98c1-6d6f2f5cb552";
    private final CloudData awsCloudData = AwsData.EMPTY_DATA;

    @BeforeClass
    public static void before() throws Exception {
        MockCoreService.getMockAgentAndBootstrapTheServiceManager();
    }

    @Test
    public void testUtilizationDataVersion() {
        UtilizationConfig utilConfig = new UtilizationConfig(null, null, null);
        UtilizationData data = new UtilizationData(null, null, null, 0, null, null, invalidBootId, awsCloudData, 0L, utilConfig,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertTrue(map.containsKey("metadata_version"));
        Assert.assertEquals(5, map.get("metadata_version"));
    }

    @Test
    public void testUtilizationKeysInvalidBootId() {
        UtilizationData data = new UtilizationData(null, null, null, 0, null, null, invalidBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertTrue(map.containsKey("metadata_version"));
        Assert.assertTrue(map.containsKey("logical_processors"));
        Assert.assertTrue(map.containsKey("total_ram_mib"));
        Assert.assertTrue(map.containsKey("hostname"));
        Assert.assertFalse(map.containsKey("boot_id"));
    }

    @Test
    public void testUtilizationKeysValidBootId() {
        UtilizationData data = new UtilizationData(null, null, null, 0, null, null, validBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertTrue(map.containsKey("metadata_version"));
        Assert.assertTrue(map.containsKey("logical_processors"));
        Assert.assertTrue(map.containsKey("total_ram_mib"));
        Assert.assertTrue(map.containsKey("hostname"));
        Assert.assertTrue(map.containsKey("boot_id"));
    }

    @Test
    public void testUtilizationKeysValidEntityIdentity() {
        UtilizationData data = new UtilizationData("newrelic", "newrelic.com", new ArrayList<>(Arrays.asList("1.2.3.4")),
                0, null, null,  validBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA, KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertTrue(map.containsKey("metadata_version"));
        Assert.assertTrue(map.containsKey("logical_processors"));
        Assert.assertTrue(map.containsKey("total_ram_mib"));
        Assert.assertTrue(map.containsKey("hostname"));
        Assert.assertTrue(map.containsKey("full_hostname"));
        Assert.assertTrue(map.containsKey("ip_address"));
        Assert.assertTrue(map.containsKey("boot_id"));
    }

    @Test
    public void testUtilizationKeysInValidEntityIdentity() {
        UtilizationData data = new UtilizationData("newrelic", "newrelic", new ArrayList<>(Arrays.asList("1.2.3.4")),
                0, null, null, validBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA, KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertTrue(map.containsKey("hostname"));
        Assert.assertFalse(map.containsKey("full_hostname"));
        Assert.assertTrue(map.containsKey("ip_address"));
    }

    @Test
    public void testNoData() {
        UtilizationData data = new UtilizationData(null, null, null, 0, null, null, invalidBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Assert.assertNull(map.get("total_ram_mib"));
        Assert.assertNull(map.get("logical_processors"));
    }

    @Test
    public void map_producesCorrectJson_withEcsFargateContainerId() {
        UtilizationData data = new UtilizationData(null, null, null, 0, null, "ecs1234567890", invalidBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Map<String, Object> vendorMap = (Map<String, Object>) map.get("vendors");
        Map<String, Object> ecsMap = (Map<String, Object>) vendorMap.get("ecs");
        String id = (String) ecsMap.get("ecsDockerId");

        Assert.assertEquals("ecs1234567890", id);
    }

    @Test
    public void map_producesCorrectJson_withBothDockerIdandEcsFargateContainerId() {
        UtilizationData data = new UtilizationData(null, null, null, 0, "dockerid09876", "ecs1234567890", invalidBootId, awsCloudData, 0L, UtilizationConfig.EMPTY_DATA,
                KubernetesData.EMPTY_KUBERNETES_DATA);
        Map<String, Object> map = data.map();
        Map<String, Object> vendorMap = (Map<String, Object>) map.get("vendors");
        Map<String, Object> ecsMap = (Map<String, Object>) vendorMap.get("ecs");
        String ecsId = (String) ecsMap.get("ecsDockerId");

        Map<String, Object> dockerMap = (Map<String, Object>) vendorMap.get("docker");
        String dockerId = (String) dockerMap.get("id");

        Assert.assertEquals("ecs1234567890", ecsId);
        Assert.assertEquals("dockerid09876", dockerId);
    }
}
