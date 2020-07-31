/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.mule3;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.HttpParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import org.apache.commons.httpclient.Header;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.PipelineMessageNotificationListener;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.context.notification.ListenerSubscriptionPair;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.transport.http.HttpResponse;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Static utility methods used by Mule 3.x instrumentation.
 */
public class MuleUtils {

    private static final URI UNKNOWN_HOST_URI = URI.create("http://UnknownHost/");

    /**
     * Because one-way flows don't return a response to the caller, there doesn't appear to be a convenient code path
     * that is also shared with request-response flows, so this global map is necessary to store a reference to each
     * token so when our custom listener receives notification of processing complete and the flow ends, the token can
     * be expired.
     * <p>
     * Also, async Tokens aren't serializable so they shouldn't be added directly to a mule message, instead store the
     * token globally and put the key to look it up into the mule message.
     */
    private static final Map<FlowKey, Token> TOKEN_MAP = new ConcurrentHashMap<>(64, 0.8f, 8);

    public static final String MULE_EVENT_TOKEN_KEY = "_ASYNC_TOKEN_";

    /**
     * Almost all work is done in runnables, including processes that live for the life of the application that we don't
     * want to see, for example when hot-loading a mule app, which looks like: org.mule.work.WorkerContext/workAccepted
     * in the RPM Transactions UI.
     */
    private static final Set<String> IGNORE_CLASSES = new HashSet<>(3);

    static {
        IGNORE_CLASSES.add("org.mule.context.notification.ServerNotificationManager");
        IGNORE_CLASSES.add("org.mule.processor.LaxSedaStageInterceptingMessageProcessor");
    }

    public static boolean ignoreClass(String className) {
        return IGNORE_CLASSES.contains(className);
    }

    /**
     * Mule event ids can span multiple flows if one initiates another, so the flow name is also necessary to uniquely
     * identify the token's owner to expire it correctly.
     */
    public static class FlowKey implements Serializable {
        private final String flowName;
        private final String eventId;

        public FlowKey(String flowName, String eventId) {
            this.flowName = flowName == null ? "" : flowName;
            this.eventId = eventId == null ? "" : eventId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FlowKey flowKey = (FlowKey) o;
            if (!flowName.equals(flowKey.flowName)) return false;
            return eventId.equals(flowKey.eventId);
        }

        @Override
        public int hashCode() {
            int result = flowName.hashCode();
            result = 31 * result + eventId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return new StringBuilder(flowName).append(":").append(eventId).toString();
        }
    }

    public static void putToken(FlowKey key, Token token) {
        TOKEN_MAP.put(key, token);
    }

    public static Token getToken(FlowKey key) {
        return TOKEN_MAP.get(key);
    }

    public static Token removeToken(FlowKey key) {
        return TOKEN_MAP.remove(key);
    }

    /**
     * Stores an async token in the global map, and sets the key as a flow variable so each processing phase can link
     * its work. Also registers an event listener so when the flow ends we can expire the token.
     */
    public static void registerToken(MuleEvent muleEvent, Token token) {
        FlowKey key = new FlowKey(muleEvent.getFlowConstruct().getName(), muleEvent.getId());
        MuleUtils.putToken(key, token);
        muleEvent.setFlowVariable(MuleUtils.MULE_EVENT_TOKEN_KEY, key);
        MuleUtils.registerFlowEndListener(muleEvent);
    }

    /**
     * The mule event that is used to create the async token and register the listener needs to be saved in the case
     * where the flow terminates abnormally and the process complete / end events aren't sent. In the handleException
     * methods that deal with exceptions we can compare the mule event that method receives to the mule event stored
     * here.
     */
    private static class NRPipelineMessageNotificationListener implements PipelineMessageNotificationListener<PipelineMessageNotification> {
        private final MuleEvent muleEvent;

        public NRPipelineMessageNotificationListener(MuleEvent muleEvent) {
            this.muleEvent = muleEvent;
        }

        public MuleEvent getMuleEvent() {
            return muleEvent;
        }

        /**
         * For whatever reason, mule has the following behavior:
         * 1. If a sub-flow throws an exception, PROCESS_COMPLETE events can still be sent but not PROCESS_END
         * 2. One-way flows send PROCESS_COMPLETE but not necessarily PROCESS_END
         * So it's safer to check for the former in both cases even though in normal circumstances there should be an
         * equal amount of both types of events.
         * <p>
         * We also weave the handleException method to further attempt any clean up if neither event is sent.
         */
        @Override
        public void onNotification(PipelineMessageNotification notification) {
            if (notification.getAction() == PipelineMessageNotification.PROCESS_COMPLETE) {
                Object source = notification.getSource();
                if (source instanceof MuleEvent) {
                    FlowKey key = new FlowKey(muleEvent.getFlowConstruct().getName(), muleEvent.getId());
                    Token token = MuleUtils.removeToken(key);
                    if (token != null) {
                        token.expire();
                        NewRelic.getAgent().getLogger().log(Level.FINEST, "MuleUtils#onNotification unregistering listener = {0} on muleEvent = {1}", this, muleEvent);
                        muleEvent.getMuleContext().unregisterListener(this);
                    }
                }
            }
        }
    }

