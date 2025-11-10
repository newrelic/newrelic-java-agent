/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.dispatchers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.attributes.AttributeNames;
import com.newrelic.agent.bridge.StatusCodePolicy;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.bridge.WebResponse;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigImpl;
import com.newrelic.agent.config.AttributesConfig;
import com.newrelic.agent.config.CustomRequestHeaderConfig;
import com.newrelic.agent.config.HiddenProperties;
import com.newrelic.agent.config.TransactionEventsConfig;
import com.newrelic.agent.config.TransactionTracerConfig;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.servlet.ServletUtils;
import com.newrelic.agent.stats.ApdexStats;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.agent.tracers.Tracer;
import com.newrelic.agent.tracers.servlet.ExternalTimeTracker;
import com.newrelic.agent.transaction.TransactionNamer;
import com.newrelic.agent.transaction.WebTransactionNamer;
import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

import java.net.HttpURLConnection;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class WebRequestDispatcher extends DefaultDispatcher implements WebResponse {

    private static final String UNKNOWN_URI = "/Unknown";

    private static final StatusCodePolicy LAST_STATUS_CODE_POLICY = (currentStatus, lastStatus) -> lastStatus;
    private static final StatusCodePolicy ERROR_STATUS_CODE_POLICY = (currentStatus, lastStatus) -> currentStatus < HttpURLConnection.HTTP_BAD_REQUEST ? lastStatus : currentStatus;
    private static final StatusCodePolicy FREEZE_STATUS_CODE_POLICY = (currentStatus, lastStatus) -> currentStatus;
    private final AtomicBoolean responseRecorded = new AtomicBoolean(false);
    private volatile Request request;
    private volatile Response response;
    private volatile String requestURI;
    private volatile ExternalTimeTracker externalTimeTracker;
    private volatile int statusCode;
    private volatile String statusMessage;
    private volatile StatusCodePolicy statusCodePolicy;

    public WebRequestDispatcher(Request request, final Response response, Transaction transaction) {
        super(transaction);

        boolean isLastStatusCodePolicy = transaction.getAgentConfig().getValue(HiddenProperties.LAST_STATUS_CODE_POLICY, Boolean.TRUE);
        statusCodePolicy = isLastStatusCodePolicy ? LAST_STATUS_CODE_POLICY : ERROR_STATUS_CODE_POLICY;
        this.request = request;
        this.response = response;
        externalTimeTracker = ExternalTimeTracker.create(request, transaction.getWallClockStartTimeMs());
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public void setRequest(Request request) {
        externalTimeTracker = ExternalTimeTracker.create(request, getTransaction().getWallClockStartTimeMs());
        this.request = request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public void transactionActivityWithResponseFinished() {
        if (request != null && responseRecorded.compareAndSet(false, true)) {
            try {
                getUri();
                setStatus();
                freezeStatus();
                setStatusMessage();
                recordParameters();
                storeReferrer();
                recordCustomHeaders();

                storeHeader("Accept", AttributeNames.REQUEST_ACCEPT_PARAMETER_NAME);
                storeHeader("Host", AttributeNames.REQUEST_HOST_PARAMETER_NAME);
                storeHeader("User-Agent", AttributeNames.REQUEST_USER_AGENT_PARAMETER_NAME);
                storeHeader("Content-Length", AttributeNames.REQUEST_CONTENT_LENGTH_PARAMETER_NAME);
                storeMethod();
                storeResponseContentType();

                AttributesConfig attributesConfig = ServiceFactory.getConfigService().getDefaultAgentConfig().getAttributesConfig();
                if (getStatus() > 0) {
                    if (attributesConfig.isLegacyHttpAttr()) {
                        // http status is now being recorded as a string
                        getTransaction().getAgentAttributes().put(AttributeNames.HTTP_STATUS, String.valueOf(getStatus()));
                    }
                    if (attributesConfig.isStandardHttpAttr()) {
                        // http.statusCode is supposed to be an int
                        getTransaction().getAgentAttributes().put(AttributeNames.HTTP_STATUS_CODE, getStatus());
                    }
                }

                if (getStatusMessage() != null) {
                    if (attributesConfig.isLegacyHttpAttr()) {
                        getTransaction().getAgentAttributes().put(AttributeNames.HTTP_STATUS_MESSAGE, getStatusMessage());
                    }
                    if (attributesConfig.isStandardHttpAttr()) {
                        getTransaction().getAgentAttributes().put(AttributeNames.HTTP_STATUS_TEXT, getStatusMessage());
                    }
                }

                // adding request.uri here includes it in the Transaction event, which also propagates to any Transaction error events
                String filteredRequestURI = filterRequestURI();
                if (filteredRequestURI != null && !filteredRequestURI.isEmpty()) {
                    getTransaction().getAgentAttributes().put(AttributeNames.REQUEST_URI, filteredRequestURI);
                }
            } catch (Throwable e) {
                Agent.LOG.log(Level.FINER, e, "Exception when reporting request/response information.");
            } finally {
                request = null;
                response = null;
            }
        }
    }

    @Override
    public void transactionFinished(String transactionName, TransactionStats stats) {
        // This acts as a fallback in case we don't have web dispatcher instrumentation for a specific framework but we
        // still have a request/response available at this point to capture parameters from. If this method was already
        // called for this request/response then this method call will not add any more state to the transaction.
        transactionActivityWithResponseFinished();

        doRecordMetrics(transactionName, stats);
    }

    private void recordParameters() {
        try {
            ServletUtils.recordParameters(getTransaction(), request);
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINEST, e, "Error recording parameters for transaction");
        }
    }

    @Override
    public final String getUri() {
        if (requestURI == null) {
            requestURI = initializeRequestURI();
        }
        return requestURI;
    }

    @Override
    public void setTransactionName() {
        if (Transaction.isDummyRequest(request)) {
            Tracer rootTracer = getTransaction().getRootTracer();
            if (rootTracer != null) {
                // use a low priority
                rootTracer.nameTransaction(TransactionNamePriority.REQUEST_URI);
            }
        }

        TransactionNamer tn = WebTransactionNamer.create(getTransaction(), filterRequestURI());
        tn.setTransactionName();
    }

    private String filterRequestURI() {
        return ServiceFactory.getAttributesService().filterRequestUri(getTransaction().getApplicationName(), AgentConfigImpl.ATTRIBUTES, getUri());
    }

    /**
     * Don't call this in the class constructor because it has the side-effect of breaking Tomcat's URI parsing: the
     * JSESSIONID is not removed. For example, HttpServletRequest.getRequestURI returns
     * /ibe/HOTEL_CALIFORNIA/rooms;jsessionid=78088C419865AAEF0181396273A47F1C instead of /ibe/HOTEL_CALIFORNIA/rooms.
     * This breaks customer's URL rewrite rules.
     *
     * See https://www.pivotaltracker.com/story/show/14614603
     *
     * @return the request URI excluding parameters
     */
    private String initializeRequestURI() {
        String result = UNKNOWN_URI;
        if (request == null) {
            return result;
        }
        try {
            String uri = request.getRequestURI();
            if (uri == null || uri.length() == 0) {
                Agent.LOG.log(Level.FINER, "requestURI is null: setting requestURI to {0}", result);
            } else {
                result = ServiceFactory.getNormalizationService().getUrlBeforeParameters(uri);
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINER, "Error calling requestURI: " + e.toString());
            Agent.LOG.log(Level.FINEST, e.toString(), e);
            result = UNKNOWN_URI;
        }
        return result;
    }

    /**
     * Store the referer header.
     */
    private void storeReferrer() {
        try {
            String referer = request.getHeader("Referer");
            if (referer != null) {
                // Don't include referrer'request parameters (they might have sensitive data)
                referer = referer.split("\\;")[0];
                // Don't include referrer's url query parameters (they might have sensitive data)
                referer = referer.split("\\?")[0];
                getTransaction().getAgentAttributes().put(AttributeNames.REQUEST_REFERER_PARAMETER_NAME, referer);
            }
        } catch (Throwable e) {
            Agent.LOG.finer("Error getting referer: " + e.toString());
            Agent.LOG.log(Level.FINEST, e.toString(), e);
        }
    }

    /**
     * Store an HTTP header, logging errors and continuing if necessary.
     */
    private void storeHeader(String headerName, String attributeName) {
        try {
            String headerVal = request.getHeader(headerName);
            if (headerVal != null) {
                getTransaction().getAgentAttributes().put(attributeName, headerVal);
            }
        } catch (Throwable e) {
            Agent.LOG.finer("Error getting HTTP " + headerName + " header: " + e.toString());
            Agent.LOG.log(Level.FINEST, e.toString(), e);
        }
    }

    /**
     * Store a custom HTTP header and header alias (if present), logging errors and continuing if necessary.
     */
    private void recordCustomHeaders() {
        AgentConfig agentConfig = ServiceFactory.getConfigService().getDefaultAgentConfig();

        if (agentConfig.isHighSecurity()) {
            return;
        }

        Set<CustomRequestHeaderConfig> headerConfigs = agentConfig.getTransactionEventsConfig().getRequestHeaderConfigs();

        try {
            for (CustomRequestHeaderConfig config : headerConfigs) {
                String headerName = config.getHeaderName();
                String headerVal = request.getHeader(headerName);
                String headerAlias = config.getHeaderAlias();

                if (headerVal != null) {
                    if (headerAlias != null) {
                        getTransaction().getUserAttributes().put(headerAlias, headerVal);
                    } else {
                        getTransaction().getUserAttributes().put(headerName, headerVal);
                    }
                } else {
                    Agent.LOG.log(Level.FINE,
                            MessageFormat.format("{0} header value was null, so it will not be included", headerName));
                }
            }
        } catch (Throwable e) {
            Agent.LOG.log(Level.FINE, "Error recording one of header or alias from custom request headers.");
        }
    }

    /**
     * Store the HTTP request method.
     */
    private void storeMethod() {
        if (request instanceof ExtendedRequest) {
            try {
                String method = ((ExtendedRequest) request).getMethod();
                if (method != null) {
                    getTransaction().getAgentAttributes().put(AttributeNames.REQUEST_METHOD_PARAMETER_NAME, method);
                }
            } catch (Throwable e) {
                Agent.LOG.finer("Error getting HTTP method: " + e.toString());
                Agent.LOG.log(Level.FINEST, e.toString(), e);
            }
        }
    }

    /**
     * Store the HTTP response Content-Type
     */
    private void storeResponseContentType() {
        if (response != null) {
            try {
                String contentType = response.getContentType();
                if (contentType != null) {
                    getTransaction().getAgentAttributes().put(AttributeNames.RESPONSE_CONTENT_TYPE_PARAMETER_NAME, contentType);
                }
            } catch (Throwable e) {
                Agent.LOG.finer("Error getting HTTP response ContentType: " + e.toString());
                Agent.LOG.log(Level.FINEST, e.toString(), e);
            }
        }
    }

    @Override
    public void freezeStatus() {
        statusCodePolicy = FREEZE_STATUS_CODE_POLICY;
        Agent.LOG.log(Level.FINER, "Freezing status code to {0}", getStatus());
    }

    private void setStatus() {
        if (response != null) {
            try {
                setStatus(response.getStatus());
            } catch (Throwable e) {
                Agent.LOG.log(Level.FINER, "Failed to get response status code {0}", e.toString());
            }
        }
    }

    private void setStatusMessage() {
        if (response != null && getStatusMessage() == null && getStatus() >= HttpURLConnection.HTTP_BAD_REQUEST) {
            try {
                setStatusMessage(response.getStatusMessage());
            } catch (Throwable e) {
                Agent.LOG.log(Level.FINER, "Failed to get response status message {0}", e.toString());
            }
        }
    }

    private void doRecordMetrics(String transactionName, TransactionStats stats) {
        recordHeaderMetrics(stats);
        recordApdexMetrics(transactionName, stats);
        recordDispatcherMetrics(transactionName, stats);
    }

    // public for unit tests
    public void recordHeaderMetrics(TransactionStats statsEngine) {
        externalTimeTracker.recordMetrics(statsEngine);
    }

    /**
     * @return the external time in milliseconds
     */
    public long getQueueTime() {
        return externalTimeTracker.getExternalTime();
    }

    private void recordDispatcherMetrics(String frontendMetricName, TransactionStats stats) {
        if (frontendMetricName == null || frontendMetricName.length() == 0) {
            return;
        }
        long frontendTimeInNanos = getTransaction().getTransactionTimer().getResponseTimeInNanos();
        // frontend represents the logical front of the request, but it is not an actual component, so it has no
        // exclusive time
        stats.getUnscopedStats().getOrCreateResponseTimeStats(frontendMetricName).recordResponseTimeInNanos(frontendTimeInNanos);
        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION).recordResponseTimeInNanos(
                frontendTimeInNanos);
        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.DISPATCHER).recordResponseTimeInNanos(
                frontendTimeInNanos);
        if (getStatus() > 0) {
            String metricName = MetricNames.NETWORK_INBOUND_STATUS_CODE + getStatus();
            stats.getUnscopedStats().getOrCreateResponseTimeStats(metricName).recordResponseTimeInNanos(frontendTimeInNanos);
        }

        if (hasTransactionName(frontendMetricName, MetricNames.WEB_TRANSACTION)) {
            // total time for the transaction
            String totalTimeMetric = getTransName(frontendMetricName, MetricNames.WEB_TRANSACTION,
                    MetricNames.TOTAL_TIME);
            stats.getUnscopedStats().getOrCreateResponseTimeStats(totalTimeMetric).recordResponseTimeInNanos(
                    getTransaction().getTransactionTimer().getTotalSumTimeInNanos());

            // record first byte
            long firstByteDurNs = getTransaction().getTransactionTimer().getTimeToFirstByteInNanos();
            if (firstByteDurNs > 0) {
                String firstByteMetricName = getTransName(frontendMetricName, MetricNames.WEB_TRANSACTION,
                        MetricNames.FIRST_BYTE);
                stats.getUnscopedStats().getOrCreateResponseTimeStats(firstByteMetricName).recordResponseTimeInNanos(
                        firstByteDurNs);
                stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION_FIRST_BYTE).recordResponseTimeInNanos(
                        firstByteDurNs);
            }

            // record last byte
            long lastByteDurNs = getTransaction().getTransactionTimer().getTimetoLastByteInNanos();
            if (lastByteDurNs > 0) {
                String lastByteMetricName = getTransName(frontendMetricName, MetricNames.WEB_TRANSACTION,
                        MetricNames.LAST_BYTE);
                stats.getUnscopedStats().getOrCreateResponseTimeStats(lastByteMetricName).recordResponseTimeInNanos(
                        lastByteDurNs);
                stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION_LAST_BYTE).recordResponseTimeInNanos(
                        lastByteDurNs);
            }

            Object cpuTime = getTransaction().getIntrinsicAttributes().get(AttributeNames.CPU_TIME_PARAMETER_NAME);
            if (cpuTime != null && cpuTime instanceof Long) {
                long val = (Long) cpuTime;
                String cpuMetricName = MetricNames.CPU_PREFIX + frontendMetricName;
                stats.getUnscopedStats().getOrCreateResponseTimeStats(cpuMetricName).recordResponseTimeInNanos(val);
                stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.CPU_WEB).recordResponseTimeInNanos(val);
            }
        }

        stats.getUnscopedStats().getOrCreateResponseTimeStats(MetricNames.WEB_TRANSACTION_TOTAL_TIME).recordResponseTimeInNanos(
                getTransaction().getTransactionTimer().getTotalSumTimeInNanos());
    }

    private void recordApdexMetrics(String frontendMetricName, TransactionStats stats) {
        if (frontendMetricName == null || frontendMetricName.length() == 0) {
            return;
        }
        if (!getTransaction().getAgentConfig().isApdexTSet()) {
            return;
        }
        if (isIgnoreApdex()) {
            Agent.LOG.log(Level.FINE, "Ignoring transaction for Apdex {0}", frontendMetricName);
            return;
        }
        String frontendApdexMetricName = getApdexMetricName(frontendMetricName, MetricNames.WEB_TRANSACTION,
                MetricNames.APDEX);
        if (frontendApdexMetricName == null || frontendApdexMetricName.length() == 0) {
            return;
        }
        long apdexT = getTransaction().getAgentConfig().getApdexTInMillis(frontendMetricName);

        ApdexStats apdexStats = stats.getUnscopedStats().getApdexStats(frontendApdexMetricName);
        ApdexStats overallApdexStats = stats.getUnscopedStats().getApdexStats(MetricNames.APDEX);
        if (isApdexFrustrating()) {
            apdexStats.recordApdexFrustrated();
            overallApdexStats.recordApdexFrustrated();
        } else {
            long responseTimeInMillis = getTransaction().getTransactionTimer().getResponseTimeInMilliseconds()
                    + externalTimeTracker.getExternalTime();
            apdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
            overallApdexStats.recordApdexResponseTime(responseTimeInMillis, apdexT);
        }
    }

    public boolean isApdexFrustrating() {
        return getTransaction().isErrorReportableAndNotIgnored() && getTransaction().isErrorNotExpected();
    }

    @Override
    public TransactionTracerConfig getTransactionTracerConfig() {
        return getTransaction().getAgentConfig().getRequestTransactionTracerConfig();
    }

    @Override
    public boolean isWebTransaction() {
        return true;
    }

    @Override
    public String getCookieValue(String name) {
        if (request == null) {
            return null;
        }
        return request.getCookieValue(name);
    }

    @Override
    public String getHeader(String name) {
        if (request == null) {
            return null;
        }
        return request.getHeader(name);
    }

    @Override
    public void setStatus(int statusCode) {
        Agent.LOG.log(Level.FINEST, "Called setStatus: {0}", statusCode);
        if (statusCode <= 0 || statusCode == this.statusCode) {
            return;
        }
        int nextStatusCode = statusCodePolicy.nextStatus(this.statusCode, statusCode);
        if (nextStatusCode != this.statusCode) {
            Agent.LOG.log(Level.FINER, "Setting status to {0}", nextStatusCode);
        }
        this.statusCode = nextStatusCode;
    }

    @Override
    public int getStatus() {
        return statusCode;
    }

    @Override
    public void setStatusMessage(String message) {
        this.statusMessage = message;
    }

    @Override
    public String getStatusMessage() {
        return statusMessage;
    }

}
