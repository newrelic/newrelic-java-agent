/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.utilization;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class AzureAppService implements CloudVendor {
    static String PROVIDER = "azureappservice";
    private final CloudUtility cloudUtility;

    public AzureAppService(CloudUtility cloudUtility) {
        this.cloudUtility = cloudUtility;
    }

    private static final String CLOUD_RESOURCE_ID_KEY = "cloud.resource_id"; // despite the dot, 'cloud' does not need to be its own node

    @Override
    public AzureAppServiceData getData() {
        String resourceGroup = System.getenv("WEBSITE_RESOURCE_GROUP");
        String siteName = System.getenv("WEBSITE_SITE_NAME");
        String ownerName = System.getenv("WEBSITE_OWNER_NAME");

        return parseEnvVars(resourceGroup, siteName, ownerName);
    }

    protected AzureAppServiceData parseEnvVars(String resourceGroup, String siteName, String ownerName) {
        try {
            String subscriptionId = ownerName == null ? null : ownerName.split("\\+")[0];

            if ("".equals(resourceGroup) || cloudUtility.isInvalidValue(resourceGroup)
                || "".equals(siteName) || cloudUtility.isInvalidValue(siteName)
                || "".equals(subscriptionId) || cloudUtility.isInvalidValue(subscriptionId)) {
                recordAzureError();
                Agent.LOG.log(Level.WARNING, "Failed to validate Azure App Services value");
                return AzureAppServiceData.EMPTY_DATA;
            }

            AzureAppServiceData data = new AzureAppServiceData(
                    "/subscriptions/" + subscriptionId + "/resourceGroups/" + resourceGroup + "/providers/Microsoft.Web/sites/" + siteName);
            Agent.LOG.log(Level.FINEST, "Found AzureData {0}", data);
            return data;
        } catch (Exception e) {
            return AzureAppServiceData.EMPTY_DATA;
        }
    }

    private void recordAzureError() {
        cloudUtility.recordError(MetricNames.SUPPORTABILITY_AZURE_ERROR);
    }

    protected static class AzureAppServiceData implements CloudData {
        private final String cloudResourceId;

        static final AzureAppServiceData EMPTY_DATA = new AzureAppServiceData();

        private AzureAppServiceData() {
            cloudResourceId = null;
        }

        protected AzureAppServiceData(String cloudResourceId) {
            this.cloudResourceId = cloudResourceId;
        }

        public String getCloudResourceId() { return cloudResourceId; }

        @Override
        public Map<String, String> getValueMap() {
            Map<String, String> azure = new HashMap<>();

            if (cloudResourceId != null) {
                azure.put(CLOUD_RESOURCE_ID_KEY, cloudResourceId);
            }

            return azure;
        }

        @Override
        public String getProvider() {
            return PROVIDER;
        }

        @Override
        public boolean isEmpty() {
            return this == EMPTY_DATA;
        }

        @Override
        public String toString() {
            return "AzureAppServiceData{" +
                    "cloud.resource_id='" + cloudResourceId + "'" +
                    '}';
        }
    }

}
