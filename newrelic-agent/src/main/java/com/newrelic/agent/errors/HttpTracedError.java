/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.TransactionData;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.config.ErrorCollectorConfig;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

public class HttpTracedError extends TracedError {

    private static int UNKNOWN_STATUS_CODE = -1;

    private final int responseStatus;
    private final String errorMessage;

    private HttpTracedError(ErrorCollectorConfig errorCollectorConfig, String appName,
            String frontendMetricName, String requestUri, int responseStatus, String errorMessage,
            long timestamp, Map<String, Map<String, String>> prefixedParams,
            Map<String, Object> userParams, Map<String, Object> agentParams,
            Map<String, ?> errorParams, Map<String, Object> intrinsics,
            TransactionData transactionData, boolean expected, String transactionGuid) {
        super(errorCollectorConfig, appName, frontendMetricName, timestamp, requestUri, prefixedParams, userParams,
                agentParams, errorParams, intrinsics, transactionData, expected, transactionGuid);

        this.responseStatus = responseStatus;
        if (errorMessage == null && responseStatus != UNKNOWN_STATUS_CODE) {
            this.errorMessage = getStatusErrorMessage(responseStatus);
        } else {
            this.errorMessage = errorMessage;
        }
    }

    public static class Builder extends TracedError.Builder implements HttpTracedErrorRequired {

        private int responseStatus;
        private String errorMessage;

        Builder(ErrorCollectorConfig errorCollectorConfig, String appName, String frontendMetricName, long timestampInMillis) {
            super(errorCollectorConfig, appName, frontendMetricName, timestampInMillis);
        }

        @Override
        public Builder statusCodeAndMessage(int responseStatus, String errorMessage) {
            this.responseStatus = responseStatus;
            this.errorMessage = errorMessage;
            return this;
        }

        @Override
        public Builder message(String errorMessage) {
            this.responseStatus = UNKNOWN_STATUS_CODE;
            this.errorMessage = errorMessage;
            return this;
        }

        public HttpTracedError build() {
            return new HttpTracedError(errorCollectorConfig, appName, frontendMetricName,
                    requestUri, responseStatus, errorMessage, timestampInMillis, prefixedAttributes, userAttributes,
                    agentAttributes, errorAttributes, intrinsicAttributes, transactionData, expected, transactionGuid);
        }

    }

    public interface HttpTracedErrorRequired {

        Builder statusCodeAndMessage(int responseStatus, String errorMessage);

        Builder message(String errorMessage);

    }

    public static HttpTracedErrorRequired builder(ErrorCollectorConfig errorCollectorConfig,
            String appName, String frontendMetricName, long timestampInMillis) {
        return new Builder(errorCollectorConfig, appName, frontendMetricName, timestampInMillis);
    }

    @Override
    public Collection<String> stackTrace() {
        return null;
    }

    @Override
    public String getExceptionClass() {
        String message = getMessage();
        if (message != null && message.getBytes(StandardCharsets.UTF_8).length > ConfigConstant.MAX_USER_ATTRIBUTE_SIZE) {
            message = getStatusErrorMessage(responseStatus);
        }
        return message;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }

    private String getStatusErrorMessage(int responseStatus) {
        if (responseStatus >= HttpURLConnection.HTTP_BAD_REQUEST
                && responseStatus < HttpURLConnection.HTTP_INTERNAL_ERROR) {
            return "HttpClientError " + responseStatus;
        } else {
            return "HttpServerError " + responseStatus;
        }
    }

    public int getStatusCode() {
        return responseStatus;
    }

    @Override
    public boolean incrementsErrorMetric() {
        boolean isExpectedStatusCode = errorCollectorConfig.getExpectedStatusCodes().contains(responseStatus);
        return !expected && !isExpectedStatusCode;
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0} ({1})", getMessage(), responseStatus);
    }

}
