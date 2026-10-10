/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Map;

/**
 * Configuration for exporting agent telemetry over OTLP/HTTP with protobuf encoding.
 */
public interface OtlpExportConfig {

    /**
     * True if OTLP export is enabled, else false. Disabled by default and always disabled in serverless mode.
     */
    boolean isEnabled();

    /**
     * True if log events should be sent via OTLP instead of the collector.
     */
    boolean isLogsEnabled();

    /**
     * Full URL that log events are sent to.
     */
    String getLogsEndpoint();

    /**
     * Additional headers to send with log event requests.
     */
    Map<String, String> getLogsHeaders();
}
