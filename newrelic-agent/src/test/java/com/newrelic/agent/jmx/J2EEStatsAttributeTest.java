/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.jmx;

import com.newrelic.agent.stats.StatsEngine;
import com.newrelic.agent.stats.StatsEngineImpl;
import com.newrelic.agent.stats.StatsImpl;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import javax.management.Attribute;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.j2ee.statistics.BoundaryStatistic;
import javax.management.j2ee.statistics.BoundedRangeStatistic;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.JCAConnectionPoolStats;
import javax.management.j2ee.statistics.JCAConnectionStats;
import javax.management.j2ee.statistics.JCAStats;
import javax.management.j2ee.statistics.JDBCConnectionPoolStats;
import javax.management.j2ee.statistics.JDBCConnectionStats;
import javax.management.j2ee.statistics.JDBCStats;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.TimeStatistic;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.times;

public class J2EEStatsAttributeTest {

    @Test
    public void processStatistic_count() {
        CountStatistic statistic = Mockito.mock(CountStatistic.class);
        Mockito.when(statistic.getName()).thenReturn("CoolCount");
        Mockito.when(statistic.getCount()).thenReturn(66L);

        StatsImpl stat = new StatsImpl(0, 0, 0, 0, 0);

        StatsEngine statsEngine = Mockito.mock(StatsEngine.class);
        Mockito.when(statsEngine.getStats("Test/Metric/CoolCount")).thenReturn(stat);

        J2EEStatsAttributeProcessor.processStatistic(statsEngine, "Test/Metric", new Attribute("CoolCount", -32), statistic);

        Mockito.verify(statsEngine, times(1)).getStats("Test/Metric/CoolCount");

        Assert.assertEquals(66, stat.getTotal(), .0001);
    }

