/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.preprocessors;

import com.google.common.collect.Sets;
import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.external.ExternalParametersFactory;
import com.newrelic.agent.bridge.reflect.ClassReflection;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.instrumentation.InstrumentationType;
import com.newrelic.agent.instrumentation.tracing.Annotation;
import com.newrelic.agent.instrumentation.tracing.BridgeUtils;
import com.newrelic.agent.instrumentation.tracing.TraceDetailsBuilder;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.QueryConverter;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.CatchAndLog;
import com.newrelic.api.agent.weaver.SkipIfPresent;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.WeaveWithAnnotation;
import com.newrelic.weave.UtilityClass;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.WeavePackage;
import com.newrelic.weave.weavepackage.WeavePreprocessor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.commons.Remapper;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;

public class AgentPreprocessors implements WeavePreprocessor {

    private static final Type EXTERNAL_PARAMETERS_FACTORY_TYPE = Type.getType(ExternalParametersFactory.class);
    private static final Type DATASTORE_PARAMETERS_SLOW_QUERY_TYPE = Type.getType(DatastoreParameters.SlowQueryParameter.class);
    private static final Type DATASTORE_PARAMETERS_SLOW_QUERY_INPUT_TYPE = Type.getType(DatastoreParameters.SlowQueryWithInputParameter.class);
    private static final Method createForDatastoreMethod = new Method("createForDatastore",
            Type.getType(com.newrelic.agent.bridge.external.ExternalParameters.class),
            new Type[]{
                    Type.getType(String.class), // product
                    Type.getType(String.class), // collection
                    Type.getType(String.class), // operation
                    Type.getType(String.class), // host
                    Type.getType(Integer.class), // port
            });
    private static final Method createForDatastoreSlowQueryMethod = new Method("createForDatastore",
            Type.getType(com.newrelic.agent.bridge.external.ExternalParameters.class),
            new Type[]{
                    Type.getType(String.class), // product
                    Type.getType(String.class), // collection
                    Type.getType(String.class), // operation
                    Type.getType(String.class), // host
                    Type.getType(Integer.class), // port
                    Type.getType(Object.class), // rawQuery
                    Type.getType(com.newrelic.agent.bridge.datastore.QueryConverter.class) // queryConverter
            });
    private static final Method createForDatastoreSlowQueryInputMethod = new Method("createForDatastore",
            Type.getType(com.newrelic.agent.bridge.external.ExternalParameters.class),
            new Type[]{
                    Type.getType(String.class), // product
                    Type.getType(String.class), // collection
                    Type.getType(String.class), // operation
                    Type.getType(String.class), // host
                    Type.getType(Integer.class), // port
                    Type.getType(Object.class), // rawQuery
                    Type.getType(com.newrelic.agent.bridge.datastore.QueryConverter.class), // queryConverter
                    Type.getType(String.class), // inputQueryLabel
                    Type.getType(Object.class), // inputQuery
                    Type.getType(com.newrelic.agent.bridge.datastore.QueryConverter.class), // inputQueryConverter
            });
    private static final Method createForDatastoreSlowQueryBuilderMethod = new Method("slowQuery",
            Type.getType(DatastoreParameters.SlowQueryWithInputParameter.class),
            new Type[]{
                    Type.getType(Object.class), // rawQuery
                    Type.getType(QueryConverter.class), // queryConverter
            });
    private static final Method noSlowQueryBuilderMethod = new Method("noSlowQuery",
            Type.getType(DatastoreParameters.SlowQueryWithInputParameter.class), new Type[]{ });
    private static final Method createForDatastoreSlowQueryInputBuilderMethod = new Method("slowQueryWithInput",
            Type.getType(DatastoreParameters.Build.class),
            new Type[]{
                    Type.getType(String.class), // inputQueryLabel
                    Type.getType(Object.class), // rawInputQuery
                    Type.getType(QueryConverter.class) // rawInputQueryConverter
            });

