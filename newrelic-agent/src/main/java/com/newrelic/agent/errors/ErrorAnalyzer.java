/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.transaction.TransactionThrowable;

import java.net.HttpURLConnection;

public interface ErrorAnalyzer {
    int NO_STATUS = 0;

    /**
     * Return true if error collection is enabled for this application, false otherwise
     */
    boolean areErrorsEnabled();

    /**
     * Identifies if a status code is reportable. This generally means >= 400.
     */
    boolean isReportable(int statusCode);

    /**
     * Identifies if a status code <em>OR</em> a throwable are reportable. This generally means
     * status code >= 400 <em>OR</em> throwable is not {@literal null}
     */
    boolean isReportable(int statusCode, Throwable throwable);

    /**
     * Identifies if a status code <em>OR</em> a throwable are reportable. This generally means
     * status code >= 400 <em>OR</em> throwable is not {@literal null}.
     */
    boolean isReportable(int statusCode, TransactionThrowable transactionThrowable);

    /**
     * Returns {@literal true} if the status code is in a list of configured ignored status codes. This
     * strictly checks the list and does not take reportability into account.
     *
     * <p>An ignored error is not reported.</p>
     */
    boolean isIgnoredStatus(int statusCode);

    /**
     * Returns {@literal true} if the throwable's class is in a list of configured ignored throwable classes.
     *
     * <p>An ignored error is not reported.</p>
     */
    boolean isIgnoredThrowable(Throwable throwable);

    /**
     * Returns {@literal true} if the throwable is ignored <em>OR</em> the status code is ignored.
     *
     * <p>An ignored error is not reported.</p>
     */
    boolean isIgnoredError(int statusCode, Throwable throwable);

    /**
     * Returns {@literal true} if the throwable's class is in a list of configured expected
     * throwable classes, if the throwable has already been marked "expected", or if the
     * status code is in a list of expected status codes.
     *
     * <p>An expected error</p> is still reported, but it's marked expected so queries
     * can easily filter them.
     */
    boolean isExpectedError(int statusCode, TransactionThrowable transactionThrowable);

    // An implementation of the interface in case we don't have any configuration.
    ErrorAnalyzer DEFAULT = new ErrorAnalyzer() {

        @Override
        public boolean areErrorsEnabled() {
            return true;
        }

        @Override
        public boolean isReportable(int statusCode) {
            return statusCode >= HttpURLConnection.HTTP_BAD_REQUEST;
        }

        @Override
        public boolean isReportable(int statusCode, Throwable throwable) {
            return isReportable(statusCode) || throwable != null;
        }

        @Override
        public boolean isReportable(int statusCode, TransactionThrowable transactionThrowable) {
            return isReportable(statusCode) || transactionThrowable != null;
        }

        @Override
        public boolean isIgnoredStatus(int statusCode) {
            return false;
        }

        @Override
        public boolean isIgnoredThrowable(Throwable throwable) {
            return false;
        }

        @Override
        public boolean isIgnoredError(int statusCode, Throwable throwable) {
            return false;
        }

        @Override
        public boolean isExpectedError(int statusCode, TransactionThrowable transactionThrowable) {
            return transactionThrowable != null && transactionThrowable.expected;
        }
    };
}
