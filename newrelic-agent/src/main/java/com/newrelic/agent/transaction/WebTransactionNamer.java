/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transaction;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.service.ServiceFactory;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * {@link TransactionNamer} implementation for web transactions.
 */
public class WebTransactionNamer extends AbstractTransactionNamer {

    private WebTransactionNamer(Transaction tx, String requestUri) {
        super(tx, requestUri);
    }

    @Override
    public void setTransactionName() {
        if (!canSetTransactionName(TransactionNamePriority.STATUS_CODE)) {
            return;
        }

        Transaction tx = getTransaction();
        int responseStatusCode = tx.getStatus();
        if (responseStatusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            String normalizedStatus = normalizeStatus(responseStatusCode);
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using response status",
                        normalizedStatus);
                Agent.LOG.finer(msg);
            }
            if (canSetTransactionName(TransactionNamePriority.STATUS_CODE)) {
                setTransactionName(normalizedStatus, MetricNames.NORMALIZED_URI, TransactionNamePriority.STATUS_CODE);
                tx.freezeStatus();
            }
            return;
        }

        if (!canSetTransactionName()) {
            return;
        }

        String requestUri = getUri();

        if (requestUri == null) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" because request uri is null",
                        MetricNames.UNKNOWN);
                Agent.LOG.finer(msg);
            }
            setTransactionName(MetricNames.UNKNOWN, MetricNames.NORMALIZED_URI, TransactionNamePriority.REQUEST_URI);
            return;
        }

        String appName = tx.getPriorityApplicationName().getName();
        String normalizedUri = ServiceFactory.getNormalizationService().getUrlNormalizer(appName).normalize(requestUri);
        if (normalizedUri == null) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = "Ignoring transaction because normalized request uri is null";
                Agent.LOG.finer(msg);
            }
            tx.setIgnore(true);
            return;
        }
        if (normalizedUri.equals(requestUri)) {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using request uri", requestUri);
                Agent.LOG.finer(msg);
            }
            setTransactionName(requestUri, MetricNames.URI, TransactionNamePriority.REQUEST_URI);
            return;
        } else {
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Setting transaction name to \"{0}\" using normalized request uri",
                        normalizedUri);
                Agent.LOG.finer(msg);
            }
            setTransactionName(normalizedUri, MetricNames.NORMALIZED_URI, TransactionNamePriority.REQUEST_URI);
        }
    }

    private static String normalizeStatus(int responseStatus) {
        return "/" + String.valueOf(responseStatus) + "/*";
    }

    public static TransactionNamer create(Transaction tx, String requestUri) {
        return new WebTransactionNamer(tx, requestUri);
    }
}
