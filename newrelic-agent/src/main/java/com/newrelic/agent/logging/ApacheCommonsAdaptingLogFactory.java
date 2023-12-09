/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.logging;

import com.newrelic.agent.Agent;
import com.newrelic.agent.util.LicenseKeyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;

import java.util.logging.Level;

import static com.newrelic.agent.util.LicenseKeyUtil.*;

public class ApacheCommonsAdaptingLogFactory extends LogFactory {

    public static final IAgentLogger LOG = AgentLogManager.getLogger();

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public String[] getAttributeNames() {
        return new String[0];
    }

    @Override
    public Log getInstance(@SuppressWarnings("rawtypes") Class clazz) throws LogConfigurationException {
        return new LogAdapter(clazz, Agent.LOG);
    }

    @Override
    public Log getInstance(String name) throws LogConfigurationException {
        return new LogAdapter(name, Agent.LOG);
    }

    @Override
    public void release() {
        return;
    }

    @Override
    public void removeAttribute(String name) {
        return;
    }

    @Override
    public void setAttribute(String name, Object value) {
        return;
    }

    private class LogAdapter implements Log {

        private final IAgentLogger logger;

        public LogAdapter(Class<?> clazz, IAgentLogger logger) {
            this.logger = logger.getChildLogger(clazz);
        }

        public LogAdapter(String name, IAgentLogger logger) {
            this.logger = logger.getChildLogger(name);
        }

        @Override
        public boolean isDebugEnabled() {
            return Agent.isDebugEnabled() && logger.isDebugEnabled();
        }

        @Override
        public boolean isErrorEnabled() {
            return isDebugEnabled() && logger.isLoggable(Level.SEVERE);
        }

        @Override
        public boolean isFatalEnabled() {
            return isDebugEnabled() && logger.isLoggable(Level.SEVERE);
        }

        @Override
        public boolean isInfoEnabled() {
            return isDebugEnabled() && logger.isLoggable(Level.INFO);
        }

        @Override
        public boolean isTraceEnabled() {
            return isDebugEnabled() && logger.isLoggable(Level.FINEST);
        }

        @Override
        public boolean isWarnEnabled() {
            return isDebugEnabled() && logger.isLoggable(Level.WARNING);
        }

        @Override
        public void trace(Object message) {
            if (isDebugEnabled()) {
                logger.trace(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void trace(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.FINEST, t, obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void debug(Object message) {
            if (isDebugEnabled()) {
                logger.debug(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void debug(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.FINEST, "{0} : {1}", obfuscateLicenseKey(message.toString()), t);
            }
        }

        @Override
        public void info(Object message) {
            if (isDebugEnabled()) {
                logger.info(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void info(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.INFO, "{0} : {1}", obfuscateLicenseKey(message.toString()), t);
            }
        }

        @Override
        public void warn(Object message) {
            if (isDebugEnabled()) {
                logger.warning(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void warn(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.WARNING, "{0} : {1}", obfuscateLicenseKey(message.toString()), t);
            }
        }

        @Override
        public void error(Object message) {
            if (isDebugEnabled()) {
                logger.error(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void error(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.SEVERE, "{0} : {1}", obfuscateLicenseKey(message.toString()), t);
            }
        }

        @Override
        public void fatal(Object message) {
            if (isDebugEnabled()) {
                logger.severe(obfuscateLicenseKey(message.toString()));
            }
        }

        @Override
        public void fatal(Object message, Throwable t) {
            if (isDebugEnabled()) {
                logger.log(Level.SEVERE, "{0} : {1}", obfuscateLicenseKey(message.toString()), t);
            }
        }
    }

}