    private static final String CATCH_AND_LOG_DESC = Type.getDescriptor(CatchAndLog.class);
    private String weavePackageName = "Unknown";
    private ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails;
    private final boolean captureSqlQueries;
    private final Set<String> collectSlowQueriesFromModules;

    public AgentPreprocessors(AgentConfig agentConfig,
            ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> tracedWeaveInstrumentationDetails) {
        this.tracedWeaveInstrumentationDetails = tracedWeaveInstrumentationDetails;

        this.captureSqlQueries = !agentConfig.isHighSecurity() && !agentConfig.laspEnabled();
        this.collectSlowQueriesFromModules = agentConfig.getTransactionTracerConfig().getCollectSlowQueriesFromModules();
    }

    public static AgentPreprocessors createWithInstrumentationTitle(AgentConfig agentConfig, String instrumentationTitle) {
        AgentPreprocessors result = new AgentPreprocessors(agentConfig,
                new ConcurrentHashMap<String, Set<TracedWeaveInstrumentationTracker>>());
        result.setInstrumentationTitle(instrumentationTitle);
        return result;
    }

    public void setInstrumentationTitle(String instrumentationTitle) {
        this.weavePackageName = instrumentationTitle;
    }

    public ConcurrentMap<String, Set<TracedWeaveInstrumentationTracker>> getTracedWeaveInstrumentationDetails() {
        return tracedWeaveInstrumentationDetails;
    }

    @Override
    public ClassVisitor preprocess(ClassVisitor cv, Set<String> utilityClassesInternalNames, WeavePackage weavePackage) {
        if (System.getSecurityManager() != null) {
            // We only need to run this if a SecurityManager exists which denotes Java 2 security is in place
            cv = handleElevatePermissions(cv);
        }
        cv = handleCatchAndLog(cv);
        cv = gatherTraceInfo(cv);
        cv = markUtilityClasses(cv);
        cv = rewriteSlowQueryIfRequired(cv);
        cv = nullOutTokenAfterExpire(cv);
        cv = instrumentationPackageRemapper(cv, utilityClassesInternalNames);
        return cv;
    }

