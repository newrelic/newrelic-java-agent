package com.newrelic.api.agent;

public interface ErrorGrouper {

    String group(ErrorData errorData);
}
