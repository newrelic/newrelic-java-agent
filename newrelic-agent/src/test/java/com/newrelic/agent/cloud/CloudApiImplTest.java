/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.agent.bridge.CloudApi;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.NewRelic;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class CloudApiImplTest {

    private CloudApi cloudApi;
    private CloudAccountInfoCache cache;

    @Before
    public void setup() {
        cache = mock(CloudAccountInfoCache.class);
        AwsAccountDecoder decoder = mock(AwsAccountDecoder.class);
        cloudApi = new CloudApiImpl(cache, decoder);
    }

    @Test
    public void setAccountInfo() {
        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {

            String accountId = "123456789012";
            cloudApi.setAccountInfo(CloudAccountInfo.AWS_ACCOUNT_ID, accountId);

            newRelic.verify(() -> NewRelic.incrementCounter("Supportability/API/Cloud/SetAccountInfo/AWS_ACCOUNT_ID/API"));
            newRelic.verifyNoMoreInteractions();

            verify(cache).setAccountInfo(eq(CloudAccountInfo.AWS_ACCOUNT_ID), eq(accountId));
            verifyNoMoreInteractions(cache);
        }
    }

    @Test
    public void setAccountInfoClient() {
        CloudAccountInfoCache cache = mock(CloudAccountInfoCache.class);
        CloudApiImpl cloudApi = new CloudApiImpl(cache, AwsAccountDecoderImpl.newInstance());

        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {

            String accountId = "123456789012";
            Object sdkClient = new Object();
            cloudApi.setAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID, accountId);

            newRelic.verify(() -> NewRelic.incrementCounter("Supportability/API/Cloud/SetAccountInfoClient/AWS_ACCOUNT_ID/API"));
            newRelic.verifyNoMoreInteractions();

            verify(cache).setAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID), eq(accountId));
            verifyNoMoreInteractions(cache);
        }
    }

    @Test
    public void getAccountInfo() {
        CloudAccountInfoCache cache = mock(CloudAccountInfoCache.class);
        CloudApiImpl cloudApi = new CloudApiImpl(cache, AwsAccountDecoderImpl.newInstance());

        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {

            cloudApi.getAccountInfo(CloudAccountInfo.AWS_ACCOUNT_ID);

            newRelic.verifyNoInteractions();

            verify(cache).getAccountInfo(eq(CloudAccountInfo.AWS_ACCOUNT_ID));
            verifyNoMoreInteractions(cache);
        }
    }

    @Test
    public void getAccountInfoClient() {
        CloudAccountInfoCache cache = mock(CloudAccountInfoCache.class);
        CloudApiImpl cloudApi = new CloudApiImpl(cache, AwsAccountDecoderImpl.newInstance());

        try (MockedStatic<NewRelic> newRelic = mockStatic(NewRelic.class)) {

            Object sdkClient = new Object();
            cloudApi.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);

            newRelic.verifyNoInteractions();

            verify(cache).getAccountInfo(eq(sdkClient), eq(CloudAccountInfo.AWS_ACCOUNT_ID));
            verifyNoMoreInteractions(cache);
        }
    }
}