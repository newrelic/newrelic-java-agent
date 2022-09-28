/*
 *
 *  * Copyright 2022 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.jms3.integration;

/**
 * Per-provider tests implement this interface, typically by extending BaseJmsProviderTest.
 * The reusable provider-independent test class also implements this interface.
 */
public interface JmsProviderTest {

    /**
     * Test that our instrumentation works when JMS is used in a nontransactional, nonpersistent
     * mode with automatic acknowledge.
     *
     * @throws Exception trouble
     */
    void testEchoServer() throws Exception;

}