    private ClassVisitor handleCatchAndLog(ClassVisitor cv) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                className = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, final String name, final String desc, String signature,
                    String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new AdviceAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, name, desc) {

                    boolean isCatchAndLog;
                    Label start = newLabel(), end = newLabel(), handler = newLabel();

                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        AnnotationVisitor av = super.visitAnnotation(desc, visible);
                        isCatchAndLog = isCatchAndLog || desc.equals(CATCH_AND_LOG_DESC);
                        return av;
                    }

                    @Override
                    protected void onMethodEnter() {
                        super.onMethodEnter();
                        if (!isCatchAndLog) {
                            return;
                        }
                        visitLabel(start);
                    }

                    @Override
                    public void visitMaxs(int maxStack, int maxLocals) {
                        if (!isCatchAndLog) {
                            super.visitMaxs(maxStack, maxLocals);
                            return;
                        }
                        super.visitLabel(handler);

                        final int throwableLocal = newLocal(Type.getType(Throwable.class));
                        storeLocal(throwableLocal);

                        Runnable throwableMessage = () -> loadLocal(throwableLocal);

                        BridgeUtils.getLogger(this).logToChild(weavePackageName, Level.FINE,
                                "{0}.{1}{2} threw an exception: {3}", className, name, desc, throwableMessage);

                        BridgeUtils.loadLogger(this);

                        getStatic(Type.getType(Level.class), Level.FINEST.getName(), Type.getType(Level.class));

                        loadLocal(throwableLocal);
                        push("Exception stack:");

                        BridgeUtils.getLoggerBuilder(this, false).build().log(Level.FINEST, (Exception) null, null);

                        visitInsn(Opcodes.RETURN);

                        super.visitLabel(end);
                        super.visitTryCatchBlock(start, end, handler, Type.getInternalName(Throwable.class));

                        super.visitMaxs(maxStack, maxLocals);
                    }

                };
            }
        };
    }

    /**
     * This rewrites various instructions to work around issues that come up when java security is enabled. Our weaved
     * instrumentation does some reflection and those api calls have to be made with the agent's permissions.
     *
     * @see ClassReflection
     */
    static ClassVisitor handleElevatePermissions(ClassVisitor cv) {

        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            private final ReflectionHelper reflection = ReflectionHelper.get();
            private String className;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                className = name;
                // for compatibility with older class files.
                if (version < 50) {
                    version = 50;
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc,
                    String signature, String[] exceptions) {
                final MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);

                return new GeneratorAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, methodName, methodDesc) {

                    /**
                     * Rewrite instructions that load classes (weaved code references to SomeClass.class) so that we
                     * instead load the current class, get its classloader and invoke
                     * ClassReflection#loadClass(ClassLoader, String).
                     *
                     * @see ClassReflection#loadClass(ClassLoader, String)
                     */
                    @Override
                    public void visitLdcInsn(Object cst) {

                        if (cst instanceof Type && !className.equals(((Type) cst).getInternalName())) {
                            loadClass((Type) cst);
                        } else {
                            super.visitLdcInsn(cst);
                        }
                    }

                    /**
                     * Load a class by inserting a ldc class instruction for this class (ThisClass.class), invoking
                     * getClassLoader through ClassReflection#getClassLoader, then invoking the ClassReflection
                     * loadClass method.
                     *
                     * @param typeToLoad
                     */
                    private void loadClass(Type typeToLoad) {
                        super.visitLdcInsn(Type.getObjectType(className));

                        super.invokeStatic(Type.getType(ClassReflection.class), new Method("getClassLoader",
                                "(Ljava/lang/Class;)Ljava/lang/ClassLoader;"));

                        push(typeToLoad.getClassName());

                        super.invokeStatic(Type.getType(ClassReflection.class), new Method("loadClass",
                                "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Class;"));
                    }

                    /**
                     * Rewrite INSTANCEOF instructions to load the type using the above loadClass method, then invoke
                     * Class.isInstance.
                     *
                     * Rewrite CHECKCAST instructions to load the type using the above loadClass method, then invoke
                     * Class.cast.
                     */
                    @Override
                    public void visitTypeInsn(int opcode, String type) {

                        Type objectType = Type.getObjectType(type);
                        if (Opcodes.INSTANCEOF == opcode) {
                            // right now the object instance to be checked is on the stack

                            loadClass(objectType);

                            // stack is object - class. Swap so it's class - object
                            super.swap();

                            super.invokeVirtual(Type.getType(Class.class), new Method("isInstance",
                                    "(Ljava/lang/Object;)Z"));

                        } else {
                            super.visitTypeInsn(opcode, type);
                        }
                    }

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

                        if (reflection.process(owner, name, desc, this)) {
                            return;
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    }

                };
            }

        };
    }

    /**
     * Visit the methods of weave classes which have {@link Trace} annotations. <br/>
     * Methods which have tracers will be stored in the tracedWeaveInstrumentationDetails map.
     */
    ClassVisitor gatherTraceInfo(ClassVisitor cv) {
        // weave package name -> WeaverTraceDetails
        Set<TracedWeaveInstrumentationTracker> initial = Sets.newConcurrentHashSet();
        tracedWeaveInstrumentationDetails.putIfAbsent(weavePackageName, initial);

        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            private boolean isWeave = false;
            private boolean isWeaveWithAnnotation = false;
            private String originalName;

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                originalName = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }

            /**
             * Get the name of the original class specified in @Weave(originalName="...")
             */
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(desc, visible);
                if (Type.getDescriptor(Weave.class).equals(desc)) {
                    isWeave = true;
                    return new AnnotationVisitor(WeaveUtils.ASM_API_LEVEL, av) {
                        @Override
                        public void visit(String name, Object value) {
                            if (name.equals("originalName")) {
                                originalName = ((String) value).replace('.', '/');
                            }
                            super.visit(name, value);
                        }
                    };
                }
                if (Type.getDescriptor(WeaveWithAnnotation.class).equals(desc)) {
                    isWeaveWithAnnotation = true;
                }
                return av;
            }

            @Override
            public MethodVisitor visitMethod(final int access, final String methodName, final String methodDesc,
                    String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);
                return new MethodVisitor(WeaveUtils.ASM_API_LEVEL, mv) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        AnnotationVisitor av = super.visitAnnotation(desc, visible);
                        if (Type.getDescriptor(Trace.class).equals(desc)) {
                            Agent.LOG.log(Level.FINER, "Storing TracedWeaveInstrumentation: {0} - {1}.{2}({3})",
                                    weavePackageName, originalName, methodName, methodDesc);
                            TraceDetailsBuilder builder = TraceDetailsBuilder.newBuilder().setInstrumentationType(
                                    InstrumentationType.TracedWeaveInstrumentation).setInstrumentationSourceName(
                                    weavePackageName);
                            av = new Annotation(av, desc, builder) {
                                @Override
                                public void visitEnd() {
                                    tracedWeaveInstrumentationDetails.get(weavePackageName).add(
                                            new TracedWeaveInstrumentationTracker(weavePackageName, originalName,
                                                    new Method(methodName, methodDesc), isWeaveWithAnnotation, getTraceDetails(false)));
                                    super.visitEnd();
                                }
                            };
                        }
                        return av;
                    }
                };
            }
        };
    }

    /**
     * Visit classes and add a "@UtilityClass" annotation on anything that doesn't have "@Weave" or "@SkipIfPresent"
     */
    ClassVisitor markUtilityClasses(ClassVisitor cv) {
        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {
            private boolean isUtilityClass = true;

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (Type.getDescriptor(Weave.class).equals(desc) ||
                    Type.getDescriptor(SkipIfPresent.class).equals(desc) ||
                    Type.getDescriptor(WeaveWithAnnotation.class).equals(desc)) {
                    this.isUtilityClass = false;
                }
                return super.visitAnnotation(desc, visible);
            }

            @Override
            public void visitEnd() {
                if (isUtilityClass) {
                    addUtilityClassAnnotation(cv);
                }
                super.visitEnd();
            }

            private ClassVisitor addUtilityClassAnnotation(ClassVisitor cv) {
                AnnotationVisitor av = cv.visitAnnotation(Type.getDescriptor(UtilityClass.class), true);
                av.visit("weavePackageName", weavePackageName);
                av.visitEnd();
                return cv;
            }
        };
    }

    ClassVisitor rewriteSlowQueryIfRequired(ClassVisitor cv) {
        // If capturing sql queries we have nothing to rewrite here, all calls are allowed through
        if (captureSqlQueries) {
            return cv;
        }

        // high_security or lasp is enabled and this weave package is allowed through, nothing to rewrite
        if (collectSlowQueriesFromModules.contains(weavePackageName)) {
            return cv;
        }

        return new ClassVisitor(WeaveUtils.ASM_API_LEVEL, cv) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName,
                    String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
            }

            @Override
            public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature,
                    String[] exceptions) {
                final MethodVisitor mv = super.visitMethod(access, methodName, methodDesc, signature, exceptions);

                return new GeneratorAdapter(WeaveUtils.ASM_API_LEVEL, mv, access, methodName, methodDesc) {

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        // Rewrite both slow query calls to use the regular datastore method and drop the other params
                        if (isDatastoreSlowQueryBuilderMethod(owner, name, desc)) {
                            // First, pop off the 2 parameter values to the "slowQuery" builder method
                            pop2();

                            // Now rewrite the method call to use "noSlowQuery()" instead of "slowQuery(params...)"
                            super.visitMethodInsn(opcode, owner, noSlowQueryBuilderMethod.getName(),
                                    noSlowQueryBuilderMethod.getDescriptor(), itf);
                        } else if (isDatastoreSlowQueryInputBuilderMethod(owner, name, desc)) {
                            // First, pop off the 3 parameter values to the "slowQueryWithInput" builder method
                            pop2();
                            pop();

                            // Don't call visitMethodInsn here and we can effectively scrub the slowQueryWithInput call
                        } else if (isDatastoreSlowQueryMethod(owner, name, desc)) {
                            // If we got here it means that matched the slow query method and the module is NOT
                            // in the list to collect from so we are going to rewrite this call to the standard datastore call.

                            // First, pop off the 2 extra parameter values
                            pop2();

                            // Now rewrite the method call
                            super.visitMethodInsn(opcode, owner, name, createForDatastoreMethod.getDescriptor(), itf);
                        } else if (isDatastoreSlowQueryInputMethod(owner, name, desc)) {
                            // If we got here it means that matched the slow query input method and the module is NOT
                            // in the list to collect from so we are going to rewrite this call to the standard datastore call.

                            // First, pop off the 5 extra parameter values
                            pop2();
                            pop2();
                            pop();

                            // Now rewrite the method call
                            super.visitMethodInsn(opcode, owner, name, createForDatastoreMethod.getDescriptor(), itf);
                        } else {
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                        }
                    }
                };
            }

        };
    }

    private static boolean isDatastoreSlowQueryMethod(String owner, String name, String desc) {
        return owner.equals(EXTERNAL_PARAMETERS_FACTORY_TYPE.getInternalName())
                && name.equals(createForDatastoreSlowQueryMethod.getName())
                && desc.equals(createForDatastoreSlowQueryMethod.getDescriptor());
    }

    private static boolean isDatastoreSlowQueryInputMethod(String owner, String name, String desc) {
        return owner.equals(EXTERNAL_PARAMETERS_FACTORY_TYPE.getInternalName())
                && name.equals(createForDatastoreSlowQueryInputMethod.getName())
                && desc.equals(createForDatastoreSlowQueryInputMethod.getDescriptor());
    }

    private static boolean isDatastoreSlowQueryBuilderMethod(String owner, String name, String desc) {
        return owner.equals(DATASTORE_PARAMETERS_SLOW_QUERY_TYPE.getInternalName())
                && name.equals(createForDatastoreSlowQueryBuilderMethod.getName())
                && desc.equals(createForDatastoreSlowQueryBuilderMethod.getDescriptor());
    }

    private static boolean isDatastoreSlowQueryInputBuilderMethod(String owner, String name, String desc) {
        return owner.equals(DATASTORE_PARAMETERS_SLOW_QUERY_INPUT_TYPE.getInternalName())
                && name.equals(createForDatastoreSlowQueryInputBuilderMethod.getName())
                && desc.equals(createForDatastoreSlowQueryInputBuilderMethod.getDescriptor());
    }

    TokenNullCheckClassVisitor nullOutTokenAfterExpire(ClassVisitor cv) {
        return new TokenNullCheckClassVisitor(WeaveUtils.ASM_API_LEVEL, cv);
    }

    /**
     * A visitor which detects simple cases where the user calls expire on a token but does not null the token out.
     * In these cases the visitor will insert bytecode which nulls out the token. Bytecode which already nulls out the NewField will be unaffected.
     * By nulling out the token, the newfield will be reset and be eligible for map removal.
     *
     * Example:<pre>
     *   token.link();
     *   token.expire(); // visitor detects expire
     *   token = null;   // visitor writes this (unless the user already did)</pre>
     */
    public class TokenNullCheckClassVisitor extends ClassVisitor {
        public TokenNullCheckClassVisitor(int api, ClassVisitor cv) {
            super(api, cv);
        }
        private String className;
        private List<TokenNullCheckMethodVisitor> methodVisitors = new ArrayList<>();

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                String[] interfaces) {
            className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, final String name, final String desc, String signature,
                String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            TokenNullCheckMethodVisitor tmv = new TokenNullCheckMethodVisitor(WeaveUtils.ASM_API_LEVEL, mv, weavePackageName, className, name);
            methodVisitors.add(tmv);
            return tmv;
        }

        /**
         * For testing
         */
        public Map<String, Integer> getExpireRewriteCounts() {
            Map<String, Integer> counts = new HashMap<>(methodVisitors.size());
            for (TokenNullCheckMethodVisitor mv : methodVisitors) {
                counts.put(mv.methodName, mv.numNullOpsAdded);
            }
            return counts;
        }
    }
    private static class TokenNullCheckMethodVisitor extends MethodVisitor {
        public TokenNullCheckMethodVisitor(int api, MethodVisitor mv, String packageName, String className, String methodName) {
            super(api, mv);
            this.packageName = packageName;
            this.className = className;
            this.methodName = methodName;
        }

        // State tracking to ensure newfield tokens are nulled out after expire.
        private enum State{
            INIT,
            PUSH_FIELD_OWNER_FOR_GET,
            GETFIELD_TOKEN,
            EXPIRE_TOKEN,
            POP_TOKEN_EXP_RES,
            PUSH_FIELD_OWNER_FOR_PUT,
            PUSH_NULL_FOR_PUT
        }

        private static final Type API_TOKEN_TYPE = Type.getType(com.newrelic.api.agent.Token.class);
        private static final Type BRIDGE_TOKEN_TYPE = Type.getType(com.newrelic.agent.bridge.Token.class);
        private static final Set<String> TOKEN_EXPIRE_METHODS = Sets.newHashSet("expire", "linkAndExpire");

        private final String packageName;
        private final String className;
        private final String methodName;

        private State state = State.INIT;
        private String lastGetOwner;
        private String lastGetName;
        private int lastGetVarSlot;
        private int lastLineNumber = 0;
        private int expireLineNumber = 0;

        public int numNullOpsAdded = 0;

        private void checkState(){
            if (state == State.EXPIRE_TOKEN
                    || state == State.POP_TOKEN_EXP_RES
                    || state == State.PUSH_FIELD_OWNER_FOR_PUT
                    || state == State.PUSH_NULL_FOR_PUT) {
                String msg = MessageFormat.format(
                        "WARNING: Instrumentation {0}-{1}.{2}{3}: token newfield expired but not set to null."
                        + " To prevent high GC churn the agent has inserted bytecode to null out this field. "
                        + " This may cause NPEs in the instrumentation code. To avoid this, explicitly null the NewField after calling expire()."
                                , packageName, className, methodName, 0 == expireLineNumber ? "" : ":"+expireLineNumber);
                AgentBridge.getAgent().getLogger().log(Level.FINE, msg);
                state = State.INIT;
                this.visitVarInsn(Opcodes.ALOAD, lastGetVarSlot);
                this.visitInsn(Opcodes.ACONST_NULL);
                this.visitFieldInsn(Opcodes.PUTFIELD, this.lastGetOwner, this.lastGetName, API_TOKEN_TYPE.getDescriptor());
                numNullOpsAdded++;
            }
            state = State.INIT;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack+numNullOpsAdded*2, maxLocals);
        }

        @Override
        public void visitInsn(int opcode) {
            if (Opcodes.POP == opcode
                    && state == State.EXPIRE_TOKEN) {
                state = State.POP_TOKEN_EXP_RES;
            } else if(Opcodes.ACONST_NULL == opcode
                    && state == State.PUSH_FIELD_OWNER_FOR_PUT) {
                state = State.PUSH_NULL_FOR_PUT;
            } else {
                checkState();
            }
            super.visitInsn(opcode);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name,
                String desc) {
            if(API_TOKEN_TYPE.getDescriptor().equals(desc) || BRIDGE_TOKEN_TYPE.getDescriptor().equals(desc)){
                if (Opcodes.GETFIELD == opcode
                        && state == State.PUSH_FIELD_OWNER_FOR_GET) {
                    state = State.GETFIELD_TOKEN;
                    this.lastGetOwner = owner;
                    this.lastGetName = name;
                } else if (Opcodes.PUTFIELD == opcode
                           && state == State.PUSH_NULL_FOR_PUT
                           && owner.equals(lastGetOwner)
                           && name.equals(lastGetName)) {
                    // user nulled out the token
                    state = State.INIT;
                } else {
                    checkState();
                }
            } else{
                checkState();
            }
            super.visitFieldInsn(opcode, owner, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (state == State.GETFIELD_TOKEN
                    && (API_TOKEN_TYPE.getInternalName().equals(owner) || BRIDGE_TOKEN_TYPE.getInternalName().equals(owner) )
                    && TOKEN_EXPIRE_METHODS.contains(name)) {
                expireLineNumber = lastLineNumber;
                state = State.EXPIRE_TOKEN;
            } else{
                checkState();
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (state == State.INIT && Opcodes.ALOAD == opcode) {
                state = State.PUSH_FIELD_OWNER_FOR_GET;
                this.lastGetVarSlot = var;
            } else if (state == State.POP_TOKEN_EXP_RES && Opcodes.ALOAD == opcode && var == lastGetVarSlot) {
                state = State.PUSH_FIELD_OWNER_FOR_PUT;
            } else {
                checkState();
            }
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            lastLineNumber = line;
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            checkState();
            super.visitJumpInsn(opcode, label);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            checkState();
            super.visitFrame(type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            checkState();
            super.visitIntInsn(opcode, operand);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            checkState();
            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            checkState();
            super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        }

        @Override
        public void visitLdcInsn(Object cst) {
            checkState();
            super.visitLdcInsn(cst);
        }

        @Override
        public void visitIincInsn(int var, int increment) {
            checkState();
            super.visitIincInsn(var, increment);
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            checkState();
            super.visitTableSwitchInsn(min, max, dflt, labels);
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            checkState();
            super.visitLookupSwitchInsn(dflt, keys, labels);
        }

        @Override
        public void visitMultiANewArrayInsn(String desc, int dims) {
            checkState();
            super.visitMultiANewArrayInsn(desc, dims);
        }
    }

    ClassVisitor instrumentationPackageRemapper(ClassVisitor cv, Set<String> utilityClasses) {
        return new InstrumentationPackageNameRewriter(WeaveUtils.ASM_API_LEVEL, cv, utilityClasses);
    }

    public static class InstrumentationPackageRemapper extends Remapper {
        // Set of utility classes whose package name collide with core agent classes (package name is
        // com.newrelic.agent.)
        final Set<String> conflictingUtilityClasses = new HashSet<>();

        public InstrumentationPackageRemapper(String weavePackageName, Set<String> utilityClasses) {

            for (String utilityClass : utilityClasses) {
                if (utilityClass.startsWith("com/newrelic/agent/")) {

                    // Sanity check to make sure we don't remap to an existing class.
                    String renameTo = utilityClass.replaceFirst("^com/newrelic/agent", "com/nr/instrumentation");
                    if (utilityClass.contains(renameTo)) {
                        AgentBridge.getAgent().getLogger().log(Level.INFO,
                                "Failed to remap reference for weave module {0}: {1} already exists.",
                                weavePackageName, renameTo);
                        this.conflictingUtilityClasses.clear();
                        break;
                    }

                    this.conflictingUtilityClasses.add(utilityClass);
                }
            }
        }

        @Override
        public String map(String typeName) {
            if (conflictingUtilityClasses.contains(typeName)) {
                String newTypeName = typeName.replaceFirst("^com/newrelic/agent", "com/nr/instrumentation");
                AgentBridge.getAgent().getLogger().log(
                        Level.FINE,
                        "Weave package class uses the agents package namespace. Consider changing this. Remapping reference from {0} to {1}",
                        typeName, newTypeName);
                return newTypeName;
            }
            return super.map(typeName);
        }
    }

    public class InstrumentationPackageNameRewriter extends ClassRemapper {

        public InstrumentationPackageNameRewriter(int api, ClassVisitor cv, Set<String> utilityClasses) {
            super(api, cv, new InstrumentationPackageRemapper(weavePackageName, utilityClasses));
        }
    }
}
