/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */
package com.newrelic.agent.bridge.logging;

import com.newrelic.agent.bridge.logging.AppLoggingUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class AppLoggingUtilsTest {

    @Test
    public void testUrlEncoding() {
        final String ENCODED_PIPE = "%7C";
        final String ENCODED_SPACE = "+";
        // The main goal of the encoding is to eliminate | characters from the entity.name as | is used as
        // the BLOB_DELIMITER for separating the agent metadata attributes that are appended to log files
        final String valueToEncode = "|My Application|";
        final String expectedEncodedValue = ENCODED_PIPE + "My" + ENCODED_SPACE + "Application" + ENCODED_PIPE;

        String encodedValue = AppLoggingUtils.urlEncode(valueToEncode);

        Assert.assertEquals(expectedEncodedValue, encodedValue);
    }
}