/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.config.util;

import com.newrelic.agent.config.EnvironmentFacade;
import com.newrelic.agent.config.SystemPropertyFactory;
import com.newrelic.agent.config.SystemPropertyProvider;
import com.newrelic.agent.config.SystemProps;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class SaveSystemPropertyProviderRule implements TestRule {
    public void mockSingleProperty(String key, boolean value) {
        mockSingleProperty(key, String.valueOf(value));
    }

    public void mockSingleProperty(String key, String propertyValue) {
        Properties properties = new Properties();
        properties.put(key, propertyValue);
        SystemPropertyFactory.setSystemPropertyProvider(new SystemPropertyProvider(
                new TestSystemProps(properties),
                new TestEnvironmentFacade()
        ));
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SystemPropertyProvider previousProvider = SystemPropertyFactory.getSystemPropertyProvider();
                try {
                    base.evaluate();
                } finally {
                    SystemPropertyFactory.setSystemPropertyProvider(previousProvider);
                }
            }
        };
    }

    public static class TestSystemProps extends SystemProps {
        private final Properties systemProperties;

        public TestSystemProps() {
            this(new Properties());
        }

        public TestSystemProps(Properties sys) {
            systemProperties = sys;
        }

        @Override
        public String getSystemProperty(String prop) {
            return systemProperties.getProperty(prop);
        }

        @Override
        public Properties getAllSystemProperties() {
            return systemProperties;
        }
    }

    public static class TestEnvironmentFacade extends EnvironmentFacade {
        private final Map<String, String> envProperties;

        public TestEnvironmentFacade() {
            this(Collections.<String, String>emptyMap());
        }

        public TestEnvironmentFacade(Map<String, String> envProperties) {
            this.envProperties = envProperties;
        }

        @Override
        public String getenv(String key) {
            return envProperties.get(key);
        }

        @Override
        public Map<String, String> getAllEnvProperties() {
            return envProperties;
        }
    }
}
