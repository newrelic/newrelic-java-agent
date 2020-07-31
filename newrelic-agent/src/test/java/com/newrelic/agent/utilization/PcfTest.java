/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.utilization.PCF.PcfData;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link PCF} cloud vendor class.
 */
public class PcfTest {

    private static final String VALID_CF_INSTANCE_GUID = "fd326c0e-847e-47a1-65cc-45f6";
    private static final String VALID_CF_INSTANCE_IP = "10.10.149.48";
    private static final String VALID_MEMORY_LIMIT = "1024m";
    private static final String VALID_UTF_8_CODEPOINTS = "滈 橀槶澉 鞻饙騴 鱙鷭黂 甗糲 紁羑 嗂 蛶觢豥 餤駰鬳 釂鱞鸄";

    private static final String INVALID_VALUE = "<script>lol</script>";

    @Test
    public void testGcpValueMap() {
        PCF pcf = Mockito.spy(new PCF(new CloudUtility()));

        Mockito.doReturn(VALID_CF_INSTANCE_GUID).doReturn(VALID_CF_INSTANCE_IP).doReturn(VALID_MEMORY_LIMIT)
                .when(pcf).getPcfValue(anyString());

        PcfData pcfData = pcf.getData();
        assertFalse(pcfData.getValueMap().isEmpty());
    }

    @Test
    public void crossAgentTest() {
        // "testname": "cf_instance_guid, cf_instance_ip, memory_limit are all happy"
        PCF pcf = Mockito.spy(new PCF(new CloudUtility()));

        Mockito.doReturn(VALID_CF_INSTANCE_GUID).doReturn(VALID_CF_INSTANCE_IP).doReturn(VALID_MEMORY_LIMIT)
                .when(pcf).getPcfValue(anyString());

        PcfData pcfData = pcf.getData();
        assertNotEquals(PcfData.EMPTY_DATA, pcfData);
        assertEquals(VALID_CF_INSTANCE_GUID, pcfData.getInstanceGuid());
        assertEquals(VALID_CF_INSTANCE_IP, pcfData.getInstanceIp());
        assertEquals(VALID_MEMORY_LIMIT, pcfData.getMemoryLimit());
    }

    @Test
    public void anyInvalidCharactersReturnEmptyMapAndMetric() {
        // "testname": "cf_instance_guid with invalid characters"
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue(INVALID_VALUE)).thenReturn(true);
        PCF pcf = Mockito.spy(new PCF(mockCloud));

        Mockito.doReturn(INVALID_VALUE).doReturn(VALID_CF_INSTANCE_IP).doReturn(VALID_MEMORY_LIMIT)
                .when(pcf).getPcfValue(anyString());

        PcfData pcfData = pcf.getData();
        assertEquals(PcfData.EMPTY_DATA, pcfData);

        verify(mockCloud, times(1)).recordError("Supportability/utilization/pcf/error");
    }

    @Test
    public void crossAgentTestUtfCodepoints() {
        // "testname": "UTF-8 high codepoints"
        PCF pcf = Mockito.spy(new PCF(new CloudUtility()));

        Mockito.doReturn(VALID_UTF_8_CODEPOINTS).doReturn(VALID_CF_INSTANCE_IP).doReturn(VALID_MEMORY_LIMIT)
                .when(pcf).getPcfValue(anyString());

        PcfData pcfData = pcf.getData();
        assertNotEquals(PcfData.EMPTY_DATA, pcfData);
        assertEquals(VALID_UTF_8_CODEPOINTS, pcfData.getInstanceGuid());
        assertEquals(VALID_CF_INSTANCE_IP, pcfData.getInstanceIp());
        assertEquals(VALID_MEMORY_LIMIT, pcfData.getMemoryLimit());
    }

}
