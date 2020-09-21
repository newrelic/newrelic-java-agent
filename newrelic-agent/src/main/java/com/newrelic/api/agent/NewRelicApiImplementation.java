/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.api.agent;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.attributes.CustomAttributeSender;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * The internal New Relic API implementation class.
 *
 * DO NOT INVOKE THIS CLASS DIRECTLY. Use {@link com.newrelic.api.agent.NewRelic}.
 */
public class NewRelicApiImplementation implements PublicApi {
    private final AttributeSender attributeSender;

    public NewRelicApiImplementation(AttributeSender sender) {
        attributeSender = sender;
    }

    public NewRelicApiImplementation() {
        this(new CustomAttributeSender());
    }

    // ************************** Error collector ***********************************//

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     *
     * @param throwable
     * @param params Custom parameters to include in the traced error. May be null. Copied to avoid side effects.
     */
    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params) {
        noticeError(throwable, params, false);
    }

    /**
     * Report an exception to New Relic.
     *
     * @param throwable
     * @see #noticeError(Throwable, Map)
     */
    @Override
    public void noticeError(Throwable throwable) {
        Map<String, String> params = Collections.emptyMap();
        noticeError(throwable, params, false);
    }

    /**
     * Notice an error and report it to New Relic. If this method is called within a transaction, the error message will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     *
     * @param message the error message
     * @param params Custom parameters to include in the traced error. May be null. Map is copied to avoid side effects.
     */
    @Override
    public void noticeError(String message, Map<String, ?> params) {
        noticeError(message, params, false);
    }

    /**
     * Report an error to New Relic.
     *
     * @param message the error message
     * @see #noticeError(String, Map)
     */
    @Override
    public void noticeError(String message) {
        Map<String, String> params = Collections.emptyMap();
        noticeError(message, params, false);
    }

    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        try {
            ServiceFactory.getRPMService().getErrorService().reportException(throwable, filterErrorAtts(params, attributeSender), expected);

            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_NOTICE_ERROR);
            if (expected) {
                MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_API_THROWABLE);
            }

            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Reported error: {0}", throwable);
                Agent.LOG.finer(msg);
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format("Exception reporting exception \"{0}\": {1}", throwable, t);
            logException(msg, t);
        }
    }

    @Override
    public void noticeError(Throwable throwable, boolean expected) {
        Map<String, String> params = Collections.emptyMap();
        noticeError(throwable, params, expected);
    }

    @Override
    public void noticeError(String message, Map<String, ?> params, boolean expected) {
        try {
            ServiceFactory.getRPMService().getErrorService().reportError(message, filterErrorAtts(params, attributeSender), expected);
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_NOTICE_ERROR);
            if (expected) {
                MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_EXPECTED_ERROR_API_MESSAGE);
            }

            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Reported error: {0}", message);
                Agent.LOG.finer(msg);
            }
        } catch (Throwable t) {
            String msg = MessageFormat.format("Exception reporting exception \"{0}\": {1}", message, t);
            logException(msg, t);
        }
    }

    @Override
    public void noticeError(String message, boolean expected) {
        Map<String, String> params = Collections.emptyMap();
        noticeError(message, params, expected);
    }

    private static Map<String, ?> filterErrorAtts(Map<String, ?> params, AttributeSender attributeSender) {
        Map<String, Object> attributes = new TreeMap<>();
        if (params == null) {
            return attributes;
        }

        int maxErrorCount = getNumberOfErrorAttsLeft();

        for (Entry<String, ?> current : params.entrySet()) {
            if (attributes.size() >= maxErrorCount) {
                // we have reached the limit on attributes added
                Agent.LOG.log(Level.FINE,
                        "Unable to add custom attribute for key \"{0}\" because the limit on error attributes has been reached.",
                        current.getKey());

                // keep iterating so that we can log all the attributes that got dropped.
                continue;
            }

            Object currentValue = current.getValue();
            String strValue = null;

            if (currentValue instanceof Number || currentValue instanceof Boolean || currentValue instanceof AtomicBoolean) {
                currentValue = attributeSender.verifyParameterAndReturnValue(current.getKey(), currentValue, "noticeError");
                if (currentValue == null) {
                    Agent.LOG.log(Level.FINE,
                            "Unable to add custom attribute for key \"{0}\" because it is not a valid type.",
                            current.getKey());
                    continue;
                }
                attributes.put(current.getKey(), currentValue);
                continue;
            }

            if (currentValue != null) {
                try {
                    strValue = currentValue.toString();
                } catch (Throwable t) {
                    Agent.LOG.log(Level.FINE,
                            "Unable to add custom attribute for key \"{0}\" because toString threw exception {1}.",
                            current.getKey(),
                            t);
                    continue;
                }
            }

            // general verification; returns null if validation failed.
            strValue = attributeSender.verifyParameterAndReturnValue(current.getKey(), strValue, "noticeError");

            if (strValue == null) {
                continue;
            }

            attributes.put(current.getKey(), strValue);
        }
        return attributes;
    }

    private static int getNumberOfErrorAttsLeft() {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            return ConfigConstant.MAX_USER_ATTRIBUTES - tx.getErrorAttributes().size();
        }
        return ConfigConstant.MAX_USER_ATTRIBUTES - 1;
    }

    // **************************** Transaction APIs ********************************//

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key
     * @param value @
     */
    @Override
    public void addCustomParameter(String key, String value) {
        attributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key
     * @param value @
     */
    @Override
    public void addCustomParameter(String key, Number value) {
        attributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key
     * @param value @
     */
    @Override
    public void addCustomParameter(String key, Boolean value) {
        attributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add key/value pairs to the current transaction. These are reported in errors and transaction traces.
     *
     * @param params
     */
    @Override
    public void addCustomParameters(Map<String, Object> params) {
        attributeSender.addAttributes(params, "addCustomParameters");
    }

    /**
     * Set the name of the current transaction.
     *
     * @param category
     * @param name The name of the transaction in URI format. example: /store/order
     */
    @Override
    public void setTransactionName(String category, String name) {
        if (StringUtils.isEmpty(category)) {
            category = MetricNames.CUSTOM;
        }
        if (name == null || name.length() == 0) {
            Agent.LOG.log(Level.FINER, "Unable to set the transaction name to an empty string");
            return;
        }
        if (!name.startsWith("/")) {
            name = "/" + name;
        }
        Transaction tx = Transaction.getTransaction(false);
        if (tx == null) {
            return;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if (dispatcher == null) {
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.finer(MessageFormat.format("Unable to set the transaction name to \"{0}\" in NewRelic API - no transaction", name));
            }
            return;
        }

        boolean isWebTransaction = dispatcher.isWebTransaction();

        TransactionNamingPolicy policy = TransactionNamingPolicy.getSameOrHigherPriorityTransactionNamingPolicy();
        com.newrelic.agent.bridge.TransactionNamePriority namePriority = MetricNames.URI.equals(category)
                ? com.newrelic.agent.bridge.TransactionNamePriority.REQUEST_URI
                : com.newrelic.agent.bridge.TransactionNamePriority.CUSTOM_HIGH;

        if (Agent.LOG.isLoggable(Level.FINER)) {
            if (policy.canSetTransactionName(tx, namePriority)) {
                String msg = MessageFormat.format("Setting {1} transaction name to \"{0}\" in NewRelic API", name, isWebTransaction ? "web" : "background");
                Agent.LOG.finer(msg);
            } else {
                Agent.LOG.finer("Unable to set the transaction name to " + name);
            }
        }
        synchronized (tx) {
            policy.setTransactionName(tx, name, category, namePriority);
        }
    }

    /**
     * Ignore the current transaction.
     */
    @Override
    public void ignoreTransaction() {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            synchronized (tx) {
                tx.setIgnore(true);
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.finer("Set ignore transaction in NewRelic API");
            }
        }
    }

    /**
     * Ignore the current transaction for calculating Apdex score.
     */
    @Override
    public void ignoreApdex() {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            synchronized (tx) {
                tx.ignoreApdex();
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                Agent.LOG.finer("Set ignore APDEX in NewRelic API");
            }
        }
    }

    @Override
    public void setRequestAndResponse(Request request, Response response) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            Agent.LOG.finest("Registering custom request dispatcher");
            tx.setRequestAndResponse(request, response);
        }
    }

    // **************************** Real User Monitoring ********************************//

    /**
     * Called by JSPs. Version 2.4.0 got the content type from the JSP. Post-2.4.0 versions get it from the
     * HttpResponse. The argument is retained for backwards-compatibility with JSPs compiled with older Agents.
     *
     * NOTE: This method is called by the AbstractRUMState class. It needs to remain static.
     *
     * @see com.newrelic.agent.tracers.jasper.GeneratorVisitTracerFactory
     */
    public static String getBrowserTimingHeaderForContentType(String contentType) {
        Transaction tx = Transaction.getTransaction(false);
        try {
            if (tx == null) {
                Agent.LOG.finer("Unable to inject browser timing header in a JSP: not running in a transaction");
                return "";
            }
            String header = null;
            synchronized (tx) {
                header = tx.getBrowserTransactionState().getBrowserTimingHeaderForJsp();
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Injecting browser timing header in a JSP: {0}", header);
                Agent.LOG.log(Level.FINER, msg);
            }
            return header;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error injecting browser timing header in a JSP: {0}", t);
            logException(msg, t);
            return "";
        }
    }

    @Override
    public String getBrowserTimingHeader() {
        Transaction tx = Transaction.getTransaction(false);
        try {
            if (tx == null) {
                Agent.LOG.finer("Unable to get browser timing header in NewRelic API: not running in a transaction");
                return "";
            }
            String header = null;
            synchronized (tx) {
                header = tx.getBrowserTransactionState().getBrowserTimingHeader();
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Got browser timing header in NewRelic API: {0}", header);
                Agent.LOG.log(Level.FINER, msg);
            }
            return header;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error getting browser timing header in NewRelic API: {0}", t);
            logException(msg, t);
            return "";
        }
    }

    /**
     * Called by JSPs. The content type is not needed for the footer since it's checked in the header API call, and a
     * footer cannot be written without a header. The argument is retained for backwards-compatibility with JSPs
     * compiled with older Agents.
     *
     * NOTE: This method is called by the AbstractRUMState class. It needs to remain static.
     *
     * @see com.newrelic.agent.tracers.jasper.GeneratorVisitTracerFactory
     */
    public static String getBrowserTimingFooterForContentType(String contentType) {
        Transaction tx = Transaction.getTransaction(false);
        try {
            if (tx == null) {
                Agent.LOG.finer("Unable to inject browser timing footer in a JSP: not running in a transaction");
                return "";
            }
            String footer = null;
            synchronized (tx) {
                footer = tx.getBrowserTransactionState().getBrowserTimingFooter();
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Injecting browser timing footer in a JSP: {0}", footer);
                Agent.LOG.log(Level.FINER, msg);
            }
            return footer;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error injecting browser timing footer in a JSP: {0}", t);
            logException(msg, t);
            return "";
        }
    }

    @Override
    public String getBrowserTimingFooter() {
        Transaction tx = Transaction.getTransaction(false);
        try {
            if (tx == null) {
                Agent.LOG.finer("Unable to get browser timing footer in NewRelic API: not running in a transaction");
                return "";
            }
            String footer = null;
            synchronized (tx) {
                footer = tx.getBrowserTransactionState().getBrowserTimingFooter();
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Got browser timing footer in NewRelic API: {0}", footer);
                Agent.LOG.log(Level.FINER, msg);
            }
            return footer;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error getting browser timing footer in NewRelic API: {0}", t);
            logException(msg, t);
            return "";
        }
    }

    /**
     * Set the user name to associate with the RUM JavaScript footer for the current web transaction.
     */
    @Override
    public void setUserName(String name) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx == null) {
            return;
        }
        Dispatcher dispatcher = tx.getDispatcher();
        if (dispatcher == null) {
            Agent.LOG.finer(MessageFormat.format("Unable to set the user name to \"{0}\" in NewRelic API - no transaction", name));
            return;
        }
        if (!dispatcher.isWebTransaction()) {
            Agent.LOG.finer(MessageFormat.format("Unable to set the user name to \"{0}\" in NewRelic API - transaction is not a web transaction", name));
            return;
        }
        if (Agent.LOG.isLoggable(Level.FINER)) {
            String msg = MessageFormat.format("Attempting to set user name to \"{0}\" in NewRelic API", name);
            Agent.LOG.finer(msg);
        }
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_USER_NAME);
        attributeSender.addAttribute("user", name, "setUserName");
    }

    /**
     * Set the account name to associate with the RUM JavaScript footer for the current web transaction.
     */
    @Override
    public void setAccountName(String name) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            Dispatcher dispatcher = tx.getDispatcher();
            if (dispatcher == null) {
                Agent.LOG.finer(MessageFormat.format("Unable to set the account name to \"{0}\" in NewRelic API - no transaction", name));
                return;
            }
            if (!dispatcher.isWebTransaction()) {
                Agent.LOG.finer(MessageFormat.format("Unable to set the account name to \"{0}\" in NewRelic API - transaction is not a web transaction", name));
                return;
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Attempting to set account name to \"{0}\" in NewRelic API", name);
                Agent.LOG.finer(msg);
            }
            attributeSender.addAttribute("account", name, "setAccountName");
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_ACCOUNT_NAME);
        }
    }

    /**
     * Set the product name to associate with the RUM JavaScript footer for the current web transaction.
     */
    @Override
    public void setProductName(String name) {
        Transaction tx = Transaction.getTransaction(false);
        if (tx != null) {
            Dispatcher dispatcher = tx.getDispatcher();
            if (dispatcher == null) {
                Agent.LOG.finer(MessageFormat.format("Unable to set the product name to \"{0}\" in NewRelic API - no transaction", name));
                return;
            }
            if (!dispatcher.isWebTransaction()) {
                Agent.LOG.finer(MessageFormat.format("Unable to set the product name to \"{0}\" in NewRelic API - transaction is not a web transaction", name));
                return;
            }
            if (Agent.LOG.isLoggable(Level.FINER)) {
                String msg = MessageFormat.format("Attempting to set product name to \"{0}\" in NewRelic API", name);
                Agent.LOG.finer(msg);
            }
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_PRODUCT_NAME);
            attributeSender.addAttribute("product", name, "setProductName");
        }
    }

    @Override
    public void setAppServerPort(int port) {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerPort(port);
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_APP_SERVER_PORT);
    }

    @Override
    public void setServerInfo(String dispatcherName, String version) {
        ServiceFactory.getEnvironmentService().getEnvironment().setServerInfo(dispatcherName, version);
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_SERVER_INFO);
    }

    @Override
    public void setInstanceName(String instanceName) {
        ServiceFactory.getEnvironmentService().getEnvironment().setInstanceName(instanceName);
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_INSTANCE_NAME);
    }

    private static void logException(String msg, Throwable t) {
        if (Agent.LOG.isLoggable(Level.FINEST)) {
            Agent.LOG.log(Level.FINEST, msg, t);
        } else if (Agent.LOG.isLoggable(Level.FINER)) {
            Agent.LOG.finer(msg);
        }
    }

    public static void initialize() {
        AgentBridge.publicApi = new NewRelicApiImplementation();
    }
}
