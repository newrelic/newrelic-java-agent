/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.BrowserMonitoringConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.Obfuscator;
import org.json.simple.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BrowserFooter {
    public BrowserFooter() {

    }

    public String getFooter(BrowserTransactionState state) {
        return "";
    }

    String getFooter(BrowserTransactionState state, String nonce) {
        return "";
    }
}
