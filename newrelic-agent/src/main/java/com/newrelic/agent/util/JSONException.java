/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class JSONException extends Exception implements JSONStreamAware {

    private static final long serialVersionUID = 3132223563667774992L;

    public JSONException(String message, Throwable cause) {
        super(message, cause);
    }

    public JSONException(String message) {
        super(message);
    }

    public JSONException(Throwable cause) {
        super(cause);
    }

    @Override
    public void writeJSONString(Writer out) throws IOException {
        JSONObject.writeJSONString(new HashMap<String, Map>() {
            {
                Map<String, Object> vals = new HashMap<>();
                vals.put("message", getMessage());
                Object cause = getCause();
                if (cause != null) {
                    vals.put("type", cause.getClass().getName());
                }
                vals.put("backtrace", StackTraces.stackTracesToStrings(getStackTrace()));
                put("exception", vals);
            }
        }, out);
    }

}
