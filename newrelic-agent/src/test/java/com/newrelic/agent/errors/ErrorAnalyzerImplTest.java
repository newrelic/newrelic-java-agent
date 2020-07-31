/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.errors;

import com.newrelic.agent.config.ErrorCollectorConfig;
import com.newrelic.agent.config.ExpectedErrorConfig;
import com.newrelic.agent.config.ExpectedErrorConfigImpl;
import com.newrelic.agent.config.IgnoreErrorConfig;
import com.newrelic.agent.config.IgnoreErrorConfigImpl;
import com.newrelic.agent.transaction.TransactionThrowable;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static java.net.HttpURLConnection.*;

public class ErrorAnalyzerImplTest {

    @Test
    public void statusCodeOnlyIsReportable() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig();
        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertFalse(target.isReportable(HTTP_OK));
        assertFalse(target.isReportable(HTTP_ACCEPTED));
        assertFalse(target.isReportable(HTTP_MOVED_TEMP));
        assertFalse(target.isReportable(399));
        assertTrue(target.isReportable(HTTP_BAD_REQUEST));
        assertTrue(target.isReportable(HTTP_INTERNAL_ERROR));
    }

    @Test
    public void statusCodeOrThrowableAreReportable() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig();
        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertFalse(target.isReportable(HTTP_OK, (Throwable)null));
        assertTrue(target.isReportable(HTTP_OK, new Throwable("bye")));

        assertTrue(target.isReportable(HTTP_BAD_REQUEST, (Throwable)null));
        assertTrue(target.isReportable(HTTP_BAD_REQUEST, new Throwable("bye")));
    }

    @Test
    public void statusCodeOrTransactionThrowableAreReportable() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig();
        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertFalse(target.isReportable(HTTP_OK, (TransactionThrowable)null));
        assertTrue(target.isReportable(
                HTTP_OK,
                new TransactionThrowable(new Throwable("bye"), false, null)));

        assertTrue(target.isReportable(HTTP_BAD_REQUEST, (TransactionThrowable)null));
        assertTrue(target.isReportable(
                HTTP_BAD_REQUEST,
                new TransactionThrowable(new Throwable("bye"), false, null)));
    }

    @Test
    public void ignoredStatusFollowsConfigNotStatusCodeMeaning() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setIgnoreStatusCodes(HTTP_BAD_REQUEST, HTTP_ACCEPTED);
        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertFalse(target.isIgnoredStatus(HTTP_OK));
        assertTrue(target.isIgnoredStatus(HTTP_ACCEPTED));

        assertTrue(target.isIgnoredStatus(HTTP_BAD_REQUEST));
        assertFalse(target.isIgnoredStatus(HTTP_UNAUTHORIZED));

        assertFalse(target.isIgnoredStatus(ErrorAnalyzer.NO_STATUS));
    }

    @Test
    public void ignoredThrowableFollowsConfig() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setIgnoreErrors(
                        new IgnoreErrorConfigImpl(MyThrowable.class.getName(), null),
                        new IgnoreErrorConfigImpl(RuntimeException.class.getName(), "ignore_token"));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertTrue(target.isIgnoredThrowable(new MyThrowable("message")));
        assertTrue(target.isIgnoredThrowable(new MyThrowable(null)));
        assertTrue(target.isIgnoredThrowable(new RuntimeException("ignore_token")));
        assertFalse(target.isIgnoredThrowable(new RuntimeException("not ignored")));
        assertFalse(target.isIgnoredThrowable(new RuntimeException((String)null)));
    }

    @Test
    public void ignoredThrowableFollowsAnyCause() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setIgnoreErrors(
                        new IgnoreErrorConfigImpl(MyThrowable.class.getName(), null));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertTrue(target.isIgnoredThrowable(new MyThrowable("message")));
        assertTrue(target.isIgnoredThrowable(new Exception("other", new MyThrowable("message"))));
        assertFalse(target.isIgnoredThrowable(new Exception("other")));
    }

    @Test
    public void ignoreStatusCodeOrThrowable() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setIgnoreStatusCodes(HTTP_BAD_REQUEST, HTTP_ACCEPTED)
                .setIgnoreErrors(
                        new IgnoreErrorConfigImpl(MyThrowable.class.getName(), null),
                        new IgnoreErrorConfigImpl(RuntimeException.class.getName(), "ignore_token"));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        // neither status nor throwable are ignored.
        assertFalse(target.isIgnoredError(HTTP_OK, null));
        assertFalse(target.isIgnoredError(HTTP_OK, new RuntimeException("not ignored")));
        assertFalse(target.isIgnoredError(HTTP_OK, new RuntimeException((String)null)));

        // only the throwable is ignored.
        assertTrue(target.isIgnoredError(HTTP_OK, new MyThrowable("message")));
        assertTrue(target.isIgnoredError(HTTP_OK, new MyThrowable(null)));
        assertTrue(target.isIgnoredError(HTTP_OK, new RuntimeException("ignore_token")));

        // only the status is ignored
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, null));
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, new RuntimeException("not ignored")));
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, new RuntimeException((String)null)));

        // both status and throwable are ignored
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, new MyThrowable("message")));
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, new MyThrowable(null)));
        assertTrue(target.isIgnoredError(HTTP_BAD_REQUEST, new RuntimeException("ignore_token")));
    }

    @Test
    public void expectedThrowableFollowsAnyCause() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setExpectedErrors(
                        new ExpectedErrorConfigImpl(MyThrowable.class.getName(), null));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertTrue(target.isExpectedError(HTTP_OK, wrap(new MyThrowable("message"))));
        assertTrue(target.isExpectedError(HTTP_OK, wrap(new Exception("other", new MyThrowable("message")))));
        assertFalse(target.isExpectedError(HTTP_OK, wrap(new Exception("other"))));
    }

    @Test
    public void expectStatusCodeOrThrowable() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setExpectedStatusCodes(HTTP_BAD_REQUEST, HTTP_ACCEPTED)
                .setExpectedErrors(
                        new ExpectedErrorConfigImpl(MyThrowable.class.getName(), null),
                        new ExpectedErrorConfigImpl(RuntimeException.class.getName(), "ignore_token"));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        // neither status nor throwable are ignored.
        assertFalse(target.isExpectedError(HTTP_OK, null));
        assertFalse(target.isExpectedError(HTTP_OK, wrap(new RuntimeException("not ignored"))));
        assertFalse(target.isExpectedError(HTTP_OK, wrap(new RuntimeException((String)null))));

        // only the throwable is ignored.
        assertTrue(target.isExpectedError(HTTP_OK, wrap(new MyThrowable("message"))));
        assertTrue(target.isExpectedError(HTTP_OK, wrap(new MyThrowable(null))));
        assertTrue(target.isExpectedError(HTTP_OK, wrap(new RuntimeException("ignore_token"))));

        // only the status is ignored
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, null));
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, wrap(new RuntimeException("not ignored"))));
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, wrap(new RuntimeException((String)null))));

        // both status and throwable are ignored
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, wrap(new MyThrowable("message"))));
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, wrap(new MyThrowable(null))));
        assertTrue(target.isExpectedError(HTTP_BAD_REQUEST, wrap(new RuntimeException("ignore_token"))));
    }

    @Test
    public void nullErrorsAreNotExpected() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig()
                .setExpectedErrors(
                        new ExpectedErrorConfigImpl(MyThrowable.class.getName(), null),
                        new ExpectedErrorConfigImpl(RuntimeException.class.getName(), "ignore_token"));

        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertFalse(target.isExpectedError(HTTP_OK, null));
        assertFalse(target.isExpectedError(HTTP_OK, wrap(null)));
    }

    @Test
    public void expectedErrorsAreIndeedExpected() {
        ErrorCollectorConfig config = new TestErrorCollectorConfig();
        ErrorAnalyzer target = new ErrorAnalyzerImpl(config);

        assertTrue(target.isExpectedError(HTTP_OK, new TransactionThrowable(new Throwable("yo"), true, null)));
    }

    private TransactionThrowable wrap(Throwable toWrap) {
        return new TransactionThrowable(toWrap, false, null);
    }

    static class MyThrowable extends RuntimeException {
        MyThrowable(String message) { super(message); }
    }

    static class TestErrorCollectorConfig implements ErrorCollectorConfig {

        TestErrorCollectorConfig() {
        }

        public TestErrorCollectorConfig setIgnoreErrors(IgnoreErrorConfig... ignoreErrors) {
            this.ignoreErrors = new HashSet<>(Arrays.asList(ignoreErrors));
            return this;
        }

        public TestErrorCollectorConfig setIgnoreStatusCodes(Integer... ignoreStatusCodes) {
            this.ignoreStatusCodes = new HashSet<>(Arrays.asList(ignoreStatusCodes));
            return this;
        }

        public TestErrorCollectorConfig setExpectedErrors(ExpectedErrorConfig... expectedErrors) {
            this.expectedErrors = new HashSet<>(Arrays.asList(expectedErrors));
            return this;
        }

        public TestErrorCollectorConfig setExpectedStatusCodes(Integer... expectedStatusCodes) {
            this.expectedStatusCodes = new HashSet<>(Arrays.asList(expectedStatusCodes));
            return this;
        }

        private Set<IgnoreErrorConfig> ignoreErrors;
        private Set<Integer> ignoreStatusCodes = Collections.emptySet();
        private Set<ExpectedErrorConfig> expectedErrors;
        private Set<Integer> expectedStatusCodes = Collections.emptySet();

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public boolean isEventsEnabled() {
            return false;
        }

        @Override
        public int getMaxSamplesStored() {
            return 0;
        }

        @Override
        public Set<IgnoreErrorConfig> getIgnoreErrors() {
            return ignoreErrors;
        }

        @Override
        public Set<Integer> getIgnoreStatusCodes() {
            return ignoreStatusCodes;
        }

        @Override
        public Set<ExpectedErrorConfig> getExpectedErrors() {
            return expectedErrors;
        }

        @Override
        public Set<Integer> getExpectedStatusCodes() {
            return expectedStatusCodes;
        }

        @Override
        public boolean isIgnoreErrorPriority() {
            return true;
        }

        @Override
        public Object getExceptionHandlers() {
            throw new AssertionError("not a thing here!");
        }
    }
}
