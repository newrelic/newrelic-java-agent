package com.newrelic.agent.bridge;

import com.newrelic.api.agent.ErrorApi;
import com.newrelic.api.agent.ErrorGroupCallback;

import java.util.Map;

class NoOpErrorApi implements ErrorApi {
    static final ErrorApi INSTANCE = new NoOpErrorApi();

    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
    }

    @Override
    public void noticeError(String message, Map<String, ?> params, boolean expected) {
    }

    @Override
    public void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
    }
}
