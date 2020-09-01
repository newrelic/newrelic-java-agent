package com.newrelic.agent.discovery;

import org.json.simple.JSONValue;

public class DefaultJsonSerializer implements JsonSerializer {

    @Override
    public String serialize(Object object, boolean encode) {
        return JSONValue.toJSONString(object);
    }
}
