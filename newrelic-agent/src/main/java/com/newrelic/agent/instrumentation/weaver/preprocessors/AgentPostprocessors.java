/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.tracing.TraceClassVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.PrivateApi;
import com.newrelic.agent.bridge.PublicApi;
import com.newrelic.agent.bridge.TracedActivity;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.weaver.internal.WeavePackageType;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePostprocessor;

public class AgentPostprocessors implements WeavePostprocessor {

    private WeavePackageType weavePackageType = WeavePackageType.CUSTOM;
    private final ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails = new ConcurrentHashMap<>();

    public void setWeavePackageType(WeavePackageType weavePackageType) {
        this.weavePackageType = weavePackageType;
    }

    /**
     * Set a map where each key is a weave package and each map entry is a set of {@link TracedWeaveInstrumentationTracker}s.
     * <p>
     * Each TracedWeaveInstrumentationTracker represents a method with the TracedWeaveInstrumentation type {@link InstrumentationType},
     * which indicates that the method was instrumented via @Trace annotation from weaved instrumentation.
     *
     * @param tracedWeaveInstrumentationDetails map of weave packages, each with and associated set of TracedWeaveInstrumentationTrackers
     */
    public void setTracedWeaveInstrumentationDetails(ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails) {
        this.tracedWeaveInstrumentationDetails.putAll(tracedWeaveInstrumentationDetails);
    }

    @Override
    public ClassVisitor postprocess(String className, ClassVisitor cv, Set<String> utilityClassesInternalNames,
            WeavePackage weavePackage, boolean isUtilityClass) {
        cv = wrapApiCallsForSupportability(cv);

        // Any class that doesn't have a @Weave or @SkipIfPresent annotation on it is considered to be a utility class. {see com.newrelic.weave.UtilityClass}
        if (isUtilityClass) {
            InstrumentationContext context = new InstrumentationContext(null, null, null);
            this.setTracedWeaveInstrumentationDetails(((AgentPreprocessors) weavePackage.getConfig().getPreprocessor()).getTracedWeaveInstrumentationDetails());
            Set<TracedWeaveInstrumentationTracker> tracedWeaveInstrumentationTrackers = tracedWeaveInstrumentationDetails.get(weavePackage.getName());
            if (tracedWeaveInstrumentationTrackers != null) {
                for (TracedWeaveInstrumentationTracker tracedWeaveInstrumentationTracker : tracedWeaveInstrumentationTrackers) {
                    tracedWeaveInstrumentationTracker.addToInstrumentationContext(context, tracedWeaveInstrumentationTracker.getMethod());
                }
            }
            cv = new TraceClassVisitor(cv, className, context, Collections.<Method>emptySet());
        }

        return cv;
    }


    ClassVisitor wrapApiCallsForSupportability(ClassVisitor cv) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public MethodVisitor visitMethod(int access, final String methodName, String methodDesc, String signature,
                    String[] exceptions) {
                final MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);

