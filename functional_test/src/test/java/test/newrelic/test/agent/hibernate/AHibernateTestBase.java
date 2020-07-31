/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package test.newrelic.test.agent.hibernate;

import com.newrelic.agent.DatabaseHelper;
import com.newrelic.api.agent.Trace;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.junit.After;
import org.junit.Before;

public class AHibernateTestBase {
    protected static SessionFactory sessionFactory;
    protected Session session;
    protected long nextId;

    @Trace(dispatcher = true)
    @Before
    public void setup() {

        Configuration config = new AnnotationConfiguration()
                .setProperty("hibernate.dialect", "org.hibernate.dialect.DerbyDialect")
                .setProperty("hibernate.connection.driver_class", DatabaseHelper.DERBY_DATABASE_DRIVER)
                .setProperty("hibernate.connection.url", DatabaseHelper.getConnectionUrl(getClass()) + ";create=true")
                .setProperty("hibernate.connection.pool_size", "1")
                .setProperty("hibernate.connection.autocommit", "true")
                .setProperty("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "true")
                .addAnnotatedClass(Player.class)
                .addAnnotatedClass(Game.class);

        sessionFactory = config.buildSessionFactory();

        session = sessionFactory.openSession();
        nextId++;
    }

    @After
    public void teardown() {
        if (session != null) {
            session.close();
        }
    }
}
