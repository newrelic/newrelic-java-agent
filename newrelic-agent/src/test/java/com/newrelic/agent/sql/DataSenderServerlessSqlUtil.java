/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.sql;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.SqlTraceConfig;
import com.newrelic.agent.tracers.Tracer;

// Designed so that
public class DataSenderServerlessSqlUtil {
    public static SlowQueryInfo SlowQueryInfoWrapper(TransactionData td, Tracer tracer, String rawQuery, String obfuscatedQuery, SqlTraceConfig sqlTraceConfig) {
        return new SlowQueryInfo(td, tracer, "select * from person", "select ? from ?", sqlTraceConfig);
    }
}
