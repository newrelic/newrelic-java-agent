package com.newrelic.agent.errors;

import com.newrelic.api.agent.ErrorGroupCallback;

public class ErrorGroupCallbackHolder {
    private static ErrorGroupCallback errorGroupCallback = null;

    public static void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        errorGroupCallback = errorGroupCallback;
    }

    public static ErrorGroupCallback getErrorGroupCallback() {
        return errorGroupCallback;
    }
}
