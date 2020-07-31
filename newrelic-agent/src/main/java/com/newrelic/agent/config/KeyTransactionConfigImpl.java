/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class KeyTransactionConfigImpl implements KeyTransactionConfig {

    private final Map<String, Long> apdexTs;
    private final long apdexTInMillis;

    private KeyTransactionConfigImpl(Map<String, Object> props, long apdexTInMillis) {
        this.apdexTInMillis = apdexTInMillis;
        Map<String, Long> apdexTs = new HashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            Object apdexT = entry.getValue();
            if (apdexT instanceof Number) {
                Long apdexTinMillis = (long) (((Number) apdexT).doubleValue() * 1000);
                String txName = entry.getKey();
                apdexTs.put(txName, apdexTinMillis);
            }
        }
        this.apdexTs = Collections.unmodifiableMap(apdexTs);
    }

    @Override
    public boolean isApdexTSet(String transactionName) {
        return apdexTs.containsKey(transactionName);
    }

    @Override
    public long getApdexTInMillis(String transactionName) {
        Long apdexT = apdexTs.get(transactionName);
        if (apdexT == null) {
            return apdexTInMillis;
        }
        return apdexT;
    }

    static KeyTransactionConfig createKeyTransactionConfig(Map<String, Object> settings, long apdexTInMillis) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new KeyTransactionConfigImpl(settings, apdexTInMillis);
    }

}
