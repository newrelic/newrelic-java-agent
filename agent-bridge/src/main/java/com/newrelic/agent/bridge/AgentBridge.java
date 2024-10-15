/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import com.newrelic.agent.bridge.ExtensionHolderFactory.NoOpExtensionHolderFactory;
import com.newrelic.api.agent.MetricAggregator;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;

import java.lang.reflect.InvocationHandler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This implementation of {@link CollectionFactory} will only be used if the agent-bridge
 * is being used by an application and the agent is NOT being loaded. Thus, it is unlikely
 * that the objects created by this implementation are going to receive much use.
 * So methods in this implementation do not need to implement all functional requirements
 * of the methods in the interface, but they should not break under low use.
 */
public final class AgentBridge {
    /**
     * Calls to methods on these classes will automatically be logged at FINEST.
     */
    public static final Class<?>[] API_CLASSES = new Class<?>[] { PrivateApi.class, TracedMethod.class,
            Instrumentation.class, AsyncApi.class, Transaction.class, JmxApi.class, MetricAggregator.class };

    /**
     * The agent sets the actual api implementation when it starts up.
     */
    public static volatile PublicApi publicApi = new NoOpPublicApi();

    public static volatile PrivateApi privateApi = new NoOpPrivateApi();

    public static volatile JmxApi jmxApi = new NoOpJmxApi();

    public static volatile Instrumentation instrumentation = new NoOpInstrumentation();

    public static volatile AsyncApi asyncApi = new NoOpAsyncApi();

    public static volatile CloudApi cloud = NoOpCloud.INSTANCE;

    public static volatile CollectionFactory collectionFactory = new DefaultCollectionFactory();

    /**
     * This is the {@link InvocationHandler} used by the old pointcuts.
     */
    public static volatile InvocationHandler agentHandler;

    public static volatile Agent agent = NoOpAgent.INSTANCE;

    public static Agent getAgent() {
        return agent;
    }

    public static volatile ExtensionHolderFactory extensionHolderFactory = new NoOpExtensionHolderFactory();

    /**
     * This thread local is set directly before a "tracked" API call and unset after the call completes. This allows us
     * to easily track the source of an API call (e.g. - internal weave module, FIT module, custom code, etc). This data
     * is set from AgentPreprocessors.wrapApiCallsForSupportability().
     */
    public static volatile ThreadLocal<WeavePackageType> currentApiSource = new ThreadLocal<WeavePackageType>() {
        @Override
        protected WeavePackageType initialValue() {
            return null;
        }

        @Override
        public WeavePackageType get() {
            try {
                WeavePackageType weavePackageType = super.get();
                if (weavePackageType == null) {
                    return WeavePackageType.UNKNOWN;
                }
                return weavePackageType;
            } catch (Throwable t) {
                // Prevent any exceptions from being thrown out to the caller
                return WeavePackageType.UNKNOWN;
            }
        }

        @Override
        public void set(WeavePackageType value) {
            try {
                super.set(value);
            } catch (Throwable t) {
                // Prevent any exceptions from being thrown out to the caller
            }
        }

        @Override
        public void remove() {
            try {
                super.remove();
            } catch (Throwable t) {
                // Prevent any exceptions from being thrown out to the caller
            }
        }
    };

    // Note: DO NOT USE. This should only be used by our high-throughput scala instrumentation for now
    public static ThreadLocal<TokenAndRefCount> activeToken = new ThreadLocal<TokenAndRefCount>() { };

    public static class TokenAndRefCount {

        public com.newrelic.agent.bridge.Token token;
        public AtomicReference<Object> tracedMethod;
        public AtomicInteger refCount;

        public TokenAndRefCount(com.newrelic.agent.bridge.Token token, TracedMethod tracedMethod, AtomicInteger refCount) {
            this.token = token == null ? NoOpToken.INSTANCE : token;
            this.tracedMethod = new AtomicReference<>(tracedMethod);
            this.refCount = refCount;
        }

    }

}
