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
import com.newrelic.agent.bridge.AgentBridge;
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

public class IgnoreErrorsTest {

    private static final ClassLoader CLASS_LOADER = IgnoreErrorsTest.class.getClassLoader();
    private static final String CONFIG_FILE = "configs/ignore_errors_test.yml";

    public EnvironmentHolder setupEnvironemntHolder(String environment) throws Exception {
        EnvironmentHolderSettingsGenerator envHolderSettings = new EnvironmentHolderSettingsGenerator(CONFIG_FILE, environment, CLASS_LOADER);
        EnvironmentHolder environmentHolder = new EnvironmentHolder(envHolderSettings);
        environmentHolder.setupEnvironment();
        return environmentHolder;
    }

    @Test
    public void nonIgnoreError() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_ignore_error_test");

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
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreError() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_test");

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
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreErrorWrongMessage() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_bad_message_test");

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
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreErrorRightMessage() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_good_message_test");

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
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreStatus() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_status_code_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/NormalizedUri/420/*",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(0, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreStatusRange() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_status_code_range_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/NormalizedUri/420/*",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(0, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreStatusSoClose() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_ignore_status_code_test");

        try {
            reportStatusCode();

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("WebTransaction/NormalizedUri/420/*",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
            assertEquals(1, statsEngine.getApdexStats(MetricName.create(MetricNames.APDEX)).getApdexFrustrating());
        } finally {
            holder.close();
        }
    }

    @Test
    public void nonIgnoreErrorNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("non_ignore_error_test");

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
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreErrorNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_test");

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
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreErrorWrongMessageNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_bad_message_test");

        try {
            try {
                throwExceptionNoTransaction("pleasen be right");
                fail("The expected exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(0, transactionList.size());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreErrorRightMessageNoTransaction() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_error_good_message_test");

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
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreClassesFallback() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_classes_fallback_test");

        try {
            try {
                throwException(new IgnoredError("blah"));
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreMessagesFallback() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_messages_fallback_test");

        try {
            try {
                throwException(new IgnoredError("message"));
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreMessagesFallbackIgnored() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_messages_fallback_test");

        try {
            try {
                throwException(new IgnoredError("ignore"));
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreInterfaceMessages() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_interface_messages_test");

        try {
            try {
                throwException(new IgnoredError("blah"));
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            // yml is configured to ignore the interface IgnoredErrorInterface, shouldn't apply to implementing class IgnoredError
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreSuperclassMessages() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_superclass_messages_test");

        try {
            try {
                throwException(new IgnoredError("blah"));
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            // yml is configured to ignore the superclass Exception, shouldn't apply to subclass IgnoredError
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreClassMessagesFallbackOverride() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_class_messages_fallback_test");

        try {
            try {
                throwException(new IgnoredError("blah")); // Doesn't matter what the message is, it will be ignored
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(0, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
    }

    @Test
    public void ignoreClassMessagesFallbackNotIgnored() throws Exception {
        EnvironmentHolder holder = setupEnvironemntHolder("ignore_class_messages_fallback_test");

        try {
            try {
                throwException(new ExpectedError("blah")); // This should be allowed through
                fail("The ignored exception was not thrown");
            } catch (Throwable t) {
            }

            // Verify the transaction was created and finished
            TransactionDataList transactionList = holder.getTransactionList();
            ServiceFactory.getHarvestService().harvestNow();
            assertEquals(1, transactionList.size());
            TransactionData td = transactionList.get(0);
            assertEquals("OtherTransaction/Custom/test.newrelic.test.agent.IgnoreErrorsTest/throwException",
                    td.getPriorityTransactionName().getName());
            StatsEngine statsEngine = holder.getStatsEngine();
            assertEquals(1, statsEngine.getStats("Errors/all").getCallCount());
        } finally {
            holder.close();
        }
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
        AgentBridge.getAgent().getTransaction().getWebResponse().setStatus(420);
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