/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import org.junit.Test;

import static com.newrelic.api.agent.CloudAccountInfo.AWS_ACCOUNT_ID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudAccountInfoCacheTest {

    @Test
    public void accountInfo() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();

        assertNull(cache.getAccountInfo(AWS_ACCOUNT_ID));

        String accountId = "123456789";
        cache.setAccountInfo(AWS_ACCOUNT_ID, accountId);

        assertEquals(accountId, cache.getAccountInfo(AWS_ACCOUNT_ID));
    }

    @Test
    public void accountInfoClient() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        Object sdkClient = new Object();

        assertNull(cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));

        String accountId = "123456789";
        cache.setAccountInfo(sdkClient, AWS_ACCOUNT_ID, accountId);

        assertEquals(accountId, cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));

        Object anotherSdkClient = new Object();
        assertNull(cache.getAccountInfo(anotherSdkClient, AWS_ACCOUNT_ID));
    }

    @Test
    public void accountInfoClientFallback() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        String accountId = "123456789";
        cache.setAccountInfo(AWS_ACCOUNT_ID, accountId);

        Object sdkClient = new Object();
        assertEquals(accountId, cache.getAccountInfo(sdkClient, AWS_ACCOUNT_ID));
    }


    @Test
    public void retrieveDataFromConfigAccountInfo() {
        CloudAccountInfoCache cache = new CloudAccountInfoCache();
        String accountId = "123456789";

        ServiceManager serviceManager = mock(ServiceManager.class, RETURNS_DEEP_STUBS);
        ServiceFactory.setServiceManager(serviceManager);
        when(serviceManager.getConfigService().getDefaultAgentConfig().getValue("cloud.aws.account_id"))
                .thenReturn(accountId);
        cache.retrieveDataFromConfig();

        assertEquals(accountId, cache.getAccountInfo(AWS_ACCOUNT_ID));
    }
}