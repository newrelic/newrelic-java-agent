/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation;

import com.newrelic.agent.Agent;
import com.newrelic.agent.TracerService;
import com.newrelic.agent.instrumentation.context.InstrumentationContext;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.tracers.PointCutInvocationHandler;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * We have two strategies for instrumenting methods. Each involves getting an {@link InvocationHandler} and calling
 * {@link InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])} to get a handler which is also an
 * {@link InvocationHandler}. When the method finishes this handler will be invoked with the return value of the method
 * or the exception if one was thrown.
 * 
 * {@link InvocationHandlerTracingMethodAdapter}: If this is the first time a class is being loaded, a field of type
 * {@link InvocationHandler} is created for every instrumented method. A static method is added which initializes these
 * fields and this is called from the class initializer. These invocation handlers are factories which create the
 * tracing invocation handler for the method, and they're created through a callback to
 * {@link PointCutClassTransformer#evaluate(Class, com.newrelic.agent.TracerService, Object, Object, Object, boolean, Object[])}
 * . This is the faster of the two instrumentation approaches.
 * 
 * {@link ReflectionStyleClassMethodAdapter}: If the class has already been loaded no fields can be added to it. In this
 * case we fetch the tracer by making a call to {@link AgentWrapper#invoke(Object, java.lang.reflect.Method, Object[])}
 * passing in the invocation handler id ( see {@link TracerService#getInvocationHandler(int)}.
 */
public class GenericClassAdapter extends ClassVisitor {
    private static final String CLINIT_METHOD_NAME = "<clinit>";
    private static final String NO_ARG_VOID_DESC = "()V";
    private static final String INIT_CLASS_METHOD_NAME = "__nr__initClass";

    protected final String className;
    private final ClassLoader classLoader;
    final Class<?> classBeingRedefined;
    private final List<AbstractTracingMethodAdapter> instrumentedMethods = new ArrayList<>();
    private final List<PointCut> pointcutsApplied = new ArrayList<>();
    /**
     * The class version. I think ASM is reporting incorrect values. Some of the Apache classes are reporting versions
     * in the thousands, but javap -verbose says the version is 45.
     * 
     * @see InitMethod#isNecessaryToLoadClassWithForName()
     */
    private int version;
    private boolean processedClassInitMethod;
    private final Collection<PointCut> matches;
    private final InstrumentationContext context;

    public GenericClassAdapter(ClassVisitor cv, ClassLoader classLoader, String className,
            Class<?> classBeingRedefined, Collection<PointCut> strongMatches, InstrumentationContext context) {
        super(WeaveUtils.ASM_API_LEVEL, cv);

        this.context = context;
        this.matches = strongMatches;
        this.classLoader = classLoader;
        this.className = className;
        this.classBeingRedefined = classBeingRedefined;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {

        /*
         * if (version > MAX_VERSION) { throw new StopProcessingException(MessageFormat.format(
         * "{0} class version of {1} is unsupported", name, version)); }
         */
        boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
        if (isInterface) {
            throw new StopProcessingException(name + " is an interface");
        }
        super.visit(version, access, name, signature, superName, interfaces);
        this.version = version;
    }

    /**
     * We can only add fields to classes which are being defined the first time or have already been defined with NR
     * class transformers.
     * 
     * @see ReflectionStyleClassMethodAdapter
     */
    boolean canModifyClassStructure() {
        return PointCutClassTransformer.canModifyClassStructure(classLoader, classBeingRedefined);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if (canModifyClassStructure()) {
            if (CLINIT_METHOD_NAME.equals(name)) {
                mv = new InitMethodAdapter(mv, access, name, desc);
                processedClassInitMethod = true;
                return mv;
            }
        }

        if ((access & Opcodes.ACC_ABSTRACT) != 0) {
            return mv; // don't instrument abstract methods
        }

        PointCut pointCut = getMatch(access, name, desc);
        if (pointCut == null) {
            return mv;
        }
        Method method = new Method(name, desc);

        context.addTimedMethods(method);

        if (canModifyClassStructure()) {
            context.addOldInvokerStyleInstrumentationMethod(method, pointCut);
            mv = new InvocationHandlerTracingMethodAdapter(this, mv, access, method);
            pointcutsApplied.add(pointCut);
        } else {
            PointCutInvocationHandler pointCutInvocationHandler = pointCut.getPointCutInvocationHandler();
            int id = ServiceFactory.getTracerService().getInvocationHandlerId(pointCutInvocationHandler);
            if (id == -1) {
                Agent.LOG.log(Level.FINE,
                        "Unable to find invocation handler for method: {0} in class: {1}. Skipping instrumentation.",
                        name, className);
            } else {
                context.addOldReflectionStyleInstrumentationMethod(method, pointCut);
                mv = new ReflectionStyleClassMethodAdapter(this, mv, access, method, id);
                pointcutsApplied.add(pointCut);
            }
        }

        return mv;
    }

    private PointCut getMatch(int access, String name, String desc) {
        for (PointCut pc : matches) {
            if (pc.getMethodMatcher().matches(MethodMatcher.UNSPECIFIED_ACCESS, name, desc,
                    MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
                return pc;
            }
        }
        return null;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();

        if ((canModifyClassStructure() && (processedClassInitMethod || instrumentedMethods.size() > 0))
                || mustAddNRClassInit()) {
            createNRClassInitMethod();
        }

        if (instrumentedMethods.size() > 0) {
            // if the classloader is null it's the bootstrap classloader which doesn't have visibility to our annotation
            // if (null != classLoader) {
            // cv.visitAnnotation(Type.getDescriptor(InstrumentedClass.class), true);
            // }
            if (canModifyClassStructure() || mustAddField()) {
                createInvocationHandlerField();
            }
            if (canModifyClassStructure() || mustAddNRClassInit()) {
                if (!processedClassInitMethod) {
                    createClassInitMethod();
                }
            }
        }
    }

    private boolean mustAddNRClassInit() {

        if (classBeingRedefined == null) {
            return false;
        }
        try {
            classBeingRedefined.getDeclaredMethod(INIT_CLASS_METHOD_NAME);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private boolean mustAddField() {
        if (classBeingRedefined == null) {
            return false;
        }
        try {
            classBeingRedefined.getDeclaredField(MethodBuilder.INVOCATION_HANDLER_FIELD_NAME);
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    /**
     * Creates a static constructor that calls the NR init method.
     */
    private void createClassInitMethod() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, CLINIT_METHOD_NAME, NO_ARG_VOID_DESC, null, null);
        mv = new InitMethodAdapter(mv, Opcodes.ACC_STATIC, CLINIT_METHOD_NAME, NO_ARG_VOID_DESC);

        // create an empty class initializer
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Creates the NR init method which initializes the InvocationHandler fields for instrumented methods and calls back
     * into the ClassTransformer {@link PointCutClassTransformer#evaluate(Class, List)}
     */
    private void createNRClassInitMethod() {
        MethodVisitor mv = cv.visitMethod(Opcodes.ACC_STATIC, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC, null, null);
        mv = new InitMethod(mv, Opcodes.ACC_STATIC, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC);
        mv.visitCode();

        mv.visitInsn(Opcodes.RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void createInvocationHandlerField() {
        cv.visitField(Opcodes.ACC_STATIC + Opcodes.ACC_PRIVATE, MethodBuilder.INVOCATION_HANDLER_FIELD_NAME,
                MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE.getDescriptor(), null, null);
    }

    int addInstrumentedMethod(AbstractTracingMethodAdapter methodAdapter) {
        int index = instrumentedMethods.size();
        instrumentedMethods.add(methodAdapter);
        return index;
    }

    public Collection<AbstractTracingMethodAdapter> getInstrumentedMethods() {
        return instrumentedMethods;
    }

    /**
     * Adapter to visit a clinit method and add a call to the NR class init method.
     */
    private class InitMethodAdapter extends AdviceAdapter {

        protected InitMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
            super(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc);
        }

        @Override
        protected void onMethodEnter() {
            mv.visitMethodInsn(INVOKESTATIC, className, INIT_CLASS_METHOD_NAME, NO_ARG_VOID_DESC, false);
        }
    }

    /**
     * Implements the {@link GenericClassAdapter#INIT_CLASS_METHOD_NAME} method.
     */
    private class InitMethod extends AdviceAdapter {

        private InitMethod(MethodVisitor mv, int access, String name, String desc) {
            super(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc);
        }

        /**
         * Returns the id of a local variable initialized to the {@link AgentWrapper} instance.
         */
        private int getAgentWrapper() {
            // get the handle to the AgentWrapper and store it in a local variable
            new MethodBuilder(this, this.methodAccess).loadInvocationHandlerFromProxy();

            final int invocationHandlerVar = newLocal(MethodBuilder.INVOCATION_HANDLER_TYPE);
            mv.visitVarInsn(ASTORE, invocationHandlerVar);

            return invocationHandlerVar;
        }

        /**
         * Initializes the InvocationHandler fields.
         */
        @Override
        protected void onMethodEnter() {
            if (classBeingRedefined != null) {
                return;
            }
            final int invocationHandlerVar = getAgentWrapper();

            // load the instrumented class
            visitLdcInsn(Type.getObjectType(className));

            final int classVar = newLocal(Type.getType(Object.class));
            mv.visitVarInsn(ASTORE, classVar);

            if (canModifyClassStructure()) {

                // create the array and store it in the invocationHandlers field
                push(instrumentedMethods.size());
                newArray(MethodBuilder.INVOCATION_HANDLER_TYPE);
                putStatic(Type.getObjectType(className), MethodBuilder.INVOCATION_HANDLER_FIELD_NAME,
                        MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);

                // initialize the invocation handler array
                for (AbstractTracingMethodAdapter methodAdapter : instrumentedMethods) {
                    if (methodAdapter.getInvocationHandlerIndex() >= 0) {
                        initMethod(classVar, invocationHandlerVar, methodAdapter);
                    }
                }
            }
        }

        /**
         * Invoke {@link AgentWrapper#invoke(Object, java.lang.reflect.Method, Object[])} with the proxy set to the
         * current class object, passing in the initialization information for the given field. The method invocation
         * will return an {@link InvocationHandler} that will be stored in the field named
         * {@link AbstractTracingMethodAdapter#getInvocationHandlerFieldName()}. If the method should not be
         * instrumented the return value will be null.
         * 
         * @param invocationHandlerVar
         * @param methodAdapter
         */
        private void initMethod(int classVar, int invocationHandlerVar, AbstractTracingMethodAdapter methodAdapter) {

            // load the array of invocation handlers field onto the stack
            getStatic(Type.getObjectType(className), MethodBuilder.INVOCATION_HANDLER_FIELD_NAME,
                    MethodBuilder.INVOCATION_HANDLER_ARRAY_TYPE);

            // load the array index of the invocation handler
            push(methodAdapter.getInvocationHandlerIndex());

            mv.visitVarInsn(ALOAD, invocationHandlerVar);

            // invoke proxy
            mv.visitVarInsn(ALOAD, classVar);

            // invoke method
            visitInsn(ACONST_NULL);

            // invoke arguments array
            List<Object> arguments = new ArrayList<>(Arrays.<Object>asList(className, methodAdapter.methodName,
                    methodAdapter.getMethodDescriptor(), false, false));

            new MethodBuilder(this, this.methodAccess).loadArray(Object.class,
                    arguments.toArray(new Object[arguments.size()])).invokeInvocationHandlerInterface(false);

            // store the invocation handler in the correct field
            checkCast(MethodBuilder.INVOCATION_HANDLER_TYPE);

            // store in the array
            arrayStore(MethodBuilder.INVOCATION_HANDLER_TYPE);

        }
    }

    public List<PointCut> getAppliedPointCuts() {
        return pointcutsApplied;
    }
}
