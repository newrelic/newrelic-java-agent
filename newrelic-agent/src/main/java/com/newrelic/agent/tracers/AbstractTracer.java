/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.TransactionActivity;
import com.newrelic.agent.TransactionErrorPriority;
import com.newrelic.agent.attributes.AttributeValidator;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.AgentWrapper;
import com.newrelic.agent.util.Strings;
import com.newrelic.api.agent.AttributeHolder;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.api.agent.Token;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Base class for all tracers. This implements {@link InvocationHandler#invoke(Object, Method, Object[])}
 */
public abstract class AbstractTracer implements Tracer, AttributeHolder {

    static final int INITIAL_PARAMETER_MAP_SIZE = 5;
    static final int INITIAL_PARAMETER_SET_SIZE = 5;
    protected static final String ATTRIBUTE_TYPE = "custom";

    private final TransactionActivity transactionActivity;
    private AttributeValidator attributeValidator;
    private Set<String> rollupMetricNames;
    private Set<String> exclusiveRollupMetricNames;
    Map<String, Object> customAttributes;
    Map<String, Object> agentAttributes;
    private Set<String> agentAttributeNamesForSpans;
    private String customPrefix = "Custom";
    // doesn't need to be thread safe since this flag affects the decision to registerAsync
    private Boolean trackChildThreads = null;
    private Boolean trackCallBackRunnable = false;
    private AtomicReference<TracedException> tracerError = new AtomicReference<>(TracedException.NO_EXCEPTION);

    private final long startTimeInMillis;
    AtomicReference<Long> finishTime = new AtomicReference<>(null);
    private final String ATTRIBUTE_API_METHOD_NAME = "TracedMethod.addCustomAttributes";

    // Tracers MUST NOT store references to the Transaction. Why: tracers are stored in the TransactionActivity,
    // and Activities can be reparented from one Transaction to another by the public APIs that support async.

    /**
     * Create a tracer on the current thread.
     *
     * @param transaction the transaction that owns the activity on the current thread. Must not be null.
     */
    public AbstractTracer(Transaction transaction) {
        this(transaction.getTransactionActivity(), new AttributeValidator(ATTRIBUTE_TYPE));
    }

    /**
     * Create a tracer on the current thread.
     *
     * @param txa the activity for the current thread. The value is allowed to be null.
     */
    public AbstractTracer(TransactionActivity txa, AttributeValidator attributeValidator) {
        this.transactionActivity = txa;
        this.startTimeInMillis = System.currentTimeMillis();
        this.attributeValidator = attributeValidator;
    }

    /**
     * Get the transaction that currently owns the activity that owns this tracer.
     *
     * @return the transaction that currently owns the activity that owns this tracer.
     */
    public final Transaction getTransaction() {
        return transactionActivity.getTransaction();
    }

    /**
     * Get the transaction activity that owns this tracer.
     *
     * @return the transaction activity that owns this tracer. This value does not change during the life of the tracer.
     */
    @Override
    public final TransactionActivity getTransactionActivity() {
        return transactionActivity;
    }

    protected Object getInvocationTarget() {
        return null;
    }

    @Override
    public final Object invoke(Object methodName, Method method, Object[] args) {
        try {
            if (args == null) {
                Agent.LOG.severe("Tracer.finish() was invoked with no arguments");
            } else if (AgentWrapper.SUCCESSFUL_METHOD_INVOCATION == methodName) {
                if (args.length == 2) {
                    finish((Integer) args[0], args[1]);
                } else {
                    Agent.LOG.severe(MessageFormat.format("Tracer.finish(int, Object) was invoked with {0} arguments(s)", args.length));
                }
            } else if (AgentWrapper.UNSUCCESSFUL_METHOD_INVOCATION == methodName) {
                if (args.length == 1) {
                    finish((Throwable) args[0]);
                } else {
                    Agent.LOG.severe(MessageFormat.format("Tracer.finish(Throwable) was invoked with {0} arguments(s)", args.length));
                }
            } else {
                Agent.LOG.severe(MessageFormat.format("Tracer.finish was invoked with an unknown method: {0}", methodName));
            }
        } catch (RetryException e) {
            return invoke(methodName, method, args);
        } catch (Throwable t) {
            if (Agent.LOG.isLoggable(Level.FINE)) {
                String msg = MessageFormat.format(
                        "An error occurred finishing method tracer {0} for signature {1} : {2}", getClass().getName(),
                        getClassMethodSignature(), t.toString());
                if (Agent.LOG.isLoggable(Level.FINEST)) {
                    Agent.LOG.log(Level.FINEST, msg, t);
                } else {
                    Agent.LOG.fine(msg);
                }
            }
        }
        return null;
    }

    @Override
    public abstract ClassMethodSignature getClassMethodSignature();

    @Override
    public boolean isChildHasStackTrace() {
        return false;
    }

    @Override
    public void nameTransaction(TransactionNamePriority priority) {
        try {
            ClassMethodSignature classMethodSignature = getClassMethodSignature();
            Object invocationTarget = getInvocationTarget();
            String className = invocationTarget == null ? classMethodSignature.getClassName()
                    : invocationTarget.getClass().getName();
            String txName = className + "/" + classMethodSignature.getMethodName();
            Agent.LOG.log(Level.FINER, "Setting transaction name using instrumented class and method: {0}", txName);
            Transaction tx = transactionActivity.getTransaction();
            tx.setTransactionName(priority, false, customPrefix, txName);
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "nameTransaction", t);
        }
    }

    @Override
    public TracedMethod getParentTracedMethod() {
        return getParentTracer();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    protected Set<String> getRollupMetricNames() {
        return rollupMetricNames;
    }

    protected Set<String> getExclusiveRollupMetricNames() {
        return exclusiveRollupMetricNames;
    }

    @Override
    public void addRollupMetricName(String... metricNameParts) {
        if (rollupMetricNames == null) {
            rollupMetricNames = new HashSet<>();
        }
        rollupMetricNames.add(Strings.join(MetricNames.SEGMENT_DELIMITER, metricNameParts));
    }

    @Override
    public void setRollupMetricNames(String... metricNames) {
        rollupMetricNames = new HashSet<>(metricNames.length);
        rollupMetricNames.addAll(Arrays.asList(metricNames));
    }

    @Override
    public void addExclusiveRollupMetricName(String... metricNameParts) {
        if (exclusiveRollupMetricNames == null) {
            exclusiveRollupMetricNames = new HashSet<>();
        }
        exclusiveRollupMetricNames.add(Strings.join(MetricNames.SEGMENT_DELIMITER, metricNameParts));
    }

    @Override
    public void setCustomMetricPrefix(String prefix) {
        this.customPrefix = prefix;
    }

    @Override
    public void setTrackChildThreads(boolean shouldTrack) {
        this.trackChildThreads = shouldTrack;
    }

    @Override
    public boolean trackChildThreads() {
        if (null == this.trackChildThreads) {
            TracedMethod parent = this.getParentTracedMethod();
            if (null == parent) {
                return true;
            } else {
                return parent.trackChildThreads();
            }
        }
        return this.trackChildThreads;
    }

    @Override
    public void setTrackCallbackRunnable(boolean shouldTrack) {
        this.trackCallBackRunnable = shouldTrack;
    }

    @Override
    public boolean isTrackCallbackRunnable() {
        return this.trackCallBackRunnable || this.isParentTrackCallbackRunnable();
    }

    private boolean isParentTrackCallbackRunnable() {
        TracedMethod parent = this.getParentTracedMethod();
        if (null == parent) {
            return false;
        }
        return parent.isTrackCallbackRunnable();
    }

    @Override
    public void addOutboundRequestHeaders(OutboundHeaders outboundHeaders) {
        Agent.LOG.severe("addOutboundRequestHeaders is only supported on subclasses of DefaultTracer: {0}");
    }

    @Override
    public void readInboundResponseHeaders(InboundHeaders inboundResponseHeaders) {
        Agent.LOG.severe("readInboundResponseHeaders is only supported on subclasses of DefaultTracer: {0}");
    }

    @Override
    public void reportAsExternal(ExternalParameters externalParameters) {
        Agent.LOG.severe("reportAsExternal is only supported on subclasses of DefaultTracer: {0}");
    }

    @Override
    public void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters) {
        Agent.LOG.severe("reportAsExternal is only supported on subclasses of DefaultTracer: {0}");
    }

    @Override
    public void markFinishTime() {
        finishTime.compareAndSet(null, System.nanoTime());
    }

    @Override
    public long getStartTimeInMillis() {
        return startTimeInMillis;
    }

    @Override
    public ExternalParameters getExternalParameters() {
        return null;
    }

    @Override
    public void setNoticedError(Throwable throwable) {
        tracerError.compareAndSet(TracedException.NO_EXCEPTION, new TracedException(TransactionErrorPriority.API, throwable));
    }

    @Override
    public void setThrownException(Throwable throwable) {
        tracerError.compareAndSet(TracedException.NO_EXCEPTION, new TracedException(TransactionErrorPriority.TRACER, throwable));
    }

    @Override
    public boolean wasExceptionSetByAPI() {
        return tracerError.get().getPriority() == TransactionErrorPriority.API;
    }

    @Override
    public Throwable getException() {
        return tracerError.get().getException();
    }

    @Override
    public void addCustomAttribute(String key, Number value) {
        setAttributeIfValid(key, value);
    }

    @Override
    public void addCustomAttribute(String key, String value) {
        setAttributeIfValid(key, value);
    }

    @Override
    public void addCustomAttribute(String key, boolean value) {
        setAttributeIfValid(key, value);
    }

    @Override
    public void addCustomAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            Agent.LOG.log(Level.FINER, "Unable to add {0} attributes because {1} was invoked with a null or empty map",
                    ATTRIBUTE_TYPE, ATTRIBUTE_API_METHOD_NAME);
            return;
        }
        for (Map.Entry<String, Object> current : attributes.entrySet()) {
            setAttributeIfValid(current.getKey(), current.getValue());
        }
    }

    private void setAttributeIfValid(String key, Object value) {
        Object verifiedValue = attributeValidator.verifyParameterAndReturnValue(
                key, value, ATTRIBUTE_API_METHOD_NAME);
        if (verifiedValue != null) {
            setAttribute(key, verifiedValue, true, true, false);
        }
    }

    private boolean shouldAddAttribute() {
        return getTransaction() != null && !getTransaction().getTransactionCounts().isOverTracerSegmentLimit();
    }

    public void setAttribute(String key, Object value, boolean checkLimits, boolean isCustom, boolean addAgentAttrToSpan) {
        if (checkLimits && !shouldAddAttribute()) {
            return;
        }

        if (value.getClass().isArray()) {
            value = Arrays.asList((Object[]) value);
        }

        if (checkLimits) {
            getTransaction().getTransactionCounts().incrementSize(sizeof(value));
        }

        if (isCustom) {
            if (customAttributes == null) {
                customAttributes = new HashMap<>(1, INITIAL_PARAMETER_MAP_SIZE);
            }
            customAttributes.put(key, value);
        } else {
            if (agentAttributes == null) {
                agentAttributes = new HashMap<>(1, INITIAL_PARAMETER_MAP_SIZE);
            }
            agentAttributes.put(key, value);
            if(addAgentAttrToSpan) {
                if (agentAttributeNamesForSpans == null) {
                    agentAttributeNamesForSpans = new HashSet<>(INITIAL_PARAMETER_SET_SIZE);
                }
                agentAttributeNamesForSpans.add(key);
            }
        }
    }

    @Override
    public Token getToken() {
        return getTransaction().getToken();
    }

    static int sizeof(Object value) {
        int size = 0;
        if (value == null) {
            return 0;
        } else if (value instanceof String) {
            return ((String) value).length();
        } else if (value instanceof StackTraceElement) {
            StackTraceElement elem = ((StackTraceElement) value);
            // rough size estimate
            return sizeof(elem.getClassName()) + sizeof(elem.getFileName()) + sizeof(elem.getMethodName()) + 10;
        } else if (value instanceof Object[]) {
            for (Object obj : (Object[]) value) {
                size += sizeof(obj);
            }
        }
        return size;
    }

    @Override
    public String getTraceId() {
        return getTransaction().getSpanProxy().getOrCreateTraceId();
    }

    @Override
    public String getSpanId() {
        return getGuid();
    }

    @Override
    public void setAgentAttribute(String key, Object value) {
        setAttribute(key, value, true, false, false);
    }

    @Override
    public void setAgentAttribute(String key, Object value, boolean addToSpan) {
        setAttribute(key, value, true, false, addToSpan);
    }

    @Override
    public void removeAgentAttribute(String key) {
        if (agentAttributes != null) {
            agentAttributes.remove(key);
        }
    }

    @Override
    public Object getAgentAttribute(String key) {
        return ((agentAttributes == null) ? null : agentAttributes.get(key));
    }

    @Override
    public Map<String, Object> getAgentAttributes() {
        if (agentAttributes == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(agentAttributes);
    }

    @Override
    public Map<String, Object> getCustomAttributes() {
        if (customAttributes == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(customAttributes);
    }

    @Override
    public Set<String> getAgentAttributeNamesForSpans() {
        if (agentAttributeNamesForSpans == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(agentAttributeNamesForSpans);
    }

    public Object getCustomAttribute(String key) {
        return ((customAttributes == null) ? null : customAttributes.get(key));

    }

    public static Tracer getParentTracerWithSpan(Tracer tracer) {
        Tracer current = tracer;
        while (current != null && !current.isTransactionSegment()) {
            current = current.getParentTracer();
        }
        return current;
    }

    private static class TracedException {
        private final TransactionErrorPriority priority;
        private final Throwable exception;

        TracedException(TransactionErrorPriority priority, Throwable exception) {
            this.priority = priority;
            this.exception = exception;
        }

        public TransactionErrorPriority getPriority() {
            return priority;
        }

        public Throwable getException() {
            return exception;
        }

        static final TracedException NO_EXCEPTION = new TracedException(null, null);
    }
}
