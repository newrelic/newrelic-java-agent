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
import com.newrelic.agent.attributes.AgentAttributeSender;
import com.newrelic.agent.attributes.AttributeSender;
import com.newrelic.agent.attributes.CustomAttributeSender;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.config.ConfigConstant;
import com.newrelic.agent.config.ExpectedErrorConfig;
import com.newrelic.agent.dispatchers.Dispatcher;
import com.newrelic.agent.errors.ErrorGroupCallbackHolder;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.transaction.TransactionNamingPolicy;
import org.apache.commons.lang3.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * The internal New Relic API implementation class.
 * <p>
 * DO NOT INVOKE THIS CLASS DIRECTLY. Use {@link com.newrelic.api.agent.NewRelic}.
 */
public class NewRelicApiImplementation implements PublicApi {
    private final AttributeSender customAttributeSender;

    private final AgentAttributeSender agentAttributeSender;

    public NewRelicApiImplementation(AttributeSender customSender, AgentAttributeSender agentSender) {
        customAttributeSender = customSender;
        agentAttributeSender = agentSender;
    }

    public NewRelicApiImplementation() {
        this(new CustomAttributeSender(), new AgentAttributeSender());
    }

    // ************************** Error collector ***********************************//

    /**
     * Notice an exception and report it to New Relic. If this method is called within a transaction, the exception will
     * be reported with the transaction when it finishes. If it is invoked outside of a transaction, a traced error will
     * be created and reported to New Relic.
     *
     * @param throwable
     * @param params    Custom parameters to include in the traced error. May be null. Copied to avoid side effects.
     */
    @Override
    public void noticeError(Throwable throwable, Map<String, ?> params) {
        noticeError(throwable, params, isExpectedErrorConfigured(throwable));
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
        noticeError(throwable, params, isExpectedErrorConfigured(throwable));
    }

    public void noticeError(Throwable throwable, Map<String, ?> params, boolean expected) {
        try {
            ServiceFactory.getRPMService().getErrorService().reportException(throwable, filterErrorAtts(params, customAttributeSender), expected);

            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_NOTICE_ERROR);
            //SUPPORTABILITY_API_EXPECTED_ERROR_API_THROWABLE metric is intended to be recorded independent of whether
            //the expected error was defined in config or via the actual api.  Read simply as the call came through the noticeError API (SUPPORTABILITY_API_NOTICE_ERROR)
            //and it was expected.
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
    public void noticeError(String message, Map<String, ?> params, boolean expected) {
        try {
            ServiceFactory.getRPMService().getErrorService().reportError(message, filterErrorAtts(params, customAttributeSender), expected);

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

    private boolean isExpectedErrorConfigured(Throwable throwable) {
        if (throwable != null) {
            String expectedConfigName = throwable.getClass().getName();
            String message = throwable.getMessage();
            String app_name = NewRelic.getAgent().getConfig().getValue("app_name");
            Set<ExpectedErrorConfig> expectedErrors = ServiceFactory.getConfigService().getErrorCollectorConfig(app_name).getExpectedErrors();
            for (ExpectedErrorConfig errorConfig : expectedErrors) {
                String errorClass = errorConfig.getErrorClass();
                String errorMessage = errorConfig.getErrorMessage();
                if ((errorClass.equals(expectedConfigName) && errorMessage == null) ||
                        (errorMessage != null) &&
                                (errorClass.equals(expectedConfigName) && errorMessage.equals(message))) {
                    return true;
                }
            }
        }
        return false;
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
     * @param value
     */
    @Override
    public void addCustomParameter(String key, String value) {
        customAttributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key
     * @param value
     */
    @Override
    public void addCustomParameter(String key, Number value) {
        customAttributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add a key/value pair to the current transaction. These are reported in errors and transaction traces.
     *
     * @param key
     * @param value
     */
    @Override
    public void addCustomParameter(String key, boolean value) {
        customAttributeSender.addAttribute(key, value, "addCustomParameter");
    }

    /**
     * Add key/value pairs to the current transaction. These are reported in errors and transaction traces.
     *
     * @param params
     */
    @Override
    public void addCustomParameters(Map<String, Object> params) {
        customAttributeSender.addAttributes(params, "addCustomParameters");
    }

    /**
     * Sets the user ID for the current transaction by adding the "enduser.id" agent attribute. It is reported in errors and transaction traces.
     * When high security mode is enabled, this method call will do nothing.
     *
     * @param userId The user ID to report. If it is a null or blank String, the "enduser.id" agent attribute will not be included in the current transaction and any associated errors.
     */
    @Override
    public void setUserId(String userId) {
        MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_USER_ID);
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
            return;
        }
        final String attributeKey = "enduser.id";
        final String methodCalled = "setUserId";
        // Ignore null and empty strings
        if (userId == null || userId.trim().isEmpty()) {
            Agent.LOG.log(Level.FINER, "Will not include the {0} attribute because {1} was invoked with a null or blank value", attributeKey, methodCalled);
            agentAttributeSender.removeAttribute(attributeKey);
            return;
        }
        agentAttributeSender.addAttribute(attributeKey, userId, methodCalled);
    }

    /**
     * Set the name of the current transaction.
     *
     * @param category
     * @param name     The name of the transaction in URI format. example: /store/order
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
     * <p>
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
        return getBrowserTimingHeader(null);
    }

    @Override
    public String getBrowserTimingHeader(String nonce) {
        Transaction tx = Transaction.getTransaction(false);
        try {
            if (tx == null) {
                Agent.LOG.finer("Unable to get browser timing header in NewRelic API: not running in a transaction");
                return "";
            }
            String header = null;
            synchronized (tx) {
                if (nonce == null) {
                    header = tx.getBrowserTransactionState().getBrowserTimingHeader();
                } else {
                    header = tx.getBrowserTransactionState().getBrowserTimingHeader(nonce);
                }
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
     * Set the user name for the current web transaction.
     * If high security mode is enabled, this method call does nothing.
     */
    @Override
    public void setUserName(String name) {
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
            return;
        }
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
        customAttributeSender.addAttribute("user", name, "setUserName");
    }

    /**
     * Set the account name for the current web transaction.
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
            customAttributeSender.addAttribute("account", name, "setAccountName");
            MetricNames.recordApiSupportabilityMetric(MetricNames.SUPPORTABILITY_API_SET_ACCOUNT_NAME);
        }
    }

    /**
     * Set the product name for the current web transaction.
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
            customAttributeSender.addAttribute("product", name, "setProductName");
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

    @Override
    public void setErrorGroupCallback(ErrorGroupCallback errorGroupCallback) {
        ErrorGroupCallbackHolder.setErrorGroupCallback(errorGroupCallback);
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
