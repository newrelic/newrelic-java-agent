/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.bridge.AgentBridge;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public final class ErrorCollectorConfigImpl extends BaseConfig implements ErrorCollectorConfig {

    public static final String ENABLED = "enabled";
    public static final String COLLECT_ERRORS = "collect_errors";
    public static final String IGNORE_ERRORS = "ignore_errors";
    public static final String IGNORE_CLASSES = "ignore_classes";
    public static final String IGNORE_MESSAGES = "ignore_messages";
    public static final String IGNORE_CLASS_NAME = "class_name";
    public static final String IGNORE_MESSAGE = "message";
    public static final String IGNORE_STATUS_CODES = "ignore_status_codes";
    public static final String IGNORE_ERROR_PRIORITY = "ignoreErrorPriority";
    public static final String EXCEPTION_HANDLERS = "exception_handlers";

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_COLLECT_ERRORS = true;
    public static final Set<Integer> DEFAULT_IGNORE_STATUS_CODES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(404))); // ignore 404 by default

    public static final String COLLECT_EVENTS = "collect_error_events"; // from collector
    public static final boolean DEFAULT_COLLECT_EVENTS = true;
    public static final String CAPTURE_EVENTS = "capture_events"; // local/server config
    public static final boolean DEFAULT_CAPTURE_EVENTS = true;
    public static final String MAX_EVENT_SAMPLES_STORED = "max_event_samples_stored"; // local/server config
    public static final int DEFAULT_MAX_EVENT_SAMPLES_STORED = 100;
    private static final boolean DEFAULT_IGNORE_ERROR_PRIORITY = true;
    private static final Object DEFAULT_EXCEPTION_HANDLERS = Collections.emptyList();

    // Expected errors
    public static final String EXPECTED_CLASSES = "expected_classes";
    public static final String EXPECTED_MESSAGES = "expected_messages";
    public static final String CLASS_NAME = "class_name";
    public static final String MESSAGE = "message";
    public static final String EXPECTED_STATUS_CODES = "expected_status_codes";

    /*
     * This error is ignored because the class PromiseActorRef throws the ActorKilledExcepton as a control mechanism in
     * normal flow to stop the Actor. See source code below:
     * https://github.com/akka/akka/blob/v2.1.0/akka-actor/src/main/scala/akka/pattern/AskSupport.scala#L311
     */
    public static final Set<String> DEFAULT_IGNORE_ERRORS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("akka.actor.ActorKilledException")));
    public static final String SYSTEM_PROPERTY_ROOT = "newrelic.config.error_collector.";

    private final boolean isEnabled;
    private Set<IgnoreErrorConfig> ignoreErrors;
    private Set<Integer> ignoreStatusCodes;
    private Set<ExpectedErrorConfig> expectedErrors;
    private Set<Integer> expectedStatusCodes;
    private final boolean isEventsEnabled;
    private final int maxEventsStored;
    private final boolean ignoreErrorPriority;
    private final Object exceptionHandlers;

    private ErrorCollectorConfigImpl(Map<String, Object> props) {
        super(props, SYSTEM_PROPERTY_ROOT);
        isEnabled = initEnabled();
        ignoreErrors = initIgnoreErrors();
        ignoreStatusCodes = initStatusCodes(IGNORE_STATUS_CODES, DEFAULT_IGNORE_STATUS_CODES);
        expectedErrors = initExpectedErrors();
        expectedStatusCodes = initStatusCodes(EXPECTED_STATUS_CODES);
        isEventsEnabled = initEventsEnabled();
        maxEventsStored = getIntProperty(MAX_EVENT_SAMPLES_STORED, DEFAULT_MAX_EVENT_SAMPLES_STORED);
        ignoreErrorPriority = getProperty(IGNORE_ERROR_PRIORITY, DEFAULT_IGNORE_ERROR_PRIORITY);
        exceptionHandlers = initExceptionHandlers();
    }

    private Object initExceptionHandlers() {
        Object value = getProperty(EXCEPTION_HANDLERS);
        return value == null ? DEFAULT_EXCEPTION_HANDLERS : value;
    }

    private Set<IgnoreErrorConfig> initIgnoreErrors() {
        Set<IgnoreErrorConfig> ignoreErrorsConfig = new HashSet<>();

        // First, get legacy "ignore errors" classes
        Collection<String> ignoreErrors = getUniqueStrings(IGNORE_ERRORS);
        for (String ignoreError : ignoreErrors) {
            ignoreErrorsConfig.add(new IgnoreErrorConfigImpl(ignoreError.replace('/', '.'), null));
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_LEGACY);
        }

        // Second, get new "ignore_classes" property
        Object ignoreClasses = getProperty(IGNORE_CLASSES);
        if (ignoreClasses instanceof Collection) {
            for (Object ignoreError : (Collection) ignoreClasses) {
                // Check to see if we have the Map based format (default for Java agent).
                // Otherwise fallback to class name String check (default for most other agents).
                if (ignoreError instanceof Map) {
                    Map<String, String> ignoreErrorMap = (Map) ignoreError;
                    String className = ignoreErrorMap.get(IGNORE_CLASS_NAME);
                    String message = ignoreErrorMap.get(IGNORE_MESSAGE);

                    // Error class cannot be null, but the ignored error message is optional and can be null
                    if (className != null) {
                        className = className.trim();
                        if (message != null && !message.isEmpty()) {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS_MESSAGE);
                        } else {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS);
                        }
                        ignoreErrorsConfig.add(new IgnoreErrorConfigImpl(className.replace('/', '.'), message));
                    } else {
                        AgentBridge.getAgent().getLogger().log(Level.WARNING, "Invalid ignore_classes config" +
                                " encountered. class_name must not be null. This configuration will be ignored");
                    }
                } else if (ignoreError instanceof String) {
                    String className = ((String) ignoreError).trim();
                    ignoreErrorsConfig.add(new IgnoreErrorConfigImpl(className.replace('/', '.'), null));
                    MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS);
                }
            }
        }

        // Third, get fallback "ignore_messages" property
        Object ignoreMessages = getProperty(IGNORE_MESSAGES);
        if (ignoreMessages instanceof Map) {
            Map<String, List<String>> ignoreMessagesMap = (Map) ignoreMessages;
            for (Map.Entry<String, List<String>> ignoreError : ignoreMessagesMap.entrySet()) {
                String className = ignoreError.getKey().trim();
                for (String message : ignoreError.getValue()) {
                    ignoreErrorsConfig.add(new IgnoreErrorConfigImpl(className.replace('/', '.'), message));
                    MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_IGNORE_ERROR_CONFIG_CLASS_MESSAGE);
                }
            }
        }

        // If all came back empty, let's use the default
        if (ignoreErrorsConfig.isEmpty()) {
            for (String defaultIgnoreError : DEFAULT_IGNORE_ERRORS) {
                ignoreErrorsConfig.add(new IgnoreErrorConfigImpl(defaultIgnoreError.replace('/', '.'), null));
            }
        }

        return ignoreErrorsConfig;
    }

    private boolean initEnabled() {
        boolean isEnabled = getProperty(ENABLED, DEFAULT_ENABLED);
        // required server property - false if subscription does not permit error collection
        boolean canCollectErrors = getProperty(COLLECT_ERRORS, DEFAULT_COLLECT_ERRORS);
        return isEnabled && canCollectErrors;
    }

    private boolean initEventsEnabled() {
        boolean collector = getProperty(COLLECT_EVENTS, DEFAULT_COLLECT_EVENTS);
        boolean config = getProperty(CAPTURE_EVENTS, DEFAULT_CAPTURE_EVENTS);
        return collector && config;
    }

    private Set<ExpectedErrorConfig> initExpectedErrors() {
        Set<ExpectedErrorConfig> expectedErrorConfigs = new HashSet<>();

        Object expectedErrors = getProperty(EXPECTED_CLASSES);
        if (expectedErrors instanceof Collection) {
            for (Object expectedError : (Collection) expectedErrors) {
                // Check to see if we have the Map based format (default for Java agent).
                // Otherwise fallback to class name String check (default for most other agents).
                if (expectedError instanceof Map) {
                    Map<String, String> expectedErrorMap = (Map) expectedError;
                    String errorClass = expectedErrorMap.get(CLASS_NAME);
                    String errorMessage = expectedErrorMap.get(MESSAGE);

                    // Error class cannot be null, but the expected error message is optional and can be null
                    if (errorClass != null && !errorClass.isEmpty()) {
                        errorClass = errorClass.trim();
                        if (errorMessage != null && !errorMessage.isEmpty()) {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS_MESSAGE);
                        } else {
                            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS);
                        }
                        expectedErrorConfigs.add(new ExpectedErrorConfigImpl(errorClass, errorMessage));
                    } else {
                        AgentBridge.getAgent().getLogger().log(Level.WARNING, "Invalid expected_classes config" +
                                " encountered. class_name must not be null. This configuration will be ignored");
                    }
                } else if (expectedError instanceof String) {
                    String className = ((String) expectedError).trim();
                    expectedErrorConfigs.add(new ExpectedErrorConfigImpl(className.replace('/', '.'), null));
                    MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS);
                }
            }
        } else if (expectedErrors != null) {
            logStatusCodeConfigError(EXPECTED_CLASSES, "invalid type", expectedErrors);
        }

        // Get fallback "expected_messages" property
        Object expectedMessages = getProperty(EXPECTED_MESSAGES);
        if (expectedMessages instanceof Map) {
            Map<String, List<String>> expectedMessagesMap = (Map) expectedMessages;
            for (Map.Entry<String, List<String>> expectedError : expectedMessagesMap.entrySet()) {
                String className = expectedError.getKey().trim();
                for (String message : expectedError.getValue()) {
                    expectedErrorConfigs.add(new ExpectedErrorConfigImpl(className.replace('/', '.'), message));
                    MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_CONFIG_CLASS_MESSAGE);
                }
            }
        }

        return expectedErrorConfigs;
    }

    private void logStatusCodeConfigError(String configurationKey, String msg, Object value) {
        AgentBridge.getAgent().getLogger().log(Level.WARNING, "Invalid {0} config encountered: {1}. " +
                "{0} {2}. This configuration will be ignored", configurationKey, value, msg);
    }

    private Set<Integer> initStatusCodes(String configurationKey) {
        return initStatusCodes(configurationKey, Collections.<Integer>emptySet());
    }

    private Set<Integer> initStatusCodes(String configurationKey, Set<Integer> defaultValues) {
        Object statusCodesValue = getProperty(configurationKey);
        if (statusCodesValue == null) {
            return defaultValues;
        } else if (statusCodesValue instanceof Number) {
            // A single status code as an integer in the config (e.g. - ... : 404)
            return ImmutableSet.of(((Number) statusCodesValue).intValue());
        } else if (statusCodesValue instanceof Collection<?>) {
            // A collection of integers in the config (e.g. - ... : [ 500, 501, 502, 503 ])
            return Collections.unmodifiableSet(getIntegerSet(configurationKey, defaultValues));
        } else if (statusCodesValue instanceof String) {
            // A string of status codes which could either be a single status code (e.g. - ... : "404")
            // or a range of status codes (e.g. - ... : "400-499")
            // or a comma separated combination of both (e.g. - ... : "400,500-509,599")
            Set<Integer> statusCodes = new HashSet<>();
            String[] statusCodesOrRanges = ((String) statusCodesValue).split(",");
            for (String statusCodeOrRange : statusCodesOrRanges) {
                if (statusCodeOrRange == null || statusCodeOrRange.isEmpty()) {
                    continue;
                }
                try {
                    statusCodeOrRange = statusCodeOrRange.trim();
                    int statusCode = Integer.parseInt(statusCodeOrRange);
                    statusCodes.add(statusCode);
                    continue; // We've matched a status code, continue to the next value
                } catch (NumberFormatException e) {
                    // This is normal if we have a range
                }

                if (!parseRange(configurationKey, statusCodeOrRange, statusCodes)) {
                    return Collections.emptySet();
                }
            }
            return statusCodes;
        } else if (statusCodesValue != null) {
            logStatusCodeConfigError(
                    configurationKey, "must be an integer or a range", statusCodesValue);
        }
        return Collections.emptySet();
    }

    private boolean parseRange(String configurationKey, String statusCodeRange, Set<Integer> statusCodes) {
        // If we got here it means that we might have a range so we should try to validate it
        String[] split = statusCodeRange.split("-");
        if (split.length == 2) {
            try {
                Integer lower = Integer.parseInt(split[0]);
                Integer upper = Integer.parseInt(split[1]);
                if (lower > upper) {
                    logStatusCodeConfigError(configurationKey, "range must start with lower bound", statusCodeRange);
                } else if (lower < 0 || upper > 1000) {
                    logStatusCodeConfigError(configurationKey, "must be between 0 and 1000", statusCodeRange);
                } else {
                    // Success
                    for (int i = lower; i <= upper; i++) {
                        statusCodes.add(i);
                    }
                    return true;
                }
            } catch (NumberFormatException e) {
                logStatusCodeConfigError(configurationKey, "range values must be integers", statusCodeRange);
            }
        } else {
            logStatusCodeConfigError(configurationKey, "range must contain two integers", statusCodeRange);
        }
        return false;
    }

    @Override
    public Set<IgnoreErrorConfig> getIgnoreErrors() {
        return ignoreErrors;
    }

    @Override
    public Set<Integer> getIgnoreStatusCodes() {
        return ignoreStatusCodes;
    }

    @Override
    public Set<ExpectedErrorConfig> getExpectedErrors() {
        return expectedErrors;
    }

    @Override
    public Set<Integer> getExpectedStatusCodes() {
        return expectedStatusCodes;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isEventsEnabled() {
        return isEventsEnabled;
    }

    @Override
    public int getMaxSamplesStored() {
        return maxEventsStored;
    }

    static ErrorCollectorConfig createErrorCollectorConfig(Map<String, Object> settings) {
        if (settings == null) {
            settings = Collections.emptyMap();
        }
        return new ErrorCollectorConfigImpl(settings);
    }

    @Override
    public boolean isIgnoreErrorPriority() {
        return ignoreErrorPriority;
    }

    @Override
    public Object getExceptionHandlers() {
        return exceptionHandlers;
    }
}
