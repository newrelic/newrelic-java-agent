package com.newrelic.agent.instrumentation.tracing;

import com.newrelic.agent.bridge.TransactionNamePriority;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.context.TraceInformation;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

public class TraceClassVisitorTests {

    @Test
    public void test_bridgeMethod() {
        TraceClassVisitor target = setupTarget(null, null, "()[V");

        MethodVisitor result = target.visitMethod(Opcodes.ACC_BRIDGE, "method", "()[V", "", null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof MethodVisitor);
    }

    @Test
    public void test_ignoreTransactionMethods() {
        MethodVisitor baseVisitor = Mockito.mock(MethodVisitor.class);
        InstrumentationContext context = mock(InstrumentationContext.class);
        TraceClassVisitor target = setupTarget(baseVisitor, context, "()[V");
        MethodVisitor result = target.visitMethod(Opcodes.ACC_FINAL, "txMethod", "()[V", "", null);

        verify(context).markAsModified();
        Assert.assertTrue(result instanceof AdviceAdapter);

        result.visitCode();
        verify(baseVisitor, atLeastOnce()).visitFieldInsn(anyInt(), any(), any(), any());
    }

    @Test
    public void test_regularPathMethod() {
        MethodVisitor baseVisitor = Mockito.mock(MethodVisitor.class);
        Method method = new Method("method", "()[V");
        FlyweightTraceMethodVisitor flyVisitor = new FlyweightTraceMethodVisitor(TraceClassVisitorTests.class.getName(),
                baseVisitor, Opcodes.ACC_PUBLIC, "method", "()[V", getTraceDetails(method), null);
        InstrumentationContext context = mock(InstrumentationContext.class);
        TraceClassVisitor target = setupTarget(flyVisitor, context, "(I)[V");

        MethodVisitor result = target.visitMethod(Opcodes.ACC_FINAL, "method", "(I)[V", null, null);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof AdviceAdapter);

        result.visitCode();
        result.visitMaxs(0, 0);
        result.visitInsn(Opcodes.RETURN);
        verify(baseVisitor, atLeastOnce()).visitInsn(anyInt());
    }

    private TraceClassVisitor setupTarget(MethodVisitor baseVisitor, InstrumentationContext context, String methodDesc) {
        if (baseVisitor == null) baseVisitor = Mockito.mock(MethodVisitor.class);
        ClassVisitor classVisitor = mock(ClassVisitor.class);
        when(classVisitor.visitMethod(anyInt(), any(), any(), any(), any())).thenReturn(baseVisitor);
        String className = TraceClassVisitorTests.class.getName();
        Method ignoredTxMethod = new Method("txMethod", "()[V");
        Method visitedMethod = new Method("method", methodDesc);

        Set<Method> ignoreTxMethods = new HashSet<>();
        ignoreTxMethods.add(ignoredTxMethod);
        TraceInformation traceInfo = mock(TraceInformation.class);
        when(traceInfo.getIgnoreTransactionMethods()).thenReturn(ignoreTxMethods);

        Map<Method, TraceDetails> traceAnnotations = new HashMap<>();
        traceAnnotations.put(visitedMethod, getTraceDetails(visitedMethod));
        when(traceInfo.getTraceAnnotations()).thenReturn(traceAnnotations);
        Set<Method> ignoreApdexMethods = new HashSet<>();
        ignoreApdexMethods.add(visitedMethod);
        when(traceInfo.getIgnoreApdexMethods()).thenReturn(ignoreApdexMethods);

        if (context == null) context = mock(InstrumentationContext.class);
        when(context.getTraceInformation()).thenReturn(traceInfo);

        Set<Method> noticeSqlMethods = new HashSet<>();
        TraceClassVisitor target = new TraceClassVisitor(classVisitor, className, context, noticeSqlMethods);

        return target;
    }

    private TraceDetails getTraceDetails(Method asmMethod) {
        List<ParameterAttributeName> reportedParams = new ArrayList<ParameterAttributeName>();
        MethodMatcher methodMatcher = mock(MethodMatcher.class);
        when(methodMatcher.matches(anyInt(), any(), any(), any())).thenReturn(true);
        reportedParams.add(new ParameterAttributeName(0, "attr1", methodMatcher));
        return TraceDetailsBuilder.newBuilder()
                .setParameterAttributeNames(reportedParams)
                .addRollupMetricName("metricName")
                .setTransactionName(TransactionNamePriority.CUSTOM_HIGH, false, "category", "path")
                .setWebTransaction(true)
                .setMetricPrefix("prefix")
                .build();
    }

    final void method(){}
    final void method(int i){}
}
