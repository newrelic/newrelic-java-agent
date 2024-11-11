/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AwsAccountDecoderImplTest {

    @Test
    public void decodeAccount() {
        AwsAccountDecoder decoder = AwsAccountDecoderImpl.newInstance();
        String accountId = decoder.decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
        assertEquals("999999999999", accountId);

        accountId = decoder.decodeAccount("FKKYQAAAAAAAZZZZZZZZ");
        assertEquals("1", accountId);
    }

}