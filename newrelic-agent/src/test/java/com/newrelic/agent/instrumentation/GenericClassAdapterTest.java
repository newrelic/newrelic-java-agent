package com.newrelic.agent.instrumentation;

import com.newrelic.agent.TracerService;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.service.ServiceFactory;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GenericClassAdapterTest {

    private ClassWriter cw;
    private InstrumentationContext context;
    private PointCut pointCut;
    private MethodVisitor mv;

    @Test
    public void visit_interface() {
        try {
            GenericClassAdapter adapter = getClassAdapter("java.util.Collection", Collection.class, true);

            adapter.visit(52, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE,  "java/util/Collection", "<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/lang/Iterable<TE;>;", "java/lang/Object", new String[]{Iterable.class.getName()});

            fail("Should have thrown exception");
        } catch (StopProcessingException e) {
            // expected
        }
    }

    @Test
    public void visit_class() throws NoSuchFieldException, IllegalAccessException {
        GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);

        adapter.visit(52, Opcodes.ACC_PUBLIC,  "java/lang/Integer", null, "java/lang/Number", new String[]{Comparable.class.getName()});

        Field versionField = GenericClassAdapter.class.getDeclaredField("version");
        versionField.setAccessible(true);
        assertEquals(52, versionField.getInt(adapter));
    }

    @Test
    public void canModifyClassStructure() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);

            boolean result = adapter.canModifyClassStructure();

            assertTrue(result);
        }
    }

    @Test
    public void visitMethod_classInit() throws IllegalAccessException, NoSuchFieldException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            Field processedClinitMethodField = GenericClassAdapter.class.getDeclaredField("processedClassInitMethod");
            processedClinitMethodField.setAccessible(true);

            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);
            assertThat(processedClinitMethodField.getBoolean(adapter), is(false));


            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_PUBLIC, "<clinit>", "()V", null, new String[0]);

            assertThat(result.getClass().getName(), equalTo("com.newrelic.agent.instrumentation.GenericClassAdapter$InitMethodAdapter"));
            assertThat(processedClinitMethodField.getBoolean(adapter), is(true));
        }
    }

    @Test
    public void visitMethod_abstractMethod() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.util.AbstractList", AbstractList.class, true);

            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_ABSTRACT, "get", "(I)TE;", "(I)TE;", new String[0]);

            assertThat(result, sameInstance(mv));
            verifyNoInteractions(context);
        }
    }

    @Test
    public void visitMethod_noPointCutMatches() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, false);

            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_PUBLIC, "shortValue", "()S", null, new String[0]);

            assertThat(result, sameInstance(mv));
            verify(pointCut.getMethodMatcher()).matches(anyInt(),anyString(),anyString(),any(Set.class));
            verifyNoMoreInteractions(context);
        }
    }

    @Test
    public void visitMethod_pointCutCanModifyStructure() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);

            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_PUBLIC, "shortValue", "()S", null, new String[0]);

            assertThat(result, instanceOf(InvocationHandlerTracingMethodAdapter.class));
            verify(pointCut.getMethodMatcher()).matches(anyInt(), anyString(), anyString(), any(Set.class));
            verify(context).addOldInvokerStyleInstrumentationMethod(eq(new Method("shortValue", "()S")), eq(pointCut));

            List<PointCut> appliedPointCuts = adapter.getAppliedPointCuts();
            assertThat(appliedPointCuts, hasSize(1));
            assertThat(appliedPointCuts.get(0), sameInstance(pointCut));
        }
    }

    @Test
    public void visitMethod_pointCutCannotModifyStructure() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class);
             MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            TracerService tracerService = mock(TracerService.class);
            serviceFactory.when(ServiceFactory::getTracerService)
                    .thenReturn(tracerService);
            when(tracerService.getInvocationHandlerId(any()))
                    .thenReturn(42);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);

            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_PUBLIC, "shortValue", "()S", null, new String[0]);

            assertThat(result, instanceOf(ReflectionStyleClassMethodAdapter.class));
            verify(pointCut.getMethodMatcher()).matches(anyInt(), anyString(), anyString(), any(Set.class));
            verify(context).addOldReflectionStyleInstrumentationMethod(eq(new Method("shortValue", "()S")), eq(pointCut));

            List<PointCut> appliedPointCuts = adapter.getAppliedPointCuts();
            assertThat(appliedPointCuts, hasSize(1));
            assertThat(appliedPointCuts.get(0), sameInstance(pointCut));
        }
    }

    @Test
    public void visitMethod_pointCutCannotModifyStructureCannotFindInvocationHandler() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class);
             MockedStatic<ServiceFactory> serviceFactory = mockStatic(ServiceFactory.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            TracerService tracerService = mock(TracerService.class);
            serviceFactory.when(ServiceFactory::getTracerService)
                    .thenReturn(tracerService);
            when(tracerService.getInvocationHandlerId(any()))
                    .thenReturn(-1);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);

            MethodVisitor result = adapter.visitMethod(Opcodes.ACC_PUBLIC, "shortValue", "()S", null, new String[0]);

            assertThat(result, sameInstance(mv));
            verify(pointCut.getMethodMatcher()).matches(anyInt(), anyString(), anyString(), any(Set.class));
            verify(tracerService).getInvocationHandlerId(any());
        }
    }

    @Test
    public void visitEnd_mustAddNrClassInit() {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            GenericClassAdapter adapter = getClassAdapter("com.newrelic.agent.instrumentation.GenericClassAdapterTest$NrClassInit", NrClassInit.class, true);
            when(cw.visitMethod(eq(Opcodes.ACC_STATIC), eq("<clinit>"), eq("()V"), isNull(), isNull()))
                    .thenReturn(mock(MethodVisitor.class));
            adapter.getInstrumentedMethods().add(mock(AbstractTracingMethodAdapter.class));
            adapter.visitEnd();

            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("__nr__initClass"), eq("()V"), isNull(), isNull());
            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("<clinit>"), eq("()V"), isNull(), isNull());

        }
    }

    @Test
    public void visitEnd_canNotModifyClassStructureAndHasInstrumentedMethods() throws NoSuchFieldException, IllegalAccessException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Object", Object.class, true);
            adapter.getInstrumentedMethods().add(mock(AbstractTracingMethodAdapter.class));

            adapter.visitEnd();

            verify(cw).visitEnd();
            verifyNoMoreInteractions(cw);
        }
    }

    @Test
    public void visitEnd_canModifyClassStructureAndProcessedInit() throws NoSuchFieldException, IllegalAccessException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Object", Object.class, true);
            setProcessedClassInitMethod(adapter);

            adapter.visitEnd();

            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("__nr__initClass"), eq("()V"), isNull(), isNull());
            verify(cw).visitEnd();
            verifyNoMoreInteractions(cw);
        }
    }

    @Test
    public void visitEnd_canModifyClassStructureAndHasInstrumentedMethods() throws NoSuchFieldException, IllegalAccessException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Object", Object.class, true);
            adapter.getInstrumentedMethods().add(mock(AbstractTracingMethodAdapter.class));
            when(cw.visitMethod(eq(Opcodes.ACC_STATIC), eq("<clinit>"), eq("()V"), isNull(), isNull()))
                    .thenReturn(mock(MethodVisitor.class));

            adapter.visitEnd();

            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("__nr__initClass"), eq("()V"), isNull(), isNull());
            verify(cw).visitField(eq(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE), eq(MethodBuilder.INVOCATION_HANDLER_FIELD_NAME),
                    eq(MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE.getDescriptor()), isNull(), isNull());
            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("<clinit>"), eq("()V"), isNull(), isNull());
            verify(cw).visitEnd();
            verifyNoMoreInteractions(cw);
        }
    }

    @Test
    public void visitEnd_canModifyClassStructureAndHasInstrumentedMethodsProcessedInit() throws NoSuchFieldException, IllegalAccessException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(true);
            GenericClassAdapter adapter = getClassAdapter("java.lang.Object", Object.class, true);
            adapter.getInstrumentedMethods().add(mock(AbstractTracingMethodAdapter.class));
            setProcessedClassInitMethod(adapter);
            when(cw.visitMethod(eq(Opcodes.ACC_STATIC), eq("<clinit>"), eq("()V"), isNull(), isNull()))
                    .thenReturn(mock(MethodVisitor.class));

            adapter.visitEnd();

            verify(cw).visitMethod(eq(Opcodes.ACC_STATIC), eq("__nr__initClass"), eq("()V"), isNull(), isNull());
            verify(cw).visitField(eq(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE), eq(MethodBuilder.INVOCATION_HANDLER_FIELD_NAME),
                    eq(MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE.getDescriptor()), isNull(), isNull());
            verify(cw).visitEnd();
            verifyNoMoreInteractions(cw);
        }
    }

    @Test
    public void visitEnd_mustAddField() throws NoSuchFieldException, IllegalAccessException {
        try (MockedStatic<PointCutClassTransformer> pointCutClassTransformer = mockStatic(PointCutClassTransformer.class)) {
            pointCutClassTransformer.when(() -> PointCutClassTransformer.canModifyClassStructure(any(ClassLoader.class), any(Class.class)))
                    .thenReturn(false);
            GenericClassAdapter adapter = getClassAdapter(
                    "com.newrelic.agent.instrumentation.GenericClassAdapterTest$InvocationHandlerField",
                    InvocationHandlerField.class, true);
            adapter.getInstrumentedMethods().add(mock(AbstractTracingMethodAdapter.class));

            adapter.visitEnd();

            verify(cw).visitField(eq(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE), eq(MethodBuilder.INVOCATION_HANDLER_FIELD_NAME),
                    eq(MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE.getDescriptor()), isNull(), isNull());
            verify(cw).visitEnd();
            verifyNoMoreInteractions(cw);
        }
    }

    @Test
    public void addInstrumentedMethod() {
        GenericClassAdapter adapter = getClassAdapter("java.lang.Integer", Integer.class, true);
        AbstractTracingMethodAdapter method1 = mock(AbstractTracingMethodAdapter.class);
        AbstractTracingMethodAdapter method2 = mock(AbstractTracingMethodAdapter.class);

        int result1 = adapter.addInstrumentedMethod(method1);

        assertEquals(0, result1);

        int result2 = adapter.addInstrumentedMethod(method2);

        assertEquals(1, result2);

        assertThat(adapter.getInstrumentedMethods(), containsInAnyOrder(method1, method2));
    }

    private GenericClassAdapter getClassAdapter(String className, Class<?> classBeingRedefined, boolean pointCutMatches) {
        cw = mock(ClassWriter.class);
        context = mock(InstrumentationContext.class);

        mv = mock(MethodVisitor.class);
        when(cw.visitMethod(anyInt(), anyString(), anyString(), any(), any(String[].class)))
                .thenReturn(mv);

        pointCut = mock(PointCut.class, Answers.RETURNS_DEEP_STUBS);
        when(pointCut.getMethodMatcher().matches(anyInt(), anyString(), anyString(), any(Set.class)))
                .thenReturn(pointCutMatches);

        return new GenericClassAdapter(cw, this.getClass().getClassLoader(), className, classBeingRedefined,
                Collections.singleton(pointCut), context);
    }

    private void setProcessedClassInitMethod(GenericClassAdapter adapter) throws IllegalAccessException, NoSuchFieldException {
        Field field = GenericClassAdapter.class.getDeclaredField("processedClassInitMethod");
        field.setAccessible(true);
        field.set(adapter, true);
    }

    private static class NrClassInit {
        public void __nr__initClass() {
            // this method indicates that we must
        }
    }

    private static class InvocationHandlerField {
        private Object __nr__InvocationHandlers;
    }
}