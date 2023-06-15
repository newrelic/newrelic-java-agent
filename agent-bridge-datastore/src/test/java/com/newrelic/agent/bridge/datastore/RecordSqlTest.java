/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge.datastore;

import org.junit.Assert;
import org.junit.Test;

public class RecordSqlTest {

    @Test
    public void raw() {
        Assert.assertSame(RecordSql.raw, RecordSql.get("raw"));
    }

    @Test
    public void obfuscated() {
        Assert.assertSame(RecordSql.obfuscated, RecordSql.get("obfuscated"));
    }

    @Test
    public void off() {
        Assert.assertSame(RecordSql.off, RecordSql.get("off"));
    }
}
