package com.newrelic.api.agent;

public interface ErrorGroupCallback {

    String generateGroupingString(ErrorData errorData);
}
