package com.newrelic.api.agent;

import java.util.Map;


public interface AttributeHolder {

    /**
     * Adds/Replaces a numerical attribute.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, Number value);

    /**
     * Adds/Replaces a string attribute.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, String value);

    /**
     * Adds/Replaces a boolean attribute.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, boolean value);

    /**
     * Adds/Replaces key/value pairs.
     *
     * @since 6.1.0
     */
    void addCustomAttributes(Map<String, Object> attributes);

}
