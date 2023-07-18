/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.browser;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A class to get the Real User Monitoring JavaScript header and footer for a transaction from the @link{BeaconConfig}.
 * 
 * This class is thread-safe.
 */
public class BrowserTransactionStateImpl implements BrowserTransactionState {

    // Thread safety: public accessors that touch this object's state are synchronized. There are numerous public
    // accessors that just forwards requests through to the Transaction; these aren't synchronized here, except
    // when the manipulate the results (e.g. getAgentAttributes()).

    private final Object lock = new Object();
    private final Transaction tx;
    private boolean browserHeaderRendered;
    private boolean browserFooterRendered;

    protected BrowserTransactionStateImpl(Transaction tx) {
        this.tx = tx;
    }

    @Override
    public String getBrowserTimingHeaderForJsp() {
        synchronized (lock) {
            if (!canRenderHeaderForJsp()) {
                return "";
            }
            return getBrowserTimingHeader2();
        }
    }

    @Override
    public String getBrowserTimingHeader() {
        synchronized (lock) {
            if (!canRenderHeader()) {
                return "";
            }
            return getBrowserTimingHeader2();
        }
    }

    @Override
    public String getBrowserTimingHeader(String nonce) {
        synchronized (lock) {
            if (!canRenderHeader()) {
                return "";
            }
            return getBrowserTimingHeader2(nonce);
        }
    }

    private String getBrowserTimingHeader2() {
        BrowserConfig config = getBeaconConfig();
        if (config == null) {
            return onNoBrowserConfig();
        }
        onBrowserHeaderObtained();
        return config.getBrowserTimingHeader();
    }

    private String getBrowserTimingHeader2(String nonce) {
        BrowserConfig config = getBeaconConfig();
        if (config == null) {
            return onNoBrowserConfig();
        }
        onBrowserHeaderObtained();
        return config.getBrowserTimingHeader(nonce);
    }

    private void onBrowserHeaderObtained() {
        browserHeaderRendered = true;
    }

    private String onNoBrowserConfig() {
        Agent.LOG.finer("Real user monitoring is disabled");
        return "";
    }

    @Override
    public String getBrowserTimingFooter() {
        synchronized (lock) {
            if (!canRenderFooter()) {
                return "";
            }
            return getBrowserTimingFooter2();
        }
    }

    @Override
    public String getBrowserTimingFooter(String nonce) {
        synchronized (lock) {
            if (!canRenderFooter()) {
                return "";
            }
            return getBrowserTimingFooter2(nonce);
        }
    }

    private String onBrowserFooterObtained(String value) {
        if (!value.isEmpty()) {
            browserFooterRendered = true;
        }
        return value;
    }

    private boolean prepareToObtainFooter() {
        // this has the side-effect of possibly ignoring the transaction
        tx.freezeTransactionName();
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction is ignore");
            return false;
        } else {
            return true;
        }
    }

    private String getBrowserTimingFooter2(String nonce) {
        BrowserConfig config = getBeaconConfig();
        if (config == null) {
            return onNoBrowserConfig();
        }
        if(!prepareToObtainFooter()) {
            return "";
        }
        return onBrowserFooterObtained(config.getBrowserTimingFooter(this, nonce));
    }

    private String getBrowserTimingFooter2() {
        BrowserConfig config = getBeaconConfig();
        if (config == null) {
            return onNoBrowserConfig();
        }
        if(!prepareToObtainFooter()) {
            return "";
        }
        return onBrowserFooterObtained(config.getBrowserTimingFooter(this));
    }

    private boolean canRenderHeader() {
        if (!tx.isInProgress()) {
            Agent.LOG.finer("Unable to get browser timing header: transaction has no tracers");
            return false;
        }
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing header: transaction is ignore");
            return false;
        }
        if (browserHeaderRendered) {
            Agent.LOG.finer("browser timing header already rendered");
            return false;
        }
        return true;
    }

    private boolean canRenderHeaderForJsp() {
        if (!canRenderHeader()) {
            return false;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if (dispatcher == null || !dispatcher.isWebTransaction()) {
            Agent.LOG.finer("Unable to get browser timing header: transaction is not a web transaction");
            return false;
        }

        String contentType = dispatcher.getResponse().getContentType();
        if (!isHtml(contentType)) {
            String msg = MessageFormat.format(
                    "Unable to inject browser timing header in a JSP: bad content type: {0}", contentType);
            Agent.LOG.finer(msg);
            return false;
        }

        return true;
    }

    private boolean isHtml(String contentType) {
        return contentType != null && (contentType.startsWith("text/html") || contentType.startsWith("text/xhtml"));
    }

    private boolean canRenderFooter() {
        if (!tx.isInProgress()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction has no tracers");
            return false;
        }
        if (tx.isIgnore()) {
            Agent.LOG.finer("Unable to get browser timing footer: transaction is ignore");
            return false;
        }
        if (browserFooterRendered && !tx.getAgentConfig().getBrowserMonitoringConfig().isAllowMultipleFooters()) {
            Agent.LOG.finer("browser timing footer already rendered");
            return false;
        }
        if (browserHeaderRendered) {
            return true;
        }
        BrowserConfig config = getBeaconConfig();
        if (config == null) {
            Agent.LOG.finer("Real user monitoring is disabled");
            return false;
        }
        Agent.LOG.finer("getBrowserTimingFooter() was invoked without a call to getBrowserTimingHeader()");
        return false;
    }

    protected BrowserConfig getBeaconConfig() {
        String appName = tx.getApplicationName();
        return ServiceFactory.getBeaconService().getBrowserConfig(appName);
    }

    /**
     * Get the duration of the transaction (so far) in milliseconds.
     * 
     * @return the elapsed time since the start of the transaction
     */
    @Override
    public long getDurationInMilliseconds() {
        return TimeUnit.MILLISECONDS.convert(tx.getRunningDurationInNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public long getExternalTimeInMilliseconds() {
        return tx.getExternalTime();
    }

    @Override
    public String getTransactionName() {
        return tx.getPriorityTransactionName().getName();
    }

    public static BrowserTransactionState create(Transaction tx) {
        return tx == null ? null : new BrowserTransactionStateImpl(tx);
    }

    @Override
    public Map<String, Object> getUserAttributes() {
        return tx.getUserAttributes();
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        synchronized (lock) {
            Map<String, Object> atts = new HashMap<>();
            atts.putAll(tx.getAgentAttributes());
            atts.putAll(AttributesUtils.appendAttributePrefixes(tx.getPrefixedAgentAttributes()));
            return atts;
        }
    }

    @Override
    public String getAppName() {
        return tx.getApplicationName();
    }
}
