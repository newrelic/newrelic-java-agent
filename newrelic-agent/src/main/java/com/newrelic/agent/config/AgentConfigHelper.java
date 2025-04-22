/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.newrelic.agent.Agent;
import com.newrelic.agent.errors.ExceptionHandlerSignature;
import com.newrelic.agent.instrumentation.methodmatchers.InvalidMethodDescriptor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class AgentConfigHelper {

    public static final String NEWRELIC_ENVIRONMENT_SYSTEM_PROP = "newrelic.environment";
    public static final String NEWRELIC_ENVIRONMENT_ENV_VAR = "NEW_RELIC_ENVIRONMENT";
    private static final String JAVA_ENVIRONMENT = "JAVA_ENV";
    private static final String PRODUCTION_ENVIRONMENT = "production";

    private static final AtomicBoolean loggedDeprecationWarning = new AtomicBoolean(false);

    public static Map<String, Object> getConfigurationFileSettings(File configFile) throws Exception {
        try (InputStream is = new FileInputStream(configFile)) {
            return parseConfiguration(is);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseConfiguration(InputStream is) throws Exception {
        String env = getEnvironment();
        try {
            Map<String, Object> allConfig = createYaml().load(new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
            if (allConfig == null) {
                Agent.LOG.info("The configuration file is empty");
                return Collections.emptyMap();
            }
            Map<String, Object> props = (Map<String, Object>) allConfig.get(env);
            if (props == null) {
                props = (Map<String, Object>) allConfig.get("common");
            }
            if (props == null) {
                throw new Exception(MessageFormat.format("Unable to find configuration named {0}", env));
            }
            return props;
        } catch (Exception e) {
            Agent.LOG.log(Level.SEVERE, MessageFormat.format("Unable to parse configuration file. Please validate the yaml: {0}", e.toString()), e);
            throw e;
        }
    }

    private static String getEnvironment() {
        try {
            String oldEnvVar = System.getenv(JAVA_ENVIRONMENT);
            if (oldEnvVar != null && !loggedDeprecationWarning.get()) {
                Agent.LOG.info("The JAVA_ENV environment variable will be deprecated. Use NEW_RELIC_ENVIRONMENT instead.");
                loggedDeprecationWarning.set(true);
            }

            String env = System.getProperty(NEWRELIC_ENVIRONMENT_SYSTEM_PROP);
            env = env == null ? oldEnvVar : env;
            env = env == null ? System.getenv(NEWRELIC_ENVIRONMENT_ENV_VAR) : env;
            return env == null ? PRODUCTION_ENVIRONMENT : env;
        } catch (Throwable t) {
            return PRODUCTION_ENVIRONMENT;
        }
    }

    private static Yaml createYaml() {
        SafeConstructor constructor = new ExtensionConstructor(new LoaderOptions());
        return new Yaml(constructor);
    }

    private static class ExtensionConstructor extends SafeConstructor {
        public ExtensionConstructor(LoaderOptions loaderOptions) {
            super(loaderOptions);
            yamlConstructors.put(new Tag("!exception_handler"), new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    List<?> args = constructSequence((SequenceNode) node);
                    try {
                        return new ExceptionHandlerSignature((String) args.get(0), (String) args.get(1), (String) args.get(2));
                    } catch (InvalidMethodDescriptor e) {
                        return e;
                    }
                }
            });
            yamlConstructors.put(new Tag("!obscured"), new AbstractConstruct() {
                @Override
                public Object construct(Node node) {
                    String value = constructScalar((ScalarNode) node);
                    return new ObscuredYamlPropertyWrapper(value);
                }
            });
        }
    }

}
