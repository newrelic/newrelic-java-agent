package com.newrelic.agent.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.newrelic.agent.config.BaseConfig.COMMA_SEPARATOR;

/**
 * Any text config value has the potential to be interpreted as more than
 * one data type.
 * <p>
 * This class allows for a text config value to be accessed
 * as a type it can validly be coerced into, or accessed as a String.
 * <p>
 * This class also acts as a wrapper around simple data types that may be
 * used in config - Boolean and Integer.
 * <p>
 * Parsing is carried out when config is loaded rather than at the time of access
 * to avoid parsing more than once.
 * <p>
 * Retrieving a value with a null default value will return the original data type of
 * the config.
 * <p>
 * Retrieving a value with a default will attempt to return the config value coerced
 * into the type of the default, if possible. If the defined value cannot be coerced
 * into the type of the default value, then the default will be returned.
 * <p>
 * Be wary of coercing into Lists and Maps. Elements, keys and values will be Strings,
 * but treating them as any other type will only fail when accessing individual
 * members, not when retrieving the parsed value.
 */
public class ParsedConfigValue {

    private static final Pattern MAP_TYPE_VALUE = Pattern.compile(".+:.+");

    private Object originalValue;
    private List<String> valueAsList;
    private Map<String, String> valueAsMap;
    private Boolean valueAsBoolean;
    private Integer valueAsInteger;

    public ParsedConfigValue(Object value) {
        originalValue = value;
        if (value instanceof String) {
            String valueAsString = (String) value;
            valueAsList = parseListValue(valueAsString);
            // note, the YAML parser interprets "on" as true and "off" as false.
            // It's unclear whether we should accept the strings "on" and "off"
            // but currently we just follow the semantics of Boolean - any variation
            // on "true" is true, anything else is false.
            valueAsBoolean = Boolean.parseBoolean(valueAsString);
            if (MAP_TYPE_VALUE.matcher(valueAsString).find()) {
                valueAsMap = parseMapValue(valueAsString);
            }
            try {
                valueAsInteger = Integer.parseInt(valueAsString);
            } catch (NumberFormatException e) {
                valueAsInteger = null;
            }
        }
    }

    public <T> T getParsedValue() {
        return getParsedValue(null);
    }

    public <T> T getParsedValue(T defaultValue) {
        Object value = null;
        if (defaultValue == null || defaultValue.getClass().equals(originalValue.getClass())) {
            value = originalValue;
        } else if (defaultValue instanceof Map) {
            value = valueAsMap;
        } else if (defaultValue instanceof List) {
            value = valueAsList;
        } else if (defaultValue instanceof Integer && valueAsInteger != null) {
            value = valueAsInteger;
        } else if (defaultValue instanceof Boolean) {
            value = valueAsBoolean;
        }
        return value == null ? defaultValue : (T) value;
    }

    private List<String> parseListValue(String originalValue) {
        return BaseConfig.getUniqueStringsFromString(originalValue, COMMA_SEPARATOR);
    }

    private Map<String, String> parseMapValue(String originalValue) {
        return new LabelsConfigImpl(originalValue).getLabels();
    }

}
