/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.instrumentation.hibernate33;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weaver;
import org.hibernate.SessionFactory;
import org.hibernate.jmx.StatisticsService;
import org.hibernate.stat.EntityStatistics;
import org.hibernate.stat.SecondLevelCacheStatistics;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class StatisticsSampler implements Runnable {

    private static final String HIBERNATE_STATISTICS = "HibernateStatistics";
    private static final String ENTITIES = HIBERNATE_STATISTICS + "/Entity";
    private static final String SECOND_LEVEL_CACHE = HIBERNATE_STATISTICS + "/SecondLevelCache";

    private final StatisticsService statisticsService;
    private final Map<String, Float> previousValues = new HashMap<>();

    public StatisticsSampler(SessionFactory sessionFactory) {
        statisticsService = new StatisticsService();
        statisticsService.setSessionFactory(sessionFactory);
        if (!statisticsService.isStatisticsEnabled()) {
            statisticsService.setStatisticsEnabled(true);
            NewRelic.getAgent().getLogger().log(Level.INFO, "Enabled Hibernate JMX metrics");
        }

        AgentBridge.instrumentation.registerCloseable(Weaver.getImplementationTitle(),
                AgentBridge.privateApi.addSampler(this, 10, TimeUnit.SECONDS));
    }

    public void recordStats() {
        recordMonoStats(HIBERNATE_STATISTICS + "/entityLoads", statisticsService.getEntityLoadCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/entityFetches", statisticsService.getEntityFetchCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/entityInserts", statisticsService.getEntityInsertCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/entityUpdates", statisticsService.getEntityUpdateCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/entityDeletes", statisticsService.getEntityDeleteCount());

        recordMonoStats(HIBERNATE_STATISTICS + "/queryCacheHits", statisticsService.getQueryCacheHitCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/queryCacheMisses", statisticsService.getQueryCacheMissCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/queryCachePuts", statisticsService.getQueryCachePutCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/queryExecutions", statisticsService.getQueryExecutionCount());

        recordMonoStats(HIBERNATE_STATISTICS + "/sessionOpens", statisticsService.getSessionOpenCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/sessionCloses", statisticsService.getSessionCloseCount());

        recordMonoStats(HIBERNATE_STATISTICS + "/transactions", statisticsService.getTransactionCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/closeStatements", statisticsService.getCloseStatementCount());
        recordMonoStats(HIBERNATE_STATISTICS + "/flushes", statisticsService.getFlushCount());

        // entity stats
        String[] entityNames = statisticsService.getEntityNames();
        for (String name : entityNames) {
            /*
             * In 3.4.x EntityStatistics is a Class, but in 3.5.x it is an Interface.
             */
            EntityStatistics stats = statisticsService.getEntityStatistics(name);
            String rootName = ENTITIES + '/' + name + '/';
            try {
                recordMonoStats(rootName + "loads", stats.getLoadCount());
                recordMonoStats(rootName + "fetches", stats.getFetchCount());
                recordMonoStats(rootName + "inserts", stats.getInsertCount());
                recordMonoStats(rootName + "updates", stats.getUpdateCount());
                recordMonoStats(rootName + "deletes", stats.getDeleteCount());
            } catch (Exception e) {
                String msg = MessageFormat.format("Error in Hibernate StatisticsSampler: {0}", e);
                // NewRelic.getAgent().getLogger().log(Level.FINEST, msg, e);
                NewRelic.getAgent().getLogger().log(Level.FINE, msg);
            }
        }

        String[] regionNames = statisticsService.getSecondLevelCacheRegionNames();
        for (String name : regionNames) {
            SecondLevelCacheStatistics cacheStatistics = statisticsService.getSecondLevelCacheStatistics(name);
            String rootName = SECOND_LEVEL_CACHE + '/' + name + '/';
            recordMonoStats(rootName + "elementsInMemory", cacheStatistics.getElementCountInMemory());
            recordMonoStats(rootName + "elementsOnDisk", cacheStatistics.getElementCountOnDisk());
            recordMonoStats(rootName + "hits", cacheStatistics.getHitCount());
            recordMonoStats(rootName + "misses", cacheStatistics.getMissCount());
            recordMonoStats(rootName + "puts", cacheStatistics.getPutCount());
        }
    }

    private void recordMonoStats(String name, float value) {
        Float previousValue = previousValues.get(name);
        previousValues.put(name, value);
        if (previousValue != null) {
            value -= previousValue;
        }

        NewRelic.recordMetric(name, value);
    }

    @Override
    public void run() {
        try {
            recordStats();
        } catch (Exception ex) {
        }
    }
}
