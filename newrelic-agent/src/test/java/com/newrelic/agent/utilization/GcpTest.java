/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.utilization.GCP.GcpData;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link GCP} cloud vendor class.
 */
public class GcpTest {

    @Test
    public void testGcpValueMap() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        GCP gcp = new GCP(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"id\":3161347020215157000,"
                + "\"machineType\":\"projects/492690098729/machineTypes/custom-1-1024\","
                + "\"name\" : \"aef-default-20170501t160547-7gh8\","
                + "\"zone\" : \"projects/492690098729/zones/us-central1-c\","
                + "}");

        GcpData gcpData = gcp.getData();
        assertFalse(gcpData.getValueMap().isEmpty());

        verify(mockCloud, never()).recordError("Supportability/utilization/gcp/error");
    }

    @Test
    public void crossAgentTestTimesOut() throws IOException {
        // "testname": "gcp api times out, no vendor hash or supportability metric reported"

        CloudUtility mockCloud = mock(CloudUtility.class);
        GCP gcp = new GCP(mockCloud);
        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn(null);

        GcpData gcpData = gcp.getData();
        assertEquals(GcpData.EMPTY_DATA, gcpData);

        verify(mockCloud, never()).recordError("Supportability/utilization/gcp/error");
    }

    @Test
    public void crossAgentTest() throws IOException {
        // "testname": "machine type, id, zone, name are all happy"
        CloudUtility mockCloud = mock(CloudUtility.class);
        GCP gcp = new GCP(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"id\" : 3161347020215157000"
                + "\"machineType\":\"projects/492690098729/machineTypes/custom-1-1024\","
                + "\"name\" : \"aef-default-20170501t160547-7gh8\","
                + "\"zone\" : \"projects/492690098729/zones/us-central1-c\","
                + "}");

        GcpData gcpData = gcp.getData();
        assertEquals("3161347020215157000", gcpData.getId());
        assertEquals("custom-1-1024", gcpData.getMachineType());
        assertEquals("aef-default-20170501t160547-7gh8", gcpData.getName());
        assertEquals("us-central1-c", gcpData.getZone());
    }

    @Test
    public void returnsEmptyDataIfAnyValueInvalid() throws IOException {
        // "testname": "id with invalid characters"
        CloudUtility mockCloud = mock(CloudUtility.class);
        when(mockCloud.isInvalidValue("<script>lol</script>")).thenReturn(true);
        GCP gcp = new GCP(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenReturn("{"
                + "\"id\":3161347020215157000,"
                + "\"machineType\":\"projects/492690098729/machineTypes/custom-1-1024\","
                + "\"name\" : \"<script>lol</script>\","
                + "\"zone\" : \"projects/492690098729/zones/us-central1-c\","
                + "}");

        GcpData gcpData = gcp.getData();
        assertEquals(GcpData.EMPTY_DATA, gcpData);

        verify(mockCloud, times(1)).recordError("Supportability/utilization/gcp/error");
    }

    @Test
    public void returnsEmptyDataAndErrorOnOtherException() throws IOException {
        CloudUtility mockCloud = mock(CloudUtility.class);
        GCP gcp = new GCP(mockCloud);

        when(mockCloud.httpGet(anyString(), anyInt(), anyString())).thenThrow(new RuntimeException());

        GcpData gcpData = gcp.getData();
        assertEquals(GcpData.EMPTY_DATA, gcpData);

        verify(mockCloud, times(1)).httpGet(anyString(), anyInt(), anyString());
        verify(mockCloud).recordError("Supportability/utilization/gcp/error");
        verifyNoMoreInteractions(mockCloud);
    }
}
