package com.newrelic.agent.instrumentation;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class MethodBuilderTest {

    @Test
    public void test_basicSetup() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadInvocationHandlerFromProxy();
        Assert.assertEquals(target.getGeneratorAdapter(), mv);
        Assert.assertEquals(target, result);
    }

    @Test
    public void test_invokeIncationHandlerInterface_withPop() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.invokeInvocationHandlerInterface(true);
        Mockito.verify(mv).invokeInterface(Mockito.any(), Mockito.any());
        Mockito.verify(mv).pop();
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_invokeIncationHandlerInterface_withoutPop() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.invokeInvocationHandlerInterface(false);
        Mockito.verify(mv).invokeInterface(Mockito.any(), Mockito.any());
        Mockito.verify(mv, Mockito.times(0)).pop();
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadSuccessful() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadSuccessful();
        Mockito.verify(mv).visitLdcInsn(AgentWrapper.SUCCESSFUL_METHOD_INVOCATION);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadUnsuccessful() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadUnsuccessful();
        Mockito.verify(mv).visitLdcInsn(AgentWrapper.UNSUCCESSFUL_METHOD_INVOCATION);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadInvocationHandlerProxyAndMethod_valueIsNull() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadInvocationHandlerProxyAndMethod(null);
        Mockito.verify(mv, Mockito.times(2)).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadInvocationHandlerProxyAndMethod_valueIsBoolean() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadInvocationHandlerProxyAndMethod(true);
        Mockito.verify(mv).push(true);
        Mockito.verify(mv).box(Type.BOOLEAN_TYPE);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadInvocationHandlerProxyAndMethod_valueIsInteger() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadInvocationHandlerProxyAndMethod(100);
        Mockito.verify(mv).visitIntInsn(Opcodes.SIPUSH, 100);
        Mockito.verify(mv).box(Type.INT_TYPE);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadInvocationHandlerProxyAndMethod_valueIsSomethingElse() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadInvocationHandlerProxyAndMethod("string");
        Mockito.verify(mv).visitLdcInsn("string");
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_empty() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadArray(null, null);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_withLOAD_THIS_static() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_STATIC);

        MethodBuilder result = target.loadArray(Object.class, MethodBuilder.LOAD_THIS);
        Mockito.verify(mv).push(1);
        Mockito.verify(mv).newArray(Mockito.any());
        Mockito.verify(mv).dup();
        Mockito.verify(mv).push(0);
        Mockito.verify(mv).visitInsn(Opcodes.ACONST_NULL);
        Mockito.verify(mv).arrayStore(Mockito.any());
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_withLOAD_THIS_notStatic() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadArray(Object.class, MethodBuilder.LOAD_THIS);
        Mockito.verify(mv).push(1);
        Mockito.verify(mv).newArray(Mockito.any());
        Mockito.verify(mv).dup();
        Mockito.verify(mv).push(0);
        Mockito.verify(mv).loadThis();
        Mockito.verify(mv).arrayStore(Mockito.any());
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_withLOAD_ARG_ARRAY() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadArray(Object.class, MethodBuilder.LOAD_ARG_ARRAY);
        Mockito.verify(mv).push(1);
        Mockito.verify(mv).newArray(Mockito.any());
        Mockito.verify(mv).dup();
        Mockito.verify(mv).push(0);
        Mockito.verify(mv).loadArgArray();
        Mockito.verify(mv).arrayStore(Mockito.any());
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_withRunnable() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        final boolean[] itRan = { false };
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                itRan[0] = true;
            }
        };
        MethodBuilder result = target.loadArray(Object.class, runnable);
        Mockito.verify(mv).push(1);
        Mockito.verify(mv).newArray(Mockito.any());
        Mockito.verify(mv).dup();
        Mockito.verify(mv).push(0);
        Assert.assertTrue(itRan[0]);
        Mockito.verify(mv).arrayStore(Mockito.any());
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_loadArray_withSomethingElse() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        MethodBuilder result = target.loadArray(Object.class, 100);
        Mockito.verify(mv).push(1);
        Mockito.verify(mv).newArray(Mockito.any());
        Mockito.verify(mv).dup();
        Mockito.verify(mv).push(0);
        Mockito.verify(mv).visitIntInsn(Opcodes.SIPUSH, 100);
        Mockito.verify(mv).box(Type.INT_TYPE);
        Mockito.verify(mv).arrayStore(Mockito.any());
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertNotEquals(result, mv);
    }

    @Test
    public void test_box_withObject() {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        Type inputType = Type.getType(Object.class);
        Type result = target.box(inputType);
        Assert.assertEquals(result, inputType);
    }

    @Test
    public void test_box_withVarious() {
        test_box(Type.BOOLEAN_TYPE, Type.getType(Boolean.class));
        test_box(Type.INT_TYPE, Type.getType(Integer.class));
        test_box(Type.BYTE_TYPE, Type.getType(Byte.class));
        test_box(Type.CHAR_TYPE, Type.getType(Character.class));
        test_box(Type.DOUBLE_TYPE, Type.getType(Double.class));
        test_box(Type.FLOAT_TYPE, Type.getType(Float.class));
        test_box(Type.LONG_TYPE, Type.getType(Long.class));
        test_box(Type.SHORT_TYPE, Type.getType(Short.class));
    }

    private void test_box(Type inputType, Type expectedType) {
        GeneratorAdapter mv = Mockito.mock(GeneratorAdapter.class);
        MethodBuilder target = new MethodBuilder(mv, Opcodes.ACC_PUBLIC);

        Type result = target.box(inputType);
        Mockito.verify(mv).invokeStatic(expectedType, new Method("valueOf", expectedType, new Type[] { inputType }));
        Mockito.verifyNoMoreInteractions(mv);
        Assert.assertEquals(result, expectedType);
    }
}
