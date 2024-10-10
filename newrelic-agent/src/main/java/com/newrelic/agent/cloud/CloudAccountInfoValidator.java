/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.cloud;

import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.NewRelic;

import java.util.logging.Level;
import java.util.regex.Pattern;

public class CloudAccountInfoValidator {

    private static final Pattern AWS_ACCOUNT_ID_PATTERN = Pattern.compile("^\\d+$");
    private static Level awsAccountIdLogLevel = Level.WARNING;

    public static boolean validate(CloudAccountInfo cloudAccountInfo, String value) {
        switch (cloudAccountInfo) {
            case AWS_ACCOUNT_ID:
                return validateAwsAccountId(value);
            default:
                return false;
        }
    }

    private static boolean validateAwsAccountId(String accountId) {
        final int AWS_ACCOUNT_ID_LENGTH = 12;
        if (accountId == null) {
            return false;
        }
        boolean valid = accountId.length() == AWS_ACCOUNT_ID_LENGTH &&
                AWS_ACCOUNT_ID_PATTERN.matcher(accountId).matches();
        if (!valid) {
            NewRelic.getAgent().getLogger().log(awsAccountIdLogLevel, "AWS account ID should be a 12-digit number.");
            awsAccountIdLogLevel = Level.FINEST;
        }
        return valid;
    }

    private CloudAccountInfoValidator() {
        // prevents instantiation
    }
}