    /**
     * There is one mule context per mule application, which we use to register a listener which will send us
     * notifications to track when a flow is done processing.
     */
    private static void registerFlowEndListener(MuleEvent muleEvent) {
        try {
            PipelineMessageNotificationListener listener = new NRPipelineMessageNotificationListener(muleEvent);
            muleEvent.getMuleContext().registerListener(listener);
            NewRelic.getAgent().getLogger().log(Level.FINEST, "MuleUtils#registerFlowEndListener registering listener = {0} on muleEvent = {1}", listener, muleEvent);
        } catch (Exception e) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "MuleUtils#registerFlowEndListener unable to register");
        }
    }

    /**
     * If a flow terminates due to an exception, check if there was a listener or an async token associated with it and
     * clean them up. If a listener was registered, it might not receive the processing complete notification, so we
     * have to loop through every one and check if its one of our custom listeners, pull out its mule event, and check
     * its event id to see if it matches the event id of the event that caused the exception.
     */
    public static void handleException(MuleEvent muleEvent) {
        if (muleEvent != null) {
            FlowKey key = new FlowKey(muleEvent.getFlowConstruct().getName(), muleEvent.getId());
            Token token = MuleUtils.removeToken(key);
            if (token != null) {
                token.expire();
                Set<ListenerSubscriptionPair> listeners = muleEvent.getMuleContext().getNotificationManager().getListeners();
                Set<ServerNotificationListener> removals = new HashSet<>();
                for (ListenerSubscriptionPair lsp : listeners) {
                    if (lsp.getListener() instanceof NRPipelineMessageNotificationListener) {
                        NRPipelineMessageNotificationListener nrlistener = (NRPipelineMessageNotificationListener) lsp.getListener();
                        if (nrlistener.getMuleEvent().getId() == muleEvent.getId()) {
                            NewRelic.getAgent().getLogger().log(Level.FINEST, "MuleUtils#handleException unregistering listener = {0} due to exception", lsp);
                            removals.add(lsp.getListener());
                        }
                    }
                }

                muleEvent.getMuleContext().getNotificationManager().removeAllListeners(removals);
            }
        }
    }

    /**
     * Called outbound when using Http Transport. Verify this is consistent with usages of MuleHttpConnectorResponse in
     * the mule-3.6 and mule-3.7 modules.
     */
    public static void reportToAgent(final HttpResponse response) {
        if (response == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "MuleUtils#reportToAgent httpResponse is null");
            return;
        }

        final MuleHttpTransportResponse muleResponse = new MuleHttpTransportResponse(response);
        NewRelic.getAgent().getTracedMethod().addOutboundRequestHeaders(muleResponse);

        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
        if (txn != null) {
            txn.setWebResponse(muleResponse);
            txn.getCrossProcessState().processOutboundResponseHeaders(muleResponse, getContentLength(muleResponse));
        }
    }

    /**
     * Called inbound when using Http Transport. Verify this is consistent with usages of MuleHttpConnectorRequest in
     * the mule-3.6 and mule-3.7 modules.
     */
    public static void reportToAgent(final MuleEvent muleEvent) {
        if (muleEvent == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "MuleUtils#reportToAgent muleEvent is null");
            return;
        }

        MuleMessage message = muleEvent.getMessage();
        if (message == null) {
            NewRelic.getAgent().getLogger().log(Level.FINE, "MuleUtils#reportToAgent muleEvent.message is null");
            return;
        }

        final MuleHttpTransportRequest muleRequest = new MuleHttpTransportRequest(message);

        ExternalParameters params;
        try {
            URI uri = new URI(muleRequest.getRequestURI());
            params = HttpParameters
                    .library("MuleHTTP")
                    .uri(uri)
                    .procedure("writeResponse")
                    .inboundHeaders(muleRequest)
                    .build();
        } catch (URISyntaxException uriSyntaxException) {
            params = HttpParameters
                    .library("MuleHTTP")
                    .uri(UNKNOWN_HOST_URI)
                    .procedure("writeResponse")
                    .inboundHeaders(muleRequest)
                    .build();
        }

        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

        final Transaction txn = AgentBridge.getAgent().getTransaction(false);
        txn.setWebRequest(muleRequest);

        final String txnName = message.getInboundProperty("http.context.path") + " (" + muleRequest.getMethod() + ")";
        txn.setTransactionName(TransactionNamePriority.FRAMEWORK_HIGH, false, "Mule/Transport", txnName);
    }

    private static long getContentLength(MuleHttpTransportResponse muleResponse) {
        Header contentLength = muleResponse.getHeader("Content-Length");
        if (contentLength == null) {
            return -1L;
        } else {
            return Long.parseLong(contentLength.getValue());
        }
    }

}