    @Test
    public void processStatisticCountTest() {
        CountStatistic count = new CountStatistic() {

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public String getName() {
                return "LiveCount";
            }

            @Override
            public long getLastSampleTime() {
                return 0;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public long getCount() {
                return 55;
            }
        };
        Attribute att = new Attribute("stats.LiveCount", count);
        StatsEngine statsEngine = new StatsEngineImpl();
        J2EEStatsAttributeProcessor.processStatistic(statsEngine, "Jmx/Test", att, count);
        Assert.assertEquals(55, statsEngine.getStats("Jmx/Test/LiveCount").getTotal(), .001);

    }

    @Test
    public void processStatRangeTest() {
        RangeStatistic count = new RangeStatistic() {

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public String getName() {
                return "LiveCount";
            }

            @Override
            public long getLastSampleTime() {
                return 0;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public long getLowWaterMark() {
                return 0;
            }

            @Override
            public long getHighWaterMark() {
                return 10;
            }

            @Override
            public long getCurrent() {
                return 9;
            }
        };
        Attribute att = new Attribute("stats.LiveCount", count);
        StatsEngine statsEngine = new StatsEngineImpl();
        J2EEStatsAttributeProcessor.processStatistic(statsEngine, "Jmx/Test", att, count);
        Assert.assertEquals(9, statsEngine.getStats("Jmx/Test/LiveCount").getTotal(), .001);

    }

    @Test
    public void addJmxValueCountTest() {
        CountStatistic count = new CountStatistic() {

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public String getName() {
                return "LiveCount";
            }

            @Override
            public long getLastSampleTime() {
                return 0;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public long getCount() {
                return 55;
            }
        };
        Map<String, Float> values = new HashMap<>();
        Attribute att = new Attribute("stats.LiveCount", count);
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertEquals(55, values.get("stats.LiveCount"), .001);

        att = new Attribute("LiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));

        att = new Attribute("stats.ActiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));
        Assert.assertNull(values.get("stats.ActiveCount"));

    }

    @Test
    public void addJmxValueRangeTest() {
        RangeStatistic count = new RangeStatistic() {

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public String getName() {
                return "LiveCount";
            }

            @Override
            public long getLastSampleTime() {
                return 0;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public long getLowWaterMark() {
                return 0;
            }

            @Override
            public long getHighWaterMark() {
                return 10;
            }

            @Override
            public long getCurrent() {
                return 9;
            }
        };
        Map<String, Float> values = new HashMap<>();
        Attribute att = new Attribute("stats.LiveCount", count);
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertEquals(9, values.get("stats.LiveCount"), .001);

        att = new Attribute("LiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));

        att = new Attribute("stats.ActiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));
        Assert.assertNull(values.get("stats.ActiveCount"));

    }

    @Test
    public void addJmxValueBoundaryTest() {
        BoundaryStatistic count = new BoundaryStatistic() {

            @Override
            public String getUnit() {
                return null;
            }

            @Override
            public long getStartTime() {
                return 0;
            }

            @Override
            public String getName() {
                return "LiveCount";
            }

            @Override
            public long getLastSampleTime() {
                return 0;
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public long getUpperBound() {
                return 10;
            }

            @Override
            public long getLowerBound() {
                return 0;
            }
        };
        Map<String, Float> values = new HashMap<>();
        Attribute att = new Attribute("stats.LiveCount", count);
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertEquals(5, values.get("stats.LiveCount"), .001);

        att = new Attribute("LiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));

        att = new Attribute("stats.ActiveCount", count);
        values.clear();
        J2EEStatsAttributeProcessor.addJmxValue(att, count, values);
        Assert.assertNull(values.get("stats.LiveCount"));
        Assert.assertNull(values.get("stats.ActiveCount"));

    }

    @Test
    public void addJmxValueJdbcTest() throws MalformedObjectNameException {
        JDBCStats count = new JDBCStats() {

            @Override
            public Statistic[] getStatistics() {
                return new Statistic[0];
            }

            @Override
            public String[] getStatisticNames() {
                return null;
            }

            @Override
            public Statistic getStatistic(String statisticName) {
                return null;
            }

            @Override
            public JDBCConnectionStats[] getConnections() {
                return new JDBCConnectionStats[0];
            }

            @Override
            public JDBCConnectionPoolStats[] getConnectionPools() {
                return new JDBCConnectionPoolStats[] { new TestJdbcConnectionPool() };
            }
        };

        Attribute att = new Attribute("stats", count);
        J2EEStatsAttributeProcessor processor = new J2EEStatsAttributeProcessor();
        StatsEngine engine = new StatsEngineImpl();
        processor.process(engine, new ObjectInstance("tezt:type=1", "test"), att, "JMX/Test",
                new HashMap<String, Float>());

        // check count stats
        Assert.assertEquals(77, engine.getStats("JMX/Test/Create").getTotal(), .001);
        Assert.assertEquals(1, engine.getStats("JMX/Test/Create").getCallCount(), .001);
        Assert.assertEquals(9, engine.getStats("JMX/Test/Close").getTotal(), .001);
        Assert.assertEquals(1, engine.getStats("JMX/Test/Close").getCallCount(), .001);

        // check time stats
        Assert.assertEquals(12, engine.getResponseTimeStats("JMX/Test/WaitTime").getMaxCallTime(), .001);
        Assert.assertEquals(8, engine.getResponseTimeStats("JMX/Test/WaitTime").getMinCallTime(), .001);
        Assert.assertEquals(2, engine.getResponseTimeStats("JMX/Test/WaitTime").getCallCount(), .001);
        Assert.assertEquals(6, engine.getResponseTimeStats("JMX/Test/UserTime").getMaxCallTime(), .001);
        Assert.assertEquals(4, engine.getResponseTimeStats("JMX/Test/UserTime").getMinCallTime(), .001);
        Assert.assertEquals(2, engine.getResponseTimeStats("JMX/Test/UserTime").getCallCount(), .001);

    }

    @Test
    public void addJcaValueJdbcTest() throws MalformedObjectNameException {
        JCAStats count = new JCAStats() {

            @Override
            public JCAConnectionStats[] getConnections() {
                return new JCAConnectionStats[0];
            }

            @Override
            public JCAConnectionPoolStats[] getConnectionPools() {
                return new JCAConnectionPoolStats[] { new TestJcaConnectionPool() };
            }

            @Override
            public Statistic getStatistic(String statisticName) {
                return null;
            }

            @Override
            public String[] getStatisticNames() {
                return null;
            }

            @Override
            public Statistic[] getStatistics() {
                return new Statistic[0];
            }
        };

        Attribute att = new Attribute("stats", count);
        J2EEStatsAttributeProcessor processor = new J2EEStatsAttributeProcessor();
        StatsEngine engine = new StatsEngineImpl();
        processor.process(engine, new ObjectInstance("tezt:type=2", "test"), att, "JMX/Test",
                new HashMap<String, Float>());

        // check count stats
        Assert.assertEquals(6, engine.getStats("JMX/Test/Create").getTotal(), .001);
        Assert.assertEquals(1, engine.getStats("JMX/Test/Create").getCallCount(), .001);
        Assert.assertEquals(99, engine.getStats("JMX/Test/Closed").getTotal(), .001);
        Assert.assertEquals(1, engine.getStats("JMX/Test/Closed").getCallCount(), .001);

        // check time stats
        Assert.assertEquals(10, engine.getResponseTimeStats("JMX/Test/Wait").getMaxCallTime(), .001);
        Assert.assertEquals(5, engine.getResponseTimeStats("JMX/Test/Wait").getMinCallTime(), .001);
        Assert.assertEquals(2, engine.getResponseTimeStats("JMX/Test/Wait").getCallCount(), .001);
        Assert.assertEquals(6, engine.getResponseTimeStats("JMX/Test/User").getMaxCallTime(), .001);
        Assert.assertEquals(4, engine.getResponseTimeStats("JMX/Test/User").getMinCallTime(), .001);
        Assert.assertEquals(2, engine.getResponseTimeStats("JMX/Test/User").getCallCount(), .001);

    }

    static class TestJcaConnectionPool implements JCAConnectionPoolStats {

        @Override
        public String getConnectionFactory() {
            return null;
        }

        @Override
        public String getManagedConnectionFactory() {
            return null;
        }

        @Override
        public TimeStatistic getWaitTime() {
            return new TestTimeStat("Wait", 5, 10);
        }

        @Override
        public TimeStatistic getUseTime() {
            return new TestTimeStat("User", 4, 6);
        }

        @Override
        public Statistic getStatistic(String statisticName) {
            return null;
        }

        @Override
        public String[] getStatisticNames() {
            return null;
        }

        @Override
        public Statistic[] getStatistics() {
            return new Statistic[] { getCloseCount(), getCreateCount(), getUseTime(), getWaitTime() };
        }

        @Override
        public CountStatistic getCloseCount() {
            return new TestCountStat("Closed", 99);
        }

        @Override
        public CountStatistic getCreateCount() {
            return new TestCountStat("Create", 6);
        }

        @Override
        public BoundedRangeStatistic getFreePoolSize() {
            return null;
        }

        @Override
        public BoundedRangeStatistic getPoolSize() {
            return null;
        }

        @Override
        public RangeStatistic getWaitingThreadCount() {
            return null;
        }

    }

    static class TestJdbcConnectionPool implements JDBCConnectionPoolStats {

        @Override
        public String getJdbcDataSource() {
            return null;
        }

        @Override
        public TimeStatistic getWaitTime() {
            return new TestTimeStat("WaitTime", 8, 12);
        }

        @Override
        public TimeStatistic getUseTime() {
            return new TestTimeStat("UserTime", 4, 6);
        }

        @Override
        public Statistic getStatistic(String statisticName) {
            return null;
        }

        @Override
        public String[] getStatisticNames() {
            return null;
        }

        @Override
        public Statistic[] getStatistics() {
            return new Statistic[] { getWaitTime(), getUseTime(), getCreateCount(), getCloseCount() };
        }

        @Override
        public CountStatistic getCreateCount() {
            return new TestCountStat("Create", 77);
        }

        @Override
        public CountStatistic getCloseCount() {
            return new TestCountStat("Close", 9);
        }

        @Override
        public BoundedRangeStatistic getPoolSize() {
            return null;
        }

        @Override
        public BoundedRangeStatistic getFreePoolSize() {
            return null;
        }

        @Override
        public RangeStatistic getWaitingThreadCount() {
            return null;
        }

    }

    static class TestCountStat implements CountStatistic {

        private final long value;
        private final String name;

        public TestCountStat(String pName, long count) {
            name = pName;
            value = count;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUnit() {
            return null;
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public long getStartTime() {
            return 0;
        }

        @Override
        public long getLastSampleTime() {
            return 0;
        }

        @Override
        public long getCount() {
            return value;
        }

    }

    static class TestTimeStat implements TimeStatistic {

        private final long time = System.currentTimeMillis();
        private final long min;
        private final long max;
        private final String name;

        public TestTimeStat(String pName, long mmin, long mmax) {
            name = pName;
            min = mmin;
            max = mmax;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getUnit() {
            return "SECOND";
        }

        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public long getStartTime() {
            return time;
        }

        @Override
        public long getLastSampleTime() {
            return time;
        }

        @Override
        public long getCount() {
            return 2;
        }

        @Override
        public long getMaxTime() {
            return max;
        }

        @Override
        public long getMinTime() {
            return min;
        }

        @Override
        public long getTotalTime() {
            return min + max;
        }
    }
}
