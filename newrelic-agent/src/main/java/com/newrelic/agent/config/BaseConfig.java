/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.config;

import com.google.common.base.CharMatcher;
import com.newrelic.agent.Agent;
import com.newrelic.api.agent.Logger;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

public class BaseConfig implements Config {

    public static final String COMMA_SEPARATOR = ",";
    public static final String SEMI_COLON_SEPARATOR = ";";

    protected static boolean addDeprecatedProperties = true;
    private static final Logger logger = Agent.LOG.getChildLogger(BaseConfig.class);

    private final Map<String, Object> props;
    protected final String systemPropertyPrefix;

    /**
     * Construct a BaseConfig for the map passed as an argument. Values in the map are not subject to override by either
     * system properties or environment variables.
     *
     * @param props the collection from which the values of keys are taken.
     */
    public BaseConfig(Map<String, Object> props) {
        this(props, null);
    }

    /**
     * Construct a BaseConfig.
     *
     * @param props the collection from which the values of keys are taken, subject to override behaviors.
     * @param systemPropertyPrefix If specified as null, values from the collection will not be subject to override by
     * values from the system properties or environment. If non-null, the value serves as the prefix for matching
     * overrides by the system properties and environment, with the environment having priority. Passing an empty
     * string would expose the keys in the collection to override by arbitrary keys the user may have present in
     * the the environment or system properties, so is not allowed.
     * @throws IllegalArgumentException - 0-length string given as systemPropertyPrefix
     */
    public BaseConfig(Map<String, Object> props, String systemPropertyPrefix) {
        if (systemPropertyPrefix != null && systemPropertyPrefix.length() == 0) {
            throw new IllegalArgumentException("prefix must be null or non-empty");
        }

        this.props = props == null ? emptyMap() : unmodifiableMap(props);
        this.systemPropertyPrefix = systemPropertyPrefix;
    }

    public Map<String, Object> getProperties() {
        return props;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> nestedProps(String key) {
        Object value = getProperties().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof ServerProp) {
            value = ((ServerProp) value).getValue();
        }
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        logger.log(Level.WARNING, "Agent configuration expected nested configuration values for \"{0}\", got \"{1}\"", key, value);
        return null;
    }

    protected Object getPropertyFromSystemProperties(String name, Object defaultVal) {
        if (systemPropertyPrefix == null) {
            return null;
        }

        String key = getSystemPropertyKey(name);
        String result = SystemPropertyFactory.getSystemPropertyProvider().getSystemProperty(key);
        return parseValue(result);
    }