                return new GeneratorAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, methodName, methodDesc) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        Method currentMethod = new Method(name, desc);
                        Collection<Method> methods = TRACKED_API_INTERNAL_NAME_TO_METHOD_NAMES.get(owner);
                        if (methods != null && methods.contains(currentMethod)) {
                            // Set the thread local to the current type of caller (e.g. - FIELD, INTERNAL, CUSTOM, etc)
                            System.out.println("JGB: setting api source: AgentPostProcessor");
                            setCurrentApiSource(weavePackageType);

                            // Make the original API call
                            super.visitMethodInsn(opcode, owner, name, desc, itf);

                            // Remove the thread local. If we hit an exception before we get here it will just be
                            // overwritten before the next call. So we don't need to worry about a try/catch/finally
                            System.out.println("JGB: removing api source: AgentPostProcessor");
                            unsetCurrentApiSource();
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    }

                    /**
                     * Generate the instructions required to set the value of the api source thread local.
                     *
                     * e.g: <code>AgentBridge.currentApiSource.set(WeavePackageType.INTERNAL);</code>
                     *
                     * NOTE: If you modify this method you MUST also update "visitMaxs" above to specify the correct
                     * number of parameters that the "currentApiSource" method call pushes on to the stack.
                     *
                     * @param weavePackageType the source of the API caller to set
                     */
                    private void setCurrentApiSource(WeavePackageType weavePackageType) {
                        getStatic(Type.getType(AgentBridge.class), "currentApiSource",
                                Type.getType(ThreadLocal.class));
                        getStatic(Type.getType(WeavePackageType.class), weavePackageType.name(),
                                Type.getType(WeavePackageType.class));
                        invokeVirtual(Type.getType(ThreadLocal.class), new Method("set", "(Ljava/lang/Object;)V"));
                    }

                    /**
                     * Generate the instructions required to unset the value of the api source thread local.
                     *
                     * e.g: <code>AgentBridge.currentApiSource.remove();</code>
                     */
                    private void unsetCurrentApiSource() {
                        getStatic(Type.getType(AgentBridge.class), "currentApiSource",
                                Type.getType(ThreadLocal.class));
                        invokeVirtual(Type.getType(ThreadLocal.class), new Method("remove", "()V"));
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        // We add two variables to the stack when we push the "currentApiSource" static field on the
                        // stack and when we push the "weavePackageType" parameter on the stack (maxStack = 2).
                        super.visitMaxs(maxStack + 2, maxLocals);
                    }
                };
            }
        };
    }

    void addTrackedApiMethods(Multimap<String, Method> methodsToAdd) {
        Multimap<String, Method> updatedMultimap = ImmutableMultimap
                .<String, Method>builder()
                .putAll(TRACKED_API_INTERNAL_NAME_TO_METHOD_NAMES)
                .putAll(methodsToAdd)
                .build();

        TRACKED_API_INTERNAL_NAME_TO_METHOD_NAMES = updatedMultimap;
    }

    /**
     * This Multimap contains all of the classes and methods to automatically wrap with the supportability thread local
     * call. This multimap tracks bridge AND API types and method calls.
     *
     * The key of this multimap is the Class that contains the method, and the values are any methods to match.
     *
     * Example:
     *
     * "com/newrelic/api/agent/Transaction"    -> startSegment(String segmentName)
     *                                         -> startSegment(String category, String segmentName)
     *
     * "com/newrelic/agent/bridge/Transaction" -> createAndStartTracedActivity()
     */
    private static Multimap<String, Method> TRACKED_API_INTERNAL_NAME_TO_METHOD_NAMES = ImmutableMultimap
            .<String, Method>builder()

            // Transaction API - Token - Token getToken()
            .put(Type.getType(Transaction.class).getInternalName(), new Method("getToken", Type.getType(Token.class),
                    new Type[] {}))
            // Transaction Bridge API - Token - Token getToken()
            .put(Type.getType(com.newrelic.agent.bridge.Transaction.class).getInternalName(),
                    new Method("getToken", Type.getType(com.newrelic.agent.bridge.Token.class), new Type[] {}))

            // Transaction API - Segment - Segment startSegment(String segmentName)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("startSegment",
                    Type.getType(Segment.class), new Type[] { Type.getType(String.class) }))
            // Transaction API - Segment - Segment startSegment(String category, String segmentName)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("startSegment",
                    Type.getType(Segment.class), new Type[] { Type.getType(String.class), Type.getType(String.class) }))
            // Transaction Bridge API - Traced Activity - TracedActivity createAndStartTracedActivity()
            .put(Type.getType(com.newrelic.agent.bridge.Transaction.class).getInternalName(),
                    new Method("createAndStartTracedActivity", Type.getType(TracedActivity.class), new Type[] {}))

            // Transaction API - Ignore - void ignore()
            .put(Type.getType(Transaction.class).getInternalName(), new Method("ignore", Type.VOID_TYPE,
                    new Type[] {}))

            // Transaction API - IgnoreApdex - void ignoreApdex()
            .put(Type.getType(Transaction.class).getInternalName(), new Method("ignoreApdex", Type.VOID_TYPE,
                    new Type[] {}))

            // Transaction API - ProcessRequestMetadata - void processRequestMetadata(String requestMetadata)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("processRequestMetadata", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Transaction API - ProcessResponseMetadata - void processResponseMetadata(String responseMetadata)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("processResponseMetadata", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))
            // Transaction API - ProcessResponseMetadata - void processResponseMetadata(String responseMetadata, URI uri)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("processResponseMetadata", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(URI.class) }))

            // Transaction API - SetTransactionName - boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts)
            .put(Type.getType(Transaction.class).getInternalName(), new Method("setTransactionName", Type.BOOLEAN_TYPE,
                    new Type[] { Type.getType(TransactionNamePriority.class), Type.BOOLEAN_TYPE, Type.getType(String.class), Type.getType(String[].class) }))

            // Transaction Bridge API - SetTransactionName - boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category, String... parts)
            .put(Type.getType(com.newrelic.agent.bridge.Transaction.class).getInternalName(), new Method("setTransactionName", Type.BOOLEAN_TYPE,
                    new Type[] { Type.getType(TransactionNamePriority.class), Type.BOOLEAN_TYPE, Type.getType(String.class), Type.getType(String[].class) }))

            // TracedMethod API - ReportAsExternal - void reportAsExternal(ExternalParameters externalParameters)
            .put(Type.getType(TracedMethod.class).getInternalName(), new Method("reportAsExternal", Type.VOID_TYPE,
                    new Type[] { Type.getType(ExternalParameters.class) }))
            // TracedMethod Bridge API - ReportAsExternal - void reportAsExternal(com.newrelic.agent.bridge.external.ExternalParameters externalParameters)
            .put(Type.getType(com.newrelic.agent.bridge.TracedMethod.class).getInternalName(), new Method("reportAsExternal", Type.VOID_TYPE,
                    new Type[] { Type.getType(com.newrelic.agent.bridge.external.ExternalParameters.class) }))

            // Insights API - RecordCustomEvent - void recordCustomEvent(String eventType, Map<String, Object> attributes)
            .put(Type.getType(Insights.class).getInternalName(), new Method("recordCustomEvent", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class) }))

            // Segment API - Segment/End - void end()
            .put(Type.getType(Segment.class).getInternalName(), new Method("end", Type.VOID_TYPE,
                    new Type[] {}))

            // Segment API - Segment/Ignore - void ignore()
            .put(Type.getType(Segment.class).getInternalName(), new Method("ignore", Type.VOID_TYPE,
                    new Type[] {}))

            // Segment API - ReportAsExternal - void reportAsExternal(ExternalParameters externalParameters)
            .put(Type.getType(Segment.class).getInternalName(), new Method("reportAsExternal", Type.VOID_TYPE,
                    new Type[] { Type.getType(ExternalParameters.class) }))

            // Segment API - Segment/SetMetricName - void setMetricName(String... metricNameParts)
            .put(Type.getType(Segment.class).getInternalName(), new Method("setMetricName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String[].class) }))

            // Traced Method API - Segment/SetMetricName - void setMetricName(String... metricNameParts)
            .put(Type.getType(TracedMethod.class).getInternalName(), new Method("setMetricName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String[].class) }))

            // Traced Method Bridge API - Segment/SetMetricName - void setMetricName(String... metricNameParts)
            .put(Type.getType(com.newrelic.agent.bridge.TracedMethod.class).getInternalName(),
                    new Method("setMetricName", Type.VOID_TYPE, new Type[] { Type.getType(String[].class) }))

            // Token API - Token/Link - boolean link()
            .put(Type.getType(Token.class).getInternalName(), new Method("link", Type.BOOLEAN_TYPE,
                    new Type[] {}))

            // Token API - Token/Expire - boolean expire()
            .put(Type.getType(Token.class).getInternalName(), new Method("expire", Type.BOOLEAN_TYPE,
                    new Type[] {}))

            // Token API - Token/Link, Token/Expire - boolean linkAndExpire()
            .put(Type.getType(Token.class).getInternalName(), new Method("linkAndExpire", Type.BOOLEAN_TYPE,
                    new Type[] {}))

            // NewRelic API - AddCustomParameter - public static void addCustomParameter(String key, Number value)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("addCustomParameter", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Number.class) }))
            // NewRelic API - AddCustomParameter - public static void addCustomParameter(String key, String value)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("addCustomParameter", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))
            // Public API - AddCustomParameter - public static void addCustomParameter(String key, Number value)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("addCustomParameter", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Number.class) }))
            // Public API - AddCustomParameter - public static void addCustomParameter(String key, String value)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("addCustomParameter", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))
            // Private API - AddCustomAttribute - public static void addCustomAttribute(String key, Number value)
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("addCustomAttribute", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Number.class) }))
            // Private API - AddCustomAttribute - public static void addCustomAttribute(String key, String value)
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("addCustomAttribute", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))
            // Private API - AddCustomAttribute - public static void addCustomAttribute(String key, Map<String, String> values);
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("addCustomAttribute", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class) }))

            // NewRelic API - Ignore - public static void ignoreTransaction()
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("ignoreTransaction", Type.VOID_TYPE,
                    new Type[] {}))
            // Public API - Ignore - public void ignoreTransaction()
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("ignoreTransaction", Type.VOID_TYPE,
                    new Type[] {}))

            // NewRelic API - IgnoreApdex - public static void ignoreApdex()
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("ignoreApdex", Type.VOID_TYPE,
                    new Type[] {}))
            // Public API - IgnoreApdex - public void ignoreApdex()
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("ignoreApdex", Type.VOID_TYPE,
                    new Type[] {}))

            // NewRelic API - NoticeError - public static void noticeError(Throwable throwable, Map<String, String> params, boolean expected)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.getType(Map.class), Type.BOOLEAN_TYPE }))
            // NewRelic API - NoticeError - public static void noticeError(Throwable throwable, boolean expected)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.BOOLEAN_TYPE }))

            // NewRelic API - NoticeError - public static void noticeError(String message, Map<String, String> params, boolean expected)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class), Type.BOOLEAN_TYPE }))
            // NewRelic API - NoticeError - public static void noticeError(String message, boolean expected)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.BOOLEAN_TYPE }))
            // NewRelic API - NoticeError - public static void noticeError(Throwable throwable, Map<String, String> params)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.getType(Map.class) }))
            // NewRelic API - NoticeError - public static void noticeError(Throwable throwable)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class) }))
            // NewRelic API - NoticeError - public static void noticeError(String message, Map<String, String> params)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class) }))
            // NewRelic API - NoticeError - public static void noticeError(String message)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Public API - NoticeError - public static void noticeError(Throwable throwable, Map<String, String> params, boolean expected)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.getType(Map.class), Type.BOOLEAN_TYPE }))
            // Public API - NoticeError - public static void noticeError(Throwable throwable, boolean expected)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.BOOLEAN_TYPE }))

            // Public API - NoticeError - public static void noticeError(String message, Map<String, String> params, boolean expected)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class), Type.BOOLEAN_TYPE }))
            // Public API - NoticeError - public static void noticeError(String message, boolean expected)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.BOOLEAN_TYPE }))
            // Public API - NoticeError - public static void noticeError(Throwable throwable, Map<String, String> params)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class), Type.getType(Map.class) }))
            // Public API - NoticeError - public static void noticeError(Throwable throwable)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(Throwable.class) }))
            // Public API - NoticeError - public static void noticeError(String message, Map<String, String> params)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(Map.class) }))
            // Public API - NoticeError - public static void noticeError(String message)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("noticeError", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // NewRelic API - SetAppServerPort - public static void setAppServerPort(int port)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setAppServerPort", Type.VOID_TYPE,
                    new Type[] { Type.INT_TYPE }))

            // Public API - SetAppServerPort - public static void setAppServerPort(int port)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setAppServerPort", Type.VOID_TYPE,
                    new Type[] { Type.INT_TYPE }))

            // Private API - SetAppServerPort - public static void setAppServerPort(int port)
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("setAppServerPort", Type.VOID_TYPE,
                    new Type[] { Type.INT_TYPE }))

            // NewRelic API - SetInstanceName - public static void setInstanceName(String instanceName)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setInstanceName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Public API - SetInstanceName - public static void setInstanceName(String instanceName)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setInstanceName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Private API - SetInstanceName - public static void setInstanceName(String instanceName)
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("setInstanceName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // NewRelic API - SetProductName - public static void setProductName(String name)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setProductName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Public API - SetProductName - public static void setProductName(String name)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setProductName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // NewRelic API - SetServerInfo - public static void setServerInfo(String dispatcherName, String version)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setServerInfo", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))

            // Public API - SetServerInfo - public static void setServerInfo(String dispatcherName, String version)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setServerInfo", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))
            // Private API - SetServerInfo - public static void setServerInfo(String serverInfo)
            .put(Type.getType(PrivateApi.class).getInternalName(), new Method("setServerInfo", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // NewRelic API - SetTransactionName - public static void setTransactionName(String category, String name)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setTransactionName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))

            // Public API - SetTransactionName - public static void setTransactionName(String category, String name)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setTransactionName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class), Type.getType(String.class) }))

            // NewRelic API - SetUserName - public static void setUserName(String name)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setUserName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Public API - SetUserName - public static void setUserName(String name)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setUserName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // NewRelic API - SetAccountName - public static void setAccountName(String name)
            .put(Type.getType(NewRelic.class).getInternalName(), new Method("setAccountName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            // Public API - SetAccountName - public static void setAccountName(String name)
            .put(Type.getType(PublicApi.class).getInternalName(), new Method("setAccountName", Type.VOID_TYPE,
                    new Type[] { Type.getType(String.class) }))

            .build();

}
