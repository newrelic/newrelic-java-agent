/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.hibernate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Transaction;
import org.hibernate.impl.SessionImpl;
import org.hibernate.transaction.JDBCTransaction;
import org.junit.Assert;
import org.junit.Test;

import com.newrelic.agent.AgentHelper;
import com.newrelic.agent.TransactionData;
import com.newrelic.agent.TransactionDataList;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.api.agent.Trace;

public class HibernateTest extends AHibernateTestBase {

    @Test
    public void save1() {
        _save1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/save");
    }

    @Trace(dispatcher = true)
    private void _save1() {
        Player dude = new Player();
        dude.setId(nextId++);
        session.save(null, dude);
    }

    @Test
    public void save2() {
        _save2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/save");
    }

    @Trace(dispatcher = true)
    private void _save2() {
        Player dude = createPlayer();
        session.save(dude);
    }

    @Test
    public void save3() {
        _save3();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/save");
    }

    @Trace(dispatcher = true)
    private void _save3() {
        Player dude = new Player();
        session.save(dude, nextId);
    }

    @Test
    public void saveOrUpdate1() {
        _saveOrUpdate1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/saveOrUpdate");
    }

    @Trace(dispatcher = true)
    private void _saveOrUpdate1() {
        Player dude = new Player();
        dude.setId(nextId++);
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(dude);
        transaction.commit();
    }

    @Test
    public void saveOrUpdate2() {
        _saveOrUpdate2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/saveOrUpdate");
    }

    @Trace(dispatcher = true)
    private void _saveOrUpdate2() {
        Player dude = new Player();
        dude.setId(nextId++);
        session.saveOrUpdate(Player.class.getName(), dude);
    }

    @Test
    public void persist1() {
        callPersist1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/persist");
    }

    @Trace(dispatcher = true)
    private void callPersist1() {
        Player dude = new Player();
        dude.setId(nextId++);
        session.persist(dude);
    }

    @Test
    public void persist2() {
        _persist2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/persist");
    }

    @Trace(dispatcher = true)
    private void _persist2() {
        Player dude = new Player();
        dude.setId(nextId++);
        session.persist(Player.class.getName(), dude);
    }

    @Test
    public void update1() {
        _update1();

        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/update");
    }

    @Trace(dispatcher = true)
    private void _update1() {
        Serializable id = savePlayer();
        Player player = (Player) session.get(Player.class, id);
        session.update(player);
    }

    @Test
    public void update2() {
        _update2();

        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/update");
    }

    @Trace(dispatcher = true)
    private void _update2() {
        Serializable id = savePlayer();
        Player player = (Player) session.get(Player.class, id);
        session.update(Player.class.getName(), player);
    }

    @Test
    public void load1() {
        _load1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/load");
    }

    @Trace(dispatcher = true)
    private void _load1() {
        Serializable id = savePlayer();

        session.load(Player.class, id);
    }

    @Test
    public void load2() {
        _load2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/load");
    }

    @Trace(dispatcher = true)
    private void _load2() {
        Serializable id = savePlayer();

        session.load(Player.class.getName(), nextId);
    }

    @Test
    public void get1() {
        _get1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/get");
    }

    @Trace(dispatcher = true)
    private void _get1() {
        Serializable id = savePlayer();

        session.get(Player.class, id);
    }

    @Test
    public void get2() {
        _get2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/get");
    }

    @Trace(dispatcher = true)
    private void _get2() {
        Serializable id = savePlayer();

        session.get(Player.class.getName(), id);
    }

    @Test
    public void delete1() {
        _delete1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/delete");
    }

    @Trace(dispatcher = true)
    private void _delete1() {
        Serializable id = savePlayer();

        Object player = session.get(Player.class.getName(), id);
        session.delete(player);
    }

    @Test
    public void delete2() {
        _delete2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/delete");
    }

    @Trace(dispatcher = true)
    private void _delete2() {
        Serializable id = savePlayer();

        Object player = session.get(Player.class.getName(), id);
        session.delete(Player.class.getName(), player);
    }

    @Test
    public void list() {
        callList();
        verifyScopedMetrics("ORM/Hibernate/" + Game.class.getName() + "/list");
    }

    @Trace(dispatcher = true)
    private void callList() {
        Criteria criteria = session.createCriteria(Game.class);
        Assert.assertTrue(criteria.list().isEmpty());
    }

    @Test
    public void fullTransaction() {
        List<TransactionData> list = TransactionDataList.getTransactions(new Runnable() {
            public void run() {
                executeTransaction();
            }
        });

        Assert.assertEquals(1, list.size());

        TransactionData transactionData = list.get(0);
        Collection<Tracer> children = AgentHelper.getChildren(transactionData.getRootTracer());
        Assert.assertEquals(3, children.size());

        Iterator<Tracer> iterator = children.iterator();
        Assert.assertEquals("ORM/Hibernate/test.newrelic.test.agent.hibernate.Game/list",
                iterator.next().getMetricName());
        Assert.assertEquals("ORM/Hibernate/test.newrelic.test.agent.hibernate.Player/save",
                iterator.next().getMetricName());
        Assert.assertEquals("ORM/Hibernate/test.newrelic.test.agent.hibernate.Player/load",
                iterator.next().getMetricName());
    }

    /*
     * @Test public void query() { List list = session.createQuery("from Player").list(); verifyMetrics("ORM/Hibernate/"
     * + Game.class.getName() + "/list"); }
     */

    @Trace(dispatcher = true)
    private void executeTransaction() {
        Criteria criteria = session.createCriteria(Game.class);
        Assert.assertTrue(criteria.list().isEmpty());

        Serializable id = savePlayer();

        session.load(Player.class, id);
    }

    @Test
    public void refresh1() {
        _refresh1();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/refresh");
    }

    @Trace(dispatcher = true)
    private void _refresh1() {
        Transaction transaction = session.beginTransaction();
        Serializable id = savePlayer();
        transaction.commit();
        Object player = session.get(Player.class, id);
        session.refresh(player);
    }

    @Test
    public void refresh2() {
        _refresh2();
        verifyScopedMetrics("ORM/Hibernate/" + Player.class.getName() + "/refresh", "Hibernate/"
                + JDBCTransaction.class.getName() + "/commit");
    }

    @Trace(dispatcher = true)
    private void _refresh2() {
        Transaction transaction = session.beginTransaction();
        Serializable id = savePlayer();
        transaction.commit();
        Object player = session.get(Player.class, id);
        session.refresh(player, LockMode.NONE);
    }

    @Test
    public void rollback() {
        _rollback();
        verifyScopedMetrics("Hibernate/" + JDBCTransaction.class.getName() + "/rollback");
    }

    @Trace(dispatcher = true)
    private void _rollback() {
        Transaction transaction = session.beginTransaction();
        transaction.rollback();
    }

    @Test
    public void flush() {
        callFlush();
        verifyScopedMetrics("Hibernate/" + SessionImpl.class.getName() + "/flush");
    }

    @Trace(dispatcher = true)
    private void callFlush() {
        session.flush();
    }

    private Serializable savePlayer() {
        Player dude = createPlayer();
        return session.save(dude);
    }

    private Player createPlayer() {
        Player dude = new Player();
        dude.setId(nextId);
        return dude;
    }

    private void verifyScopedMetrics(String... metric) {
        Set<String> metrics = AgentHelper.getMetrics(true);
        AgentHelper.verifyMetrics(metrics, metric);
    }

    private void verifyMetrics(String... metric) {
        Set<String> metrics = AgentHelper.getMetrics(false);
        AgentHelper.verifyMetrics(metrics, metric);
    }
}
