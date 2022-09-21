/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Response;
import com.newrelic.api.agent.Trace;
import org.junit.Test;
import test.newrelic.EnvironmentHolderSettingsGenerator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExpectedErrorsTest {

    private static final ClassLoader CLASS_LOADER = ExpectedErrorsTest.class.getClassLoader();
    private static final String CONFIG_FILE = "configs/expected_errors_test.yml";

    public EnvironmentHolder setupEnvironemntHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Test
    public void nonExpectedError() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_expected_error_test");

        try {
            try {
                throwException("something");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedError() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_test");

        try {
            try {
                throwException("something");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedErrorWrongMessage() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_bad_message_test");

        try {
            try {
                throwException("pleasen be right");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 1);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 1, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedErrorRightMessage() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_good_message_test");

        try {
            try {
                throwException("definitely right");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 1);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedStatus() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_status_code_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/reportStatusCode",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(0, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedStatusRange() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_status_code_range_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/reportStatusCode",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(0, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    private static class UnExpectedException extends RuntimeException {
        UnExpectedException(String msg) {
            super(msg);
        }
    }

    @Test
    public void testNoticeErrorAPIExpectedViaConfig() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_config_api_test");

        try {
            NewRelic.noticeError(new UnExpectedException("I'm not expected")); //nothing configured
            NewRelic.noticeError(new ExpectedError("You should have expected me")); //only exception class is configured
            NewRelic.noticeError(new RuntimeException("I should be expected by my message")); //exception class plus message is configured

            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, transactionList.size());

            assertEquals(2, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 2, 1, 1);
        } finally {

            holder.close();
        }

    }

    @Test
    public void expectedStatusSoClose() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_expected_status_code_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/reportStatusCode",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(1, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void nonExpectedErrorNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_expected_error_test");

        try {
            try {
                throwExceptionNoTransaction("something");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(0, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {

            holder.close();
        }
    }

    @Test
    public void expectedErrorNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_test");

        try {
            try {
                throwExceptionNoTransaction("something");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(0, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedErrorWrongMessageNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_bad_message_test");

        try {
            try {
                throwExceptionNoTransaction("please be right");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(0, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 1);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 1, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedErrorRightMessageNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_error_good_message_test");

        try {
            try {
                throwExceptionNoTransaction("definitely right");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(0, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 1);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedAndIgnoredError() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_and_ignored_test");

        try {
            try {
                throwException("something");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("ErrorsExpected/all").getCallCount());
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 1, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedClassesFallback() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_classes_fallback_test");

        try {
            try {
                throwException(new IgnoredError("blah"));
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 2, 0);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedMessagesFallback() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_messages_fallback_test");

        try {
            try {
                throwException(new IgnoredError("message"));
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 3);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedMessagesFallbackExpected() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_messages_fallback_test");

        try {
            try {
                throwException(new ExpectedError("expected"));
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 0, 3);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 0);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedClassMessagesFallbackOverride() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_class_messages_fallback_test");

        try {
            try {
                throwException(new ExpectedError("blah")); // Doesn't matter what the message is, it will be expected
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 2);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 1);
        } finally {
            holder.close();
        }
    }

    @Test
    public void expectedClassMessagesFallbackNotIgnored() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("expected_class_messages_fallback_test");

        try {
            try {
                throwException(new IgnoredError("blah")); // This should be allowed through as a normal error
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.ExpectedErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());

            verifyExpectedErrorSupportabilityApiCalls(statsEngine, 0, 0, 1, 2);
            verifyIgnoreErrorSupportabilityApiCalls(statsEngine, 0, 0, 1);
        } finally {
            holder.close();
        }
    }

    /**
     * This verifies that the supportability api metrics are incremented when processing expected errors while reading
     * the config file. However, due to how the environment holder is setup, the config is read twice, so you will see
     * ErrorCollectorConfigImpl#initExpectedErrors called twice which doubles our metrics. Therefore the values need to
     * be doubled.
     * <p>
     * The apiThrowable metric is recorded in the noticeError API and does not suffer from the same need to double count
     * in the assertion.
     * <p>
     * When calling this method, pass in the number of 'actual' calls you expect to occur under normal operation.
     */
    private void verifyExpectedErrorSupportabilityApiCalls(StatsEngine statsEngine, int apiMessage, int apiThrowable, int configClass, int configClassMessage) {
        assertEquals(2 * apiMessage, statsEngine.getStats("Supportability/API/ExpectedError/Api/Message/API").getCallCount());
        //The apiThrowable metric is not dependent on the config double read. You should pass in the actual expected count and the assertion should
        //not double count
        assertEquals(1 * apiThrowable, statsEngine.getStats("Supportability/API/ExpectedError/Api/Throwable/API").getCallCount());
        assertEquals(2 * configClass, statsEngine.getStats("Supportability/API/ExpectedError/Config/Class/API").getCallCount());
        assertEquals(2 * configClassMessage, statsEngine.getStats("Supportability/API/ExpectedError/Config/ClassMessage/API").getCallCount());
    }

    /**
     * This verifies that the supportability api metrics are incremented when processing ignore errors while reading
     * the config file. However, due to how the environment holder is setup, the config is read twice, so you will see
     * ErrorCollectorConfigImpl#initIgnoreErrors called twice which doubles our metrics. Therefore the values need to
     * be doubled.
     *
     * When calling this method, pass in the number of 'actual' calls you expect to occur under normal operation.
     */
    private void verifyIgnoreErrorSupportabilityApiCalls(StatsEngine statsEngine, int configLegacy, int configClass, int configClassMessage) {
        assertEquals(2 * configLegacy, statsEngine.getStats("Supportability/API/IgnoreError/Config/Legacy/API").getCallCount());
        assertEquals(2 * configClass, statsEngine.getStats("Supportability/API/IgnoreError/Config/Class/API").getCallCount());
        assertEquals(2 * configClassMessage, statsEngine.getStats("Supportability/API/IgnoreError/Config/ClassMessage/API").getCallCount());
    }

    @Trace(dispatcher = true)
    public void throwException(String message) throws Exception {
        throw new ExpectedError(message);
    }

    @Trace(dispatcher = true)
    public void throwException(Exception exception) throws Exception {
        throw exception;
    }

    @Trace(dispatcher = true)
    public void reportStatusCode() {
        NewRelic.getAgent().getTransaction().setWebResponse(new FakeResponse());
    }

    public void throwExceptionNoTransaction(String message) {
        NewRelic.noticeError(new ExpectedError(message));
    }

    public static class FakeResponse implements Response {
        @Override
        public HeaderType getHeaderType() {
            return null;
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public int getStatus() {
            return 420;
        }

        @Override
        public String getStatusMessage() {
            return null;
        }

        @Override
        public String getContentType() {
            return null;
        }
    }

}
