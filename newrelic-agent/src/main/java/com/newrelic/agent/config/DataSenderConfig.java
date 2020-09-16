/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

public interface DataSenderConfig {
    /**
     * Returns the collector host.
     */
    String getHost();

    int getPort();

    String getInsertApiKey();

    boolean isAuditMode();

    String getProxyHost();

    Integer getProxyPort();

    String getProxyScheme();

    String getProxyPassword();

    String getProxyUser();

    String getCaBundlePath();

    /**
     * If simple compression is enabled we will prevent data within a payload from being compressed. However,
     * the payload itself may still be compressed before being sent to the collector.
     *
     * If this is on, it effectively means that the internal pieces of a raw payload will not be compressed.
     *
     * @return true if simple compression is enabled, false (default) otherwise
     */
    boolean isSimpleCompression();

    /**
     * Returns the content-encoding for compressed data.  Defaults to deflate.
     *
     */
    String getCompressedContentEncoding();

    /**
     * If this is enabled, the agent will send data to the collector via a PUT command rather than the default POST.
     *
     * Note: This is not a normal configuration option and should not be used or documented unless absolutely necessary.
     * See JAVA-2208 for more information.
     *
     * @return true if PUT should be used for data sent to the collector, POST will be used otherwise
     */
    boolean isPutForDataSend();

    AuditModeConfig getAuditModeConfig();

    /**
     * Get the license key.
     */
    String getLicenseKey();

    int getTimeoutInMilliseconds();
}
