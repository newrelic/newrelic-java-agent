/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.api.agent.CloudAccountInfo;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CloudAccountInfoValidatorTest {

    @Test
    public void testValidateAwsAccountId() {
        assertFalse(CloudAccountInfoValidator.validate(CloudAccountInfo.AWS_ACCOUNT_ID, null));

        // accountId is not 12 digits
        assertFalse(CloudAccountInfoValidator.validate(CloudAccountInfo.AWS_ACCOUNT_ID, "12345678901"));

        // accountId is not a number
        assertFalse(CloudAccountInfoValidator.validate(CloudAccountInfo.AWS_ACCOUNT_ID, "12345678901a"));

        // happy path
        assertTrue(CloudAccountInfoValidator.validate(CloudAccountInfo.AWS_ACCOUNT_ID, "123456789012"));
    }

}