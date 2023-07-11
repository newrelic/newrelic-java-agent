package com.newrelic.agent.instrumentation.tracing;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.sql.Connection;
import java.util.Map;

public class TraceMethodVisitorTests {

    @Test
    public void test_visitMethodInsn_tracedMethod() {
        test_visitMethodInsn("com/newrelic/api/agent/Agent",
                BridgeUtils.GET_TRACED_METHOD_METHOD_NAME,
                "()[V",
                1);
    }

    @Test
    public void test_visitMethodInsn_isNoticeSqlMethod() {
        final Map<String, Type> setterNamesToTypes = ImmutableMap.of(
                "provideConnection", Type.getType(Connection.class),
                "setRawSql", Type.getType(String.class),
                "setParams", Type.getType(Object[].class)
        );
        final Method noticeSqlMethod = new Method("noticeSql", Type.VOID_TYPE,
                setterNamesToTypes.values().toArray(new Type[setterNamesToTypes.size()]));

        test_visitMethodInsn(BridgeUtils.DATASTORE_METRICS_TYPE.getInternalName(),
                noticeSqlMethod.getName(),
                noticeSqlMethod.getDescriptor(),
                3);
    }

    @Test
    public void test_visitMethodInsn_anyOtherMethod() {
        test_visitMethodInsn("dummy",
                "dummy",
                "()[V",
                0);
    }

    @Test
    public void test_onMethodEnter_noticeSqlTrue() {
        test_onMethodEnter("dummy", "dummy", "()[V", true, null);
    }

    @Test
    public void test_onMethodEnter_noticeSqlFalse() {
        test_onMethodEnter("dummy", "dummy", "()[V", false, null);
    }

    @Test
    public void test_onMethodEnter_withFactoryName() {
        test_onMethodEnter("dummy", "dummy", "()[V", false, "factory");
    }

    private void test_onMethodEnter(String owner, String method, String desc, boolean noticeSql, String tracerFactoryName) {
        MethodVisitor methodVisitor = Mockito.mock(MethodVisitor.class);
        TraceDetails traceDetails = Mockito.mock(TraceDetails.class);
        Mockito.when(traceDetails.dispatcher()).thenReturn(true);
        Mockito.when(traceDetails.async()).thenReturn(true);
        Mockito.when(traceDetails.isLeaf()).thenReturn(true);
        Mockito.when(traceDetails.tracerFactoryName()).thenReturn(tracerFactoryName);
        TraceMethodVisitor target = new TraceMethodVisitor(owner, methodVisitor, -1,
                method, desc, traceDetails, true, noticeSql, null);

        target.onMethodEnter();

        Mockito.verify(methodVisitor, Mockito.times(1)).visitInsn(Opcodes.POP);
    }

    private void test_visitMethodInsn(String owner, String method, String desc, int popCount) {
        MethodVisitor methodVisitor = Mockito.mock(MethodVisitor.class);
        TraceDetails traceDetails = Mockito.mock(TraceDetails.class);
        TraceMethodVisitor target = new TraceMethodVisitor(owner, methodVisitor, -1,
                method, desc, traceDetails, false, false, TraceMethodVisitorTests.class);

        target.visitMethodInsn(-1, owner, method, desc, false);

        Mockito.verify(methodVisitor, Mockito.times(popCount)).visitInsn(Opcodes.POP);
    }
}
