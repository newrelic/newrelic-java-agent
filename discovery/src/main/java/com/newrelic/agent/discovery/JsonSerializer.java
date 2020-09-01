package com.newrelic.agent.discovery;

public interface JsonSerializer {
    /**
     * Serialize a map to json.
     * @param map
     * @param encode if true, base 64 encode the json string
     */
    String serialize(Object object, boolean encode);
}
