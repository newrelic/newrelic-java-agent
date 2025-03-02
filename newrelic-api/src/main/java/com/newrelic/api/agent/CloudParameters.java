/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

/**
 * Use to report a cloud service that do not match HTTP nor messaging.
 *
 * @since 8.14.0
 */
public class CloudParameters implements ExternalParameters {

    private final String platform;

    private final String resourceId;

    private CloudParameters(Builder builder) {
        this.platform = builder.platform;
        this.resourceId = builder.resourceId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getResourceId() {
        return resourceId;
    }

    /**
     * This method starts the process of creating a CloudParameters object.
     * @param provider The cloud platform being invoked. E.g. aws_lambda, azure_function, gcp_cloud_run...
     * @since 8.14.0
     */
    public static ResourceIdParameter provider(String provider) {
        return new Builder(provider);
    }

    private static class Builder implements ResourceIdParameter, Build {
        private String platform;
        private String resourceId;

        private Builder(String platform) {
            this.platform = platform;
        }

        public Build resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public CloudParameters build() {
            return new CloudParameters(this);
        }
    }

    public interface ResourceIdParameter extends Build {
        /**
         * @param resourceId the cloud provider unique identifier for the service instance. This should be an ARN for AWS,
         * a fully qualified resource ID on Azure or a full resource name on GCP.
         * @return the object that can be used to build the CloudParameters
         *
         * @since 8.14.0
         */
        Build resourceId(String resourceId);
    }

    public interface Build {
        /**
         * Builds the CloudParameters object.
         * @return the CloudParameters object with the specified parameters.
         *
         * @since 8.14.0
         */
        CloudParameters build();
    }

}
