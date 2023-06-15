/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.nr.agent.instrumentation.target;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.Map;

public class NoopSourceTranformation implements Transformation<SourceRecord> {
    @Override
    public SourceRecord apply(SourceRecord record) {
        return record;
    }

    @Override
    public ConfigDef config() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> configs) {

    }
}
