/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport.apache;

import com.newrelic.agent.transport.HostConnectException;
import org.apache.http.HttpHost;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * This test exists because setting `cause` on an IOException is rather clumsy
 * and I want to make sure it works correctly.
 */
public class HostConnectExceptionTest {
    @Test
    public void ensureCauseIsSet() {
        HttpHostConnectException innerException = new HttpHostConnectException(
                new IOException("really inner cause"),
                HttpHost.create("https://nothing.example.com")
        );

        HostConnectException target = new HostConnectException(innerException.toString(), innerException);

        assertSame(innerException, target.getCause());
    }

}