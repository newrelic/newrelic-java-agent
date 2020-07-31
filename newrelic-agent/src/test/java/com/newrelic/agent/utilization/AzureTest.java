/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.utilization.Azure.AzureData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link Azure} cloud vendor class.
 */
public class AzureTest {

    @Test
    public void testAzureValueMap() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"location\": \"CentralUS\","
                + "\"name\": \"IMDSCanary\","
                + "\"vmId\": \"5c08b38e-4d57-4c23-ac45-aca61037f084\","
                + "\"vmSize\": \"Standard_DS2\""
                + "}");

        AzureData azureData = azure.getData();
        assertFalse(azureData.getValueMap().isEmpty());
    }

    @Test
    public void crossAgentTestTimesOut() throws IOException {
        // "testname": "azure api times out, no vendor hash or supportability metric reported"
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn(null);
        Azure azure = new Azure(mockCloud);

        AzureData azureData = azure.getData();
        assertEquals(AzureData.EMPTY_DATA, azureData);

        verify(mockCloud, never()).recordError(anyString());
    }

    @Test
    public void crossAgentTest() throws IOException {
        // "testname": "vmId, location, name, vmSize are all happy"
        CloudUtility mockCloud = mock(CloudUtility.class);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"location\": \"CentralUS\","
                + "\"name\": \"IMDSCanary\","
                + "\"vmId\": \"5c08b38e-4d57-4c23-ac45-aca61037f084\","
                + "\"vmSize\": \"Standard_DS2\""
                + "}");

        AzureData azureData = azure.getData();
        assertEquals("Standard_DS2", azureData.getVmSize());
        assertEquals("5c08b38e-4d57-4c23-ac45-aca61037f084", azureData.getVmId());
        assertEquals("IMDSCanary", azureData.getName());
        assertEquals("CentralUS", azureData.getLocation());
    }

    @Test
    public void nullTest() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn(null);

        AzureData azureData = azure.getData();
        assertEquals(azureData, AzureData.EMPTY_DATA);
    }

    @Test
    public void anyFieldInvalidYieldsErrorMetricAndEmptyMap() throws IOException {
        // "testname": "location with invalid characters"
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue("<script>lol</script>")).thenReturn(true);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"location\" : \"<script>lol</script>\","
                + "\"name\" : \"IMDSCanary\","
                + "\"vmId\" : \"5c08b38e-4d57-4c23-ac45-aca61037f084\","
                + "\"vmSize\" : \"Standard_DS2\","
                + "}");

        AzureData azureData = azure.getData();
        assertEquals(AzureData.EMPTY_DATA, azureData);

        verify(mockCloud, times(1)).recordError("Supportability/utilization/azure/error");
    }

    @Test
    public void crossAgentTestUtfCodepoints() throws IOException {
        // "testname": "UTF-8 high codepoints"
        CloudUtility mockCloud = mock(CloudUtility.class);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"vmSize\" : \"Standard_DS2\","
                + "\"location\" : \"CentralUS\","
                + "\"name\" : \"IMDSCanary\","
                + "\"vmId\" : \"滈 橀槶澉 鞻饙騴 鱙鷭黂 甗糲 紁羑 嗂 蛶觢豥 餤駰鬳 釂鱞鸄\","
                + "}");

        AzureData azureData = azure.getData();
        assertEquals("Standard_DS2", azureData.getVmSize());
        assertEquals("滈 橀槶澉 鞻饙騴 鱙鷭黂 甗糲 紁羑 嗂 蛶觢豥 餤駰鬳 釂鱞鸄", azureData.getVmId());
        assertEquals("IMDSCanary", azureData.getName());
        assertEquals("CentralUS", azureData.getLocation());
    }

    @Test
    public void returnsEmptyDataAndErrorOnOtherException() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        Azure azure = new Azure(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenThrow(new RuntimeException());

        AzureData azureData = azure.getData();
        assertEquals(AzureData.EMPTY_DATA, azureData);

        verify(mockCloud, times(1)).httpGet(anyString(), anyInt(), anyString());
        verify(mockCloud).recordError("Supportability/utilization/azure/error");
        verifyNoMoreInteractions(mockCloud);
    }
}
