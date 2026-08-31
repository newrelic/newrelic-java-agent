/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.sling;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;

import java.util.logging.Level;

public class SlingUtils {
    public static final String HTML_MIME_TYPE = "text/html";
    public static final String BROWSER_MONITORING_CONFIG_KEY = "browser_monitoring.auto_instrument";
    public static final String PRINT_WRITER_CLASS_NAME = "com.nr.agent.instrumentation.sling.NewRelicPrintWriterWrapper";
    public static final String OUTPUT_STREAM_CLASS_NAME = "com.nr.agent.instrumentation.sling.NewRelicOutputStreamWrapper";

    public static void logTransactionDetails() {
        if (AgentBridge.getAgent().getLogger().isLoggable(Level.FINEST)) {
            Transaction txn = AgentBridge.getAgent().getTransaction(false);
            if (txn != null) {
                AgentBridge.getAgent().getLogger().log(Level.FINEST,
                        "Browser monitoring script injected into Sling page, txn={0}, name={1}",
                        txn.toString(), txn.getTransactionName());
            }
        }
    }
}
