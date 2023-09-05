package com.newrelic.agent.errors;

import com.newrelic.agent.transaction.TransactionThrowable;
import org.junit.Assert;
import org.junit.Test;

public class ErrorAnalyzerTest {
    @Test
    public void defaultImpl_areErrorsEnabled_returnsTrue() {
        Assert.assertTrue(ErrorAnalyzer.DEFAULT.areErrorsEnabled());
    }

    @Test
    public void defaultImpl_isReportable_returnsCorrectValue() {
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isReportable(100));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isReportable(200));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isReportable(300));
        Assert.assertTrue(ErrorAnalyzer.DEFAULT.isReportable(400));

        Assert.assertTrue(ErrorAnalyzer.DEFAULT.isReportable(100, new Exception()));
        Assert.assertTrue(ErrorAnalyzer.DEFAULT.isReportable(100, new TransactionThrowable(new Exception(), true, "spanid")));
    }

    @Test
    public void defaultImpl_isIgnoredStatus_returnsFalse() {
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredStatus(100));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredStatus(200));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredStatus(300));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredStatus(400));
    }

    @Test
    public void defaultImpl_isIgnoredThrowable_returnsFalse() {
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredThrowable(new Exception()));
    }

    @Test
    public void defaultImpl_isIgnoredError_returnsFalse() {
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isIgnoredError(100, new Exception()));
    }

    @Test
    public void defaultImpl_isExpectedError_returnsFalse() {
        Assert.assertTrue(ErrorAnalyzer.DEFAULT.isExpectedError(100, new TransactionThrowable(new Exception(), true, "spanid")));
        Assert.assertFalse(ErrorAnalyzer.DEFAULT.isExpectedError(100, new TransactionThrowable(new Exception(), false, "spanid")));
    }
}
