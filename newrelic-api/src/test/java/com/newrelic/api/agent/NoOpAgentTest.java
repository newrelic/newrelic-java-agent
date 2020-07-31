/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import org.junit.Test;

import java.util.logging.Level;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * This test covers what happens when users call our APIs when running without the agent.
 */
public class NoOpAgentTest {

    @Test
    public void testNoOpPublicApiTransactionDefaults() {

        assertNotNull(NewRelic.getAgent().getTransaction());
        checkTransactionDefaults(NewRelic.getAgent().getTransaction());
    }

    @Test
    public void testNoOpTracedMethod()  {
        assertNotNull(NewRelic.getAgent().getTracedMethod().getMetricName());
        assertNotNull(NewRelic.getAgent().getTracedMethod().getMetricName());
    }

    @Test
    public void testNoOpConfig()  {
        assertNull(NewRelic.getAgent().getConfig().getValue("anyKey"));
        assertEquals("defaultValue", NewRelic.getAgent().getConfig().getValue("someKey", "defaultValue"));
    }

    @Test
    public void testNoOpLogger()  {
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.OFF));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.WARNING));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.SEVERE));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.ALL));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.INFO));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.FINER));
        assertFalse(NewRelic.getAgent().getLogger().isLoggable(Level.FINEST));
    }



    public void checkTransactionDefaults(Transaction transaction) {
        assertFalse(transaction.setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "", ""));
        assertFalse(transaction.isTransactionNameSet());
        assertFalse(transaction.markResponseSent());

        assertNotNull(transaction.getTracedMethod());

        assertNotNull(transaction.getToken());
        assertFalse(transaction.getToken().link());
        assertFalse(transaction.getToken().expire());
        assertFalse(transaction.getToken().linkAndExpire());
        assertFalse(transaction.getToken().isActive());

        assertNull(transaction.getRequestMetadata());
        assertNull(transaction.getResponseMetadata());

    }
}

