/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.aws;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AwsAccountDecoderImplTest {

    @Test
    public void decodeAccount() {
        AwsAccountDecoder decoder = AwsAccountDecoderImpl.newInstance();
        Long accountId = decoder.decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
        assertEquals(999999999999L, accountId.longValue());

        accountId = decoder.decodeAccount("FKKYQAAAAAAAZZZZZZZZ");
        assertEquals(1L, accountId.longValue());
    }

}