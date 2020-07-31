/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.tracing;

import com.google.common.collect.ImmutableSet;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Instrumentation;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.datastore.DatastoreMetrics;
import com.newrelic.agent.instrumentation.context.InstrumentationContextManager;
import com.newrelic.agent.util.asm.BytecodeGenProxyBuilder;
import com.newrelic.agent.util.asm.VariableLoader;
import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weaver;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.Set;
import java.util.logging.Level;

public class BridgeUtils {

    public static final Type NEW_RELIC_API_TYPE = Type.getType(NewRelic.class);

    public static final Type PRIVATE_API_TYPE = Type.getType(PrivateApi.class);

    public static final Type PUBLIC_API_TYPE = Type.getType(PublicApi.class);

    public static final Type AGENT_BRIDGE_TYPE = Type.getType(AgentBridge.class);

    public static final Type TRACED_METHOD_TYPE = Type.getType(TracedMethod.class);

    public static final Type PUBLIC_AGENT_TYPE = Type.getType(Agent.class);

    public static final Type INTERNAL_AGENT_TYPE = Type.getType(com.newrelic.agent.bridge.Agent.class);

    public static final Type INSTRUMENTATION_TYPE = Type.getType(Instrumentation.class);

    public static final Type TRANSACTION_TYPE = Type.getType(Transaction.class);

    public static final Type DATASTORE_METRICS_TYPE = Type.getType(DatastoreMetrics.class);

    public static final Type AGENT_BRIDGE_AGENT_TYPE = Type.getType(com.newrelic.agent.bridge.Agent.class);

    /**
     * {@link AgentBridge#privateApi}
     */
    public static final String PRIVATE_API_FIELD_NAME = "privateApi";

    public static final String PUBLIC_API_FIELD_NAME = "publicApi";

    /**
     * {@link AgentBridge#instrumentation}
     */
    public static final String INSTRUMENTATION_FIELD_NAME = "instrumentation";

    public static final String GET_TRACED_METHOD_METHOD_NAME = "getTracedMethod";

    public static final String GET_TRANSACTION_METHOD_NAME = "getTransaction";

    /**
     * {@link AgentBridge#agent}
     */
    private static final String AGENT_FIELD_NAME = "agent";

    public static final String AGENT_BRIDGE_GET_AGENT_METHOD = "getAgent";

    /**
     * {@link Agent#getLogger()}
     */
    private static final String GET_LOGGER_METHOD_NAME = "getLogger";

    private static final Type LOGGER_TYPE = Type.getType(Logger.class);

    public static final Type WEAVER_TYPE = Type.getType(Weaver.class);

    /**
     * References to {@literal Transaction#CURRENT} get rewritten in the final pass in {@link InstrumentationContextManager}.
     * This field was removed, but we still rewrite references to it to maintain backward compatibility.
     */
    public static final String CURRENT_TRANSACTION_FIELD_NAME = "CURRENT";

    public static final String CURRENT_TX_OR_NULL_FIELD_NAME = "CURRENT_OR_NULL";

    /**
     * Load the logger instance onto the stack.
     */
    public static void loadLogger(GeneratorAdapter mv) {

        mv.visitFieldInsn(Opcodes.GETSTATIC, BridgeUtils.AGENT_BRIDGE_TYPE.getInternalName(),
                BridgeUtils.AGENT_FIELD_NAME, BridgeUtils.INTERNAL_AGENT_TYPE.getDescriptor());

        mv.invokeInterface(BridgeUtils.PUBLIC_AGENT_TYPE, new Method(GET_LOGGER_METHOD_NAME, LOGGER_TYPE, new Type[0]));
    }

    public static BytecodeGenProxyBuilder<Logger> getLoggerBuilder(GeneratorAdapter mv, boolean loadArgs) {
        BytecodeGenProxyBuilder<Logger> builder = BytecodeGenProxyBuilder.newBuilder(Logger.class, mv, loadArgs);
        if (loadArgs) {
            builder.addLoader(Type.getType(Level.class), new VariableLoader() {

                @Override
                public void load(Object value, GeneratorAdapter methodVisitor) {
                    methodVisitor.getStatic(Type.getType(Level.class), ((Level) value).getName(),
                            Type.getType(Level.class));
                }
            });
        }

        return builder;
    }

    /**
     * Injects the instructions to load the logger instance and returns a proxy that can be used to invoke methods on
     * that instance.
     */
    public static Logger getLogger(GeneratorAdapter mv) {
        loadLogger(mv);
        return getLoggerBuilder(mv, true).build();
    }

    /**
     * Generates bytecode instructions to load the current transaction through {@literal Transaction#CURRENT}.
     */
    public static void getCurrentTransaction(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, BridgeUtils.TRANSACTION_TYPE.getInternalName(),
                BridgeUtils.CURRENT_TRANSACTION_FIELD_NAME, BridgeUtils.TRANSACTION_TYPE.getDescriptor());
    }

    public static void getCurrentTransactionOrNull(MethodVisitor mv) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, BridgeUtils.TRANSACTION_TYPE.getInternalName(),
                BridgeUtils.CURRENT_TX_OR_NULL_FIELD_NAME, BridgeUtils.TRANSACTION_TYPE.getDescriptor());
    }

    private static final Set<String> AGENT_CLASS_NAMES = ImmutableSet.of(PUBLIC_AGENT_TYPE.getInternalName(),
            INTERNAL_AGENT_TYPE.getInternalName());

    public static boolean isAgentType(String owner) {
        return AGENT_CLASS_NAMES.contains(owner);
    }

    private static final Set<String> TRACED_METHOD_CLASS_NAMES = ImmutableSet.of(TRACED_METHOD_TYPE.getInternalName(),
            Type.getInternalName(com.newrelic.api.agent.TracedMethod.class));

    public static boolean isTracedMethodType(String owner) {
        return TRACED_METHOD_CLASS_NAMES.contains(owner);
    }

    private static final Set<String> TRANSACTION_CLASS_NAMES = ImmutableSet.of(TRANSACTION_TYPE.getInternalName(),
            Type.getInternalName(com.newrelic.api.agent.Transaction.class),
            Type.getInternalName(com.newrelic.agent.Transaction.class));

    public static boolean isTransactionType(String owner) {
        return TRANSACTION_CLASS_NAMES.contains(owner);
    }

}
