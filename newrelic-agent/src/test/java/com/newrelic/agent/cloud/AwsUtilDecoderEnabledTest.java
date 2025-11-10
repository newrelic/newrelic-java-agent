/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests that AwsUtil will actually decode the account from an access key in case there is no configuration.
 */
public class AwsUtilDecoderEnabledTest {

    @Test
    public void decodeAccount() {
        String accountId = AwsAccountDecoderImpl.newInstance().decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
        assertEquals("999999999999", accountId);
    }
}