package com.newrelic.api.agent;

import java.util.Map;


public interface AttributeCarrying {

    /**
     * Adds/Replaces a numerical attribute on the current tracer.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, Number value); //

    /**
     * Adds/Replaces a string attribute on the current tracer.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, String value);

    /**
     * Adds/Replaces a boolean attribute on the current tracer.
     *
     * @since 6.1.0
     */
    void addCustomAttribute(String key, boolean value);

    /**
     * Adds/Replaces key/value pairs on the current tracer.
     *
     * @since 6.1.0
     */
    void addCustomAttributes(Map<String, Object> attributes);

}
