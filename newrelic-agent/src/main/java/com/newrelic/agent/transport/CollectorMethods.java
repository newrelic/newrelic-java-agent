/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

public class CollectorMethods {
    public static final String CONNECT = "connect";
    public static final String METRIC_DATA = "metric_data";
    public static final String GET_AGENT_COMMANDS = "get_agent_commands";
    public static final String AGENT_COMMAND_RESULTS = "agent_command_results";
    public static final String PRECONNECT = "preconnect";
    public static final String ERROR_DATA = "error_data";
    public static final String PROFILE_DATA = "profile_data";
    public static final String ERROR_EVENT_DATA = "error_event_data";
    public static final String ANALYTIC_EVENT_DATA = "analytic_event_data";
    public static final String SPAN_EVENT_DATA = "span_event_data";
    public static final String CUSTOM_EVENT_DATA = "custom_event_data";
    public static final String LOG_EVENT_DATA = "log_event_data";
    public static final String UPDATE_LOADED_MODULES = "update_loaded_modules";
    public static final String SHUTDOWN = "shutdown";
    public static final String SQL_TRACE_DATA = "sql_trace_data";
    public static final String TRANSACTION_SAMPLE_DATA = "transaction_sample_data";
    public static final String DIMENSIONAL_METRIC_DATA = "dimensional_metric_data";

    private CollectorMethods() {
    }
}
