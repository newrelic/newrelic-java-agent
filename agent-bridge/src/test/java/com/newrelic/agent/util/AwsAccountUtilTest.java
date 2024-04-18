/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class AwsAccountUtilTest {

    @Test
    public void decodeAccount() {
        Long accountId = AwsAccountUtil.get().decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
        assertEquals(999999999999L, accountId.longValue());

        accountId = AwsAccountUtil.get().decodeAccount("FKKYQAAAAAAAZZZZZZZZ");
        assertEquals(1L, accountId.longValue());
    }

}