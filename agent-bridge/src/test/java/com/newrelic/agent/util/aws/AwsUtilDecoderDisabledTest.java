/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util.aws;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.test.marker.RequiresFork;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests that if decoding is disabled, AwsUtil will return null.
 */
@Category(RequiresFork.class)
public class AwsUtilDecoderDisabledTest {

    private static MockedStatic<NewRelic> newRelicMockedStatic;

    @BeforeClass
    public static void beforeClass() {
        newRelicMockedStatic = Mockito.mockStatic(NewRelic.class, Answers.RETURNS_DEEP_STUBS);
        newRelicMockedStatic.when(() -> NewRelic.getAgent().getConfig().getValue(eq("aws.decode_account"), any()))
                .thenReturn(false);
    }

    @AfterClass
    public static void afterClass() {
        newRelicMockedStatic.close();
    }

    @Test
    public void decodeAccount() {
        Long accountId = AwsUtil.decodeAccount("FKKY6RVFFB77ZZZZZZZZ");
        assertNull(accountId);
    }
}