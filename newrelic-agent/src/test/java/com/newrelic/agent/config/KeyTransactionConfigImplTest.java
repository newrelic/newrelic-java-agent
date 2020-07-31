/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class KeyTransactionConfigImplTest {

    @Test
    public void noTransactionNames() throws Exception {
        Map<String, Object> localSettings = new HashMap<>();
        KeyTransactionConfig config = KeyTransactionConfigImpl.createKeyTransactionConfig(localSettings, 500L);

        Assert.assertEquals(500L, config.getApdexTInMillis("WebTransaction/uri/ru/betting/Motorsport"));
    }

    @Test
    public void transactionNames() throws Exception {
        Map<String, Object> serverSettings = new HashMap<>();
        serverSettings.put("WebTransaction/uri/de/betting/Chess", Float.valueOf("1.0"));
        serverSettings.put("WebTransaction/uri/fr/betting/Cycling", Float.valueOf("2.0"));
        KeyTransactionConfig config = KeyTransactionConfigImpl.createKeyTransactionConfig(serverSettings, 500L);

        Assert.assertEquals(500L, config.getApdexTInMillis("WebTransaction/uri/ru/betting/Motorsport"));
        Assert.assertEquals(1000L, config.getApdexTInMillis("WebTransaction/uri/de/betting/Chess"));
        Assert.assertEquals(2000L, config.getApdexTInMillis("WebTransaction/uri/fr/betting/Cycling"));
    }

}
