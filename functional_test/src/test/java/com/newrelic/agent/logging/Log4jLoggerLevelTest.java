/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import java.util.logging.Level;

import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.Agent;
import com.newrelic.agent.service.ServiceManager;

public class Log4jLoggerLevelTest {

    @Test
    public void testWarningLevel() {
        AgentLogManager.setLogLevel("warning");

        // test root logger
        Assert.assertFalse(Agent.LOG.isDebugEnabled());
        Assert.assertFalse(Agent.LOG.isFineEnabled());
        Assert.assertFalse(Agent.LOG.isFinerEnabled());
        Assert.assertFalse(Agent.LOG.isFinestEnabled());

        Assert.assertTrue(Agent.LOG.isLoggable(Level.SEVERE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.WARNING));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.INFO));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINE));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINER));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINEST));

        // test child logger
        IAgentLogger logger = Agent.LOG.getChildLogger(ServiceManager.class);
        Assert.assertFalse(logger.isDebugEnabled());
        Assert.assertFalse(logger.isFineEnabled());
        Assert.assertFalse(logger.isFinerEnabled());
        Assert.assertFalse(logger.isFinestEnabled());

        Assert.assertTrue(logger.isLoggable(Level.SEVERE));
        Assert.assertTrue(logger.isLoggable(Level.WARNING));
        Assert.assertFalse(logger.isLoggable(Level.INFO));
        Assert.assertFalse(logger.isLoggable(Level.FINE));
        Assert.assertFalse(logger.isLoggable(Level.FINER));
        Assert.assertFalse(logger.isLoggable(Level.FINEST));

    }

    @Test
    public void testInfoLevel() {
        AgentLogManager.setLogLevel("info");

        // test root logger
        Assert.assertFalse(Agent.LOG.isDebugEnabled());
        Assert.assertFalse(Agent.LOG.isFineEnabled());
        Assert.assertFalse(Agent.LOG.isFinerEnabled());
        Assert.assertFalse(Agent.LOG.isFinestEnabled());

        Assert.assertTrue(Agent.LOG.isLoggable(Level.SEVERE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.WARNING));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.INFO));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINE));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINER));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINEST));

        // test child logger
        IAgentLogger logger = Agent.LOG.getChildLogger(ServiceManager.class);
        Assert.assertFalse(logger.isDebugEnabled());
        Assert.assertFalse(logger.isFineEnabled());
        Assert.assertFalse(logger.isFinerEnabled());
        Assert.assertFalse(logger.isFinestEnabled());

        Assert.assertTrue(logger.isLoggable(Level.SEVERE));
        Assert.assertTrue(logger.isLoggable(Level.WARNING));
        Assert.assertTrue(logger.isLoggable(Level.INFO));
        Assert.assertFalse(logger.isLoggable(Level.FINE));
        Assert.assertFalse(logger.isLoggable(Level.FINER));
        Assert.assertFalse(logger.isLoggable(Level.FINEST));

    }

    @Test
    public void testFineLevel() {
        AgentLogManager.setLogLevel("fine");

        // test root logger
        Assert.assertTrue(Agent.LOG.isDebugEnabled());
        Assert.assertTrue(Agent.LOG.isFineEnabled());
        Assert.assertFalse(Agent.LOG.isFinerEnabled());
        Assert.assertFalse(Agent.LOG.isFinestEnabled());

        Assert.assertTrue(Agent.LOG.isLoggable(Level.SEVERE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.WARNING));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.INFO));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINE));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINER));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINEST));

        // test child logger
        IAgentLogger logger = Agent.LOG.getChildLogger(ServiceManager.class);
        Assert.assertTrue(logger.isDebugEnabled());
        Assert.assertTrue(logger.isFineEnabled());
        Assert.assertFalse(logger.isFinerEnabled());
        Assert.assertFalse(logger.isFinestEnabled());

        Assert.assertTrue(logger.isLoggable(Level.SEVERE));
        Assert.assertTrue(logger.isLoggable(Level.WARNING));
        Assert.assertTrue(logger.isLoggable(Level.INFO));
        Assert.assertTrue(logger.isLoggable(Level.FINE));
        Assert.assertFalse(logger.isLoggable(Level.FINER));
        Assert.assertFalse(logger.isLoggable(Level.FINEST));

    }

    @Test
    public void testFinerLevel() {
        AgentLogManager.setLogLevel("finer");

        // test root logger
        Assert.assertTrue(Agent.LOG.isDebugEnabled());
        Assert.assertTrue(Agent.LOG.isFineEnabled());
        Assert.assertTrue(Agent.LOG.isFinerEnabled());
        Assert.assertFalse(Agent.LOG.isFinestEnabled());

        Assert.assertTrue(Agent.LOG.isLoggable(Level.SEVERE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.WARNING));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.INFO));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINER));
        Assert.assertFalse(Agent.LOG.isLoggable(Level.FINEST));

        // test child logger
        IAgentLogger logger = Agent.LOG.getChildLogger(ServiceManager.class);
        Assert.assertTrue(logger.isDebugEnabled());
        Assert.assertTrue(logger.isFineEnabled());
        Assert.assertTrue(logger.isFinerEnabled());
        Assert.assertFalse(logger.isFinestEnabled());

        Assert.assertTrue(logger.isLoggable(Level.SEVERE));
        Assert.assertTrue(logger.isLoggable(Level.WARNING));
        Assert.assertTrue(logger.isLoggable(Level.INFO));
        Assert.assertTrue(logger.isLoggable(Level.FINE));
        Assert.assertTrue(logger.isLoggable(Level.FINER));
        Assert.assertFalse(logger.isLoggable(Level.FINEST));
    }

    @Test
    public void testFinestLevel() {
        AgentLogManager.setLogLevel("finest");

        // test root logger
        Assert.assertTrue(Agent.LOG.isDebugEnabled());
        Assert.assertTrue(Agent.LOG.isFineEnabled());
        Assert.assertTrue(Agent.LOG.isFinerEnabled());
        Assert.assertTrue(Agent.LOG.isFinestEnabled());

        Assert.assertTrue(Agent.LOG.isLoggable(Level.SEVERE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.WARNING));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.INFO));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINE));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINER));
        Assert.assertTrue(Agent.LOG.isLoggable(Level.FINEST));

        // test child logger
        IAgentLogger logger = Agent.LOG.getChildLogger(ServiceManager.class);
        Assert.assertTrue(logger.isDebugEnabled());
        Assert.assertTrue(logger.isFineEnabled());
        Assert.assertTrue(logger.isFinerEnabled());
        Assert.assertTrue(logger.isFinestEnabled());

        Assert.assertTrue(logger.isLoggable(Level.SEVERE));
        Assert.assertTrue(logger.isLoggable(Level.WARNING));
        Assert.assertTrue(logger.isLoggable(Level.INFO));
        Assert.assertTrue(logger.isLoggable(Level.FINE));
        Assert.assertTrue(logger.isLoggable(Level.FINER));
        Assert.assertTrue(logger.isLoggable(Level.FINEST));
    }

    @Test
    public void shouldNotThrowWithNulls() {
        AgentLogManager.setLogLevel("finest");

        Agent.LOG.log(Level.INFO, null);
        Agent.LOG.log(Level.INFO, null, (Throwable)null);
        Agent.LOG.log(Level.INFO, null, (Object[])null, null);
        Agent.LOG.log(Level.INFO, null, (Object)null);
        Agent.LOG.log(Level.INFO, new Exception(), null);
    }
}
