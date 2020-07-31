/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.hibernate;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.ConfigService;
import com.newrelic.agent.samplers.NoopSamplerService;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceManager;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.Stats;
import com.newrelic.api.agent.Trace;

import org.hibernate.Criteria;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class HibernateStatsTest extends AHibernateTestBase {

    protected static List<Runnable> samplers = new ArrayList<>();

    protected static void runSamplers() {
        for (Runnable r : samplers) {
            r.run();
        }
    }

    @BeforeClass
    public static void beforeClass() {
        final NoopSamplerService samplerService = new NoopSamplerService() {
            @Override
            public Closeable addSampler(Runnable sampler, long period, TimeUnit timeUnit) {
                samplers.add(sampler);
                return null;
            }
        };

        ConfigService configService = spy(ServiceFactory.getConfigService());
        AgentConfigImpl agentConfig = spy((AgentConfigImpl) configService.getDefaultAgentConfig());

        doReturn(true).when(agentConfig).getValue("instrumentation.hibernate.stats_sampler.enabled", false);
        doReturn(agentConfig).when(configService).getDefaultAgentConfig();

        ServiceManager serviceManager = spy(ServiceFactory.getServiceManager());
        doReturn(samplerService).when(serviceManager).getSamplerService();
        doReturn(configService).when(serviceManager).getConfigService();
        ServiceFactory.setServiceManager(serviceManager);
    }

    @Override
    @Trace(dispatcher = true)
    @Before
    public void setup() {
        samplers.clear();

        super.setup();
    }

    @Trace(dispatcher = true)
    @Test
    public void insertsAndLoads() {
        runSamplers();
        getTransactionStats().clear();

        Transaction transaction = session.beginTransaction();
        Player dude = new Player();
        dude.setId(nextId++);

        session.save(null, dude);
        Game game = new Game();
        game.setId(nextId++);

        session.save(null, game);

        session.load(Player.class, dude.getId());
        transaction.commit();

        Session session2 = sessionFactory.openSession();
        Criteria criteria = session2.createCriteria(Player.class);
        assertEquals(1, criteria.list().size());

        // getTransactionStats().clear();

        runSamplers();

        SimpleStatsEngine statsEngine = getTransactionStats();

        Stats stats = statsEngine.getStats("HibernateStatistics/Entity/test.newrelic.test.agent.hibernate.Player/loads");
        assertEquals(1f, stats.getTotal(), 0);
        statsEngine.getStats("HibernateStatistics/entityLoads");
        assertEquals(1f, stats.getTotal(), 0);
        stats = statsEngine.getStats("HibernateStatistics/Entity/test.newrelic.test.agent.hibernate.Player/inserts");
        assertEquals(1f, stats.getTotal(), 0);
        stats = statsEngine.getStats("HibernateStatistics/entityInserts");
        assertEquals(2f, stats.getTotal(), 0);

        stats = statsEngine.getStats("HibernateStatistics/Entity/test.newrelic.test.agent.hibernate.Player/deletes");
        assertEquals(1, stats.getCallCount());
        assertEquals(0f, stats.getTotal(), 0);

        statsEngine = getTransactionStats();
        statsEngine.clear();
        runSamplers();
        stats = statsEngine.getStats("HibernateStatistics/Entity/test.newrelic.test.agent.hibernate.Player/inserts");
        assertEquals(1, stats.getCallCount());
        assertEquals(0f, stats.getTotal(), 0);
    }

    private SimpleStatsEngine getTransactionStats() {
        return com.newrelic.agent.Transaction.getTransaction().getTransactionActivity().getTransactionStats().getUnscopedStats();
    }

    @Trace(dispatcher = true)
    @Test
    public void transactions() {
        float count = 10;
        for (int i = 0; i < count; i++) {
            Transaction tx = session.beginTransaction();
            tx.commit();
        }

        runSamplers();
        Stats stats = getTransactionStats().getStats("HibernateStatistics/transactions");
        assertEquals(count, stats.getTotal(), 0);
    }

    @Trace(dispatcher = true)
    @Test
    public void flushes() {
        float count = 10;
        for (int i = 0; i < count; i++) {
            Player dude = new Player();
            dude.setId(nextId++);

            session.save(null, dude);

            session.flush();
        }

        runSamplers();
        Stats stats = getTransactionStats().getStats("HibernateStatistics/flushes");
        assertEquals(count, stats.getTotal(), 0);
    }

    @Trace(dispatcher = true)
    @Test
    public void sessionStats() {
        runSamplers();
        getTransactionStats().clear();
        float count = 10;
        for (int i = 0; i < count; i++) {
            Session openSession = sessionFactory.openSession();
            openSession.close();
        }
        Session openSession = sessionFactory.openSession();
        runSamplers();

        Stats stats = getTransactionStats().getStats("HibernateStatistics/sessionOpens");
        assertEquals(count + 1, stats.getTotal(), 0);
        stats = getTransactionStats().getStats("HibernateStatistics/sessionCloses");
        assertEquals(count, stats.getTotal(), 0);

        openSession.close();
    }
}
