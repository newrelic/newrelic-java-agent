/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.test.marker.RequiresFork;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests that if decoding is disabled, AwsUtil will return null.
 */
@Category(RequiresFork.class)
public class AwsUtilDecoderDisabledTest {


    @Test
    public void decodeAccount() {
        try (MockedStatic<NewRelic> newRelicMockedStatic = Mockito.mockStatic(NewRelic.class, Answers.RETURNS_DEEP_STUBS)) {
            newRelicMockedStatic.when(() -> NewRelic.getAgent().getConfig().getValue(eq("cloud.aws.account_decoding.enabled"), any()))
                    .thenReturn(false);

            AwsAccountDecoder decoder = AwsAccountDecoderImpl.newInstance();
            String accountId = decoder.decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
            assertNull(accountId);
        }
    }
}