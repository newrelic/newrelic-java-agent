/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.hibernate.internal;

import static com.newrelic.api.agent.weaver.Weaver.callOriginal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave
public abstract class SessionImpl {

    @NewField
    private static final String GET = "get";
    @NewField
    private static final String REFRESH = "refresh";
    @NewField
    private static final String LOAD = "load";
    @NewField
    private static final String SAVE = "save";
    @NewField
    private static final String DELETE = "delete";
    @NewField
    private static final String PERSIST = "persist";
    @NewField
    private static final String ORM = "ORM";
    @NewField
    private static final String HIBERNATE = "Hibernate";

    @Trace
    public List list(CriteriaImpl criteria) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, criteria.getEntityOrClassName(), "list");
        return callOriginal();
    }

    @Trace
    public void saveOrUpdate(String entityName, Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object),
                "saveOrUpdate");
        callOriginal();
    }

    @Trace
    public void update(String entityName, Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object), "update");
        callOriginal();
    }

    @Trace
    public Serializable save(String entityName, Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object), SAVE);
        return callOriginal();
    }

    @Trace
    public void persist(String entityName, Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object), PERSIST);
        callOriginal();
    }

    @Trace
    public void delete(Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, object.getClass().getName(), DELETE);
        callOriginal();
    }

    @Trace
    public void delete(String entityName, Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object), DELETE);
        callOriginal();
    }

    @Trace
    public void delete(String entityName, Object object, boolean isCascadeDeleteEnabled, Set transientEntities)
            throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, getModelName(entityName, object), DELETE);
        callOriginal();
    }

    @Trace
    public void load(Object object, Serializable id) throws HibernateException {
        callOriginal();
    }

    @Trace
    public Object load(String entityName, Serializable id) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, entityName, LOAD);
        return callOriginal();
    }

    @Trace
    public Object get(String entityName, Serializable id) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, entityName, GET);
        return callOriginal();
    }

    @Trace
    public void refresh(Object object) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, object.getClass().getName(), REFRESH);
        callOriginal();
    }

    @Trace
    public void refresh(Object object, LockMode lockMode) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, object.getClass().getName(), REFRESH);
        callOriginal();
    }

    @Trace
    public void refresh(Object object, Map refreshedAlready) throws HibernateException {
        NewRelic.getAgent().getTracedMethod().setMetricName(ORM, HIBERNATE, object.getClass().getName(), REFRESH);
        callOriginal();
    }

    @Trace
    public void flush() {
        NewRelic.getAgent().getTracedMethod().setMetricName(HIBERNATE, SessionImpl.class.getName(), "flush");
        callOriginal();
    }

    private String getModelName(String entityName, Object object) {
        if (entityName != null) {
            return entityName;
        } else {
            return object.getClass().getName();
        }
    }
}