    protected String getSystemPropertyKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        return systemPropertyPrefix + key;
    }

    protected Object getPropertyFromSystemEnvironment(String name, Object defaultVal) {
        if (systemPropertyPrefix == null) {
            return null;
        }

        String key = getSystemPropertyKey(name);
        String result = SystemPropertyFactory.getSystemPropertyProvider().getEnvironmentVariable(key);
        return parseValue(result);
    }

    static Object parseValue(String val) {
        if (val == null) {
            return null;
        }
        try {
            return new JSONParser().parse(val);
        } catch (org.json.simple.parser.ParseException e) {
            return val;
        }
    }

    @Override
    public <T> T getProperty(String key, T defaultVal) {
        Object propVal = getProperties().get(key);
        if (propVal instanceof ServerProp) {
            propVal = ((ServerProp) propVal).getValue();
            return castValue(key, propVal, defaultVal);
        }
        Object override = getPropertyFromSystemEnvironment(key, defaultVal);
        if (override != null) {
            return castValue(key, override, defaultVal);
        }
        override = getPropertyFromSystemProperties(key, defaultVal);
        if (override != null) {
            return castValue(key, override, defaultVal);
        }
        return castValue(key, propVal, defaultVal);
    }

    @SuppressWarnings("unchecked")
    protected <T> T castValue(String key, Object value, T defaultVal) {
        try {
            if (defaultVal instanceof Integer && value instanceof Long) {
                return (T) Integer.valueOf(((Long) value).intValue());
            }
            if (defaultVal instanceof Boolean && value instanceof String) {
                return (T) Boolean.valueOf((String) value);
            }
            T val = (T) value;
            if (val == null) {
                return defaultVal;
            } else if (val instanceof String) {
                return (T) ((String) val).trim();
            } else {
                return val;
            }
        } catch (ClassCastException e) {
            logger.log(Level.FINE, e, "Unable to parse {0}", key);
            return defaultVal;
        }
    }

    @Override
    public <T> T getProperty(String key) {
        return getProperty(key, null);
    }

    protected Set<Integer> getIntegerSet(String key, Set<Integer> defaultVal) {
        Object val = getProperty(key);
        if (val instanceof String) {
            return Collections.unmodifiableSet(getIntegerSetFromString((String) val));
        }
        if (val instanceof Collection<?>) {
            return Collections.unmodifiableSet(getIntegerSetFromCollection((Collection<?>) val));
        }
        if (val instanceof Integer) {
            return Collections.unmodifiableSet(getIntegerSetFromCollection(Collections.singletonList((Integer) val)));
        }
        return defaultVal;
    }

    /**
     * Returns a collection of strings for the given key using comma as a separator character.
     *
     * @see BaseConfig#getUniqueStrings(String, String)
     */
    protected List<String> getUniqueStrings(String key) {
        return getUniqueStrings(key, COMMA_SEPARATOR);
    }

    /**
     * Returns a collection of strings for the given key.  The property value can be a collection or
     * a String list that uses a separator character.
     */
    protected List<String> getUniqueStrings(String key, String separator) {
        Object val = getProperty(key);
        if (val instanceof String) {
            return Collections.unmodifiableList(getUniqueStringsFromString((String) val, separator));
        }
        if (val instanceof Collection<?>) {
            return Collections.unmodifiableList(getUniqueStringsFromCollection((Collection<?>) val));
        }
        return Collections.emptyList();
    }

    public static List<String> getUniqueStringsFromCollection(Collection<?> values, String prefix) {
        List<String> result = new ArrayList<>(values.size());
        boolean noPrefix = (prefix == null || prefix.isEmpty());
        for (Object value : values) {
            String val;
            if (value instanceof Integer) {
                val = String.valueOf(value);
            } else if (value instanceof Long) {
                val = String.valueOf(value);
            } else {
                val = (String) value;
            }
            val = val.trim();
            if (val.length() != 0 && !result.contains(val)) {
                if (noPrefix) {
                    result.add(val);
                } else {
                    result.add(prefix + val);
                }
            }
        }
        return result;
    }

    public static List<String> getUniqueStringsFromCollection(Collection<?> values) {
        return getUniqueStringsFromCollection(values, null);
    }

    public static List<String> getUniqueStringsFromString(String valuesString, String separator) {
        String[] valuesArray = valuesString.split(separator);
        List<String> result = new ArrayList<>(valuesArray.length);
        for (String value : valuesArray) {
            value = value.trim();
            if (value.length() != 0 && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    public static void parseMapEntriesFromString(String mapAsString, MapParsingBiConsumer<String, String> parsedEntryHandler) throws
            ParseException {
        mapAsString = CharMatcher.is(';').trimFrom(mapAsString); // allow leading/trailing semicolons

        String[] mapEntries = mapAsString.split(";");
        for (String mapEntry : mapEntries) {
            String[] keyAndValue = mapEntry.split(":");

            if (keyAndValue.length != 2) {
                throw new ParseException("invalid syntax");
            }
            parsedEntryHandler.accept(keyAndValue[0].trim(), keyAndValue[1].trim());
        }
    }

    protected int getIntProperty(String key, int defaultVal) {
        Number val = getProperty(key);
        if (val == null) {
            return defaultVal;
        }
        return val.intValue();
    }

    protected String getStringPropertyOrNull(String key) {
        Object val = getProperty(key);
        if (val == null) {
            logger.log(Level.FINE, "Value for \"{0}\" is null", key);
            return null;
        }
        return val.toString();
    }

    protected double getDoubleProperty(String key, double defaultVal) {
        Number val = getProperty(key);
        if (val == null) {
            return defaultVal;
        }
        return val.doubleValue();
    }

    private Set<Integer> getIntegerSetFromCollection(Collection<?> values) {
        Set<Integer> result = new HashSet<>(values.size());
        for (Object value : values) {
            int val = ((Number) value).intValue();
            result.add(val);
        }
        return result;
    }

    private Set<Integer> getIntegerSetFromString(String valuesString) {
        String[] valuesArray = valuesString.split(COMMA_SEPARATOR);
        Set<Integer> result = new HashSet<>(valuesArray.length);
        for (String value : valuesArray) {
            value = value.trim();
            if (value.length() != 0) {
                result.add(Integer.parseInt(value));
            }
        }
        return result;
    }

    /**
     * Adds a marker for later that a deprecated property should be logged as such.
     *
     * @param deprecatedProperty The property path that should be deprecated (e.g., <code>["transaction_tracer","deprecated_name"]</code>)
     * @param newProperty The property path that should be used instead; set to null if there is no new property.
     */
    protected static void addDeprecatedProperty(String[] deprecatedProperty, String[] newProperty) {
        if (addDeprecatedProperties) {
            deprecatedProperties.put(deprecatedProperty, new DeprecatedProperty(deprecatedProperty, newProperty));
        }
    }

    protected static void clearDeprecatedProperties() {
        deprecatedProperties.clear();
    }

    protected static final Map<String[], DeprecatedProperty> deprecatedProperties = new ConcurrentHashMap<>();

    /**
     * Extension of BiConsumer that can throw a checked {@link ParseException} when accepting input.
     */
    @FunctionalInterface
    protected interface MapParsingBiConsumer<T, U> {
        void accept(T t, U u) throws ParseException;
    }

}
