/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static com.newrelic.agent.utilization.AzureAppService.AzureAppServiceData;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the {@link AzureAppService} cloud vendor class.
 */
public class AzureAppServiceTest {

    @Test
    public void testNullValues() {
        AzureAppService azureAppService = new AzureAppService(new CloudUtility());

        AzureAppServiceData azureAppServiceData = azureAppService.parseEnvVars(null, null, null);
        assertTrue(azureAppServiceData.isEmpty());

        azureAppServiceData = azureAppService.parseEnvVars(null, "siteName", "ownerName");
        assertTrue(azureAppServiceData.isEmpty());

        azureAppServiceData = azureAppService.parseEnvVars("resourceGroup", null, "ownerName");
        assertTrue(azureAppServiceData.isEmpty());

        azureAppServiceData = azureAppService.parseEnvVars("resourceGroup", "siteName", null);
        assertTrue(azureAppServiceData.isEmpty());
    }

    @Test
    public void testValidValues() {
        AzureAppService azureAppService = new AzureAppService(new CloudUtility());

        AzureAppServiceData azureAppServiceData = azureAppService.parseEnvVars("resourceGroup", "siteName", "subscriptionId+afterPlus");
        assertEquals(azureAppServiceData.getCloudResourceId(), "/subscriptions/subscriptionId/resourceGroups/resourceGroup/providers/Microsoft.Web/sites/siteName");
        assertEquals(azureAppServiceData.getProvider(), "azureappservice");
        assertTrue(azureAppServiceData.getValueMap().containsKey("cloud.resource_id"));
    }

    @Test
    public void testGetDataReturnsEmptyWhenNotInAzureAppService() {
        AzureAppService azureAppService = new AzureAppService(new CloudUtility());

        AzureAppServiceData azureAppServiceData = azureAppService.getData();
        assertTrue(azureAppServiceData.isEmpty());
    }
}
