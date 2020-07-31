/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.SynchronizedMethodNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ErrorTrapHandler;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Wrap weave code in try-catch blocks and delegate errors to the configured {@link ErrorTrapHandler}.
 */
public class ErrorTrapWeaveMethodsProcessor {

    private final MethodNode trappedMethod;
    private final ClassNode errorTrapHandler;
    private final LabelNode startOfTrapLabelNode;
    private final LabelNode endOfTrapLabelNode;
    private final LabelNode startOfOriginalMethodLabelNode;
    private final LabelNode endOfOriginalMethodLabelNode;

    public static MethodNode writeErrorTrap(MethodNode weaveMethod, ClassNode errorTrapHandler,
            LabelNode startOfOriginalMethodLabelNode, LabelNode endOfOriginalMethodLabelNode) {
        LabelNode startOfTrap = WeaveUtils.makeLabelNode();
        LabelNode endOfTrap = WeaveUtils.makeLabelNode();
        weaveMethod.instructions.insert(startOfTrap);
        weaveMethod.instructions.add(endOfTrap);

        ErrorTrapWeaveMethodsProcessor trapper = new ErrorTrapWeaveMethodsProcessor(weaveMethod, errorTrapHandler,
                startOfTrap, endOfTrap, startOfOriginalMethodLabelNode, endOfOriginalMethodLabelNode);
        return trapper.process();
    }

    public ErrorTrapWeaveMethodsProcessor(MethodNode weaveMethod, ClassNode errorTrapHandler,
            LabelNode startOfTrapLabelNode, LabelNode endOfTrapLabelNode, LabelNode startOfOriginalMethodLabelNode,
            LabelNode endOfOriginalMethodLabelNode) {
        this.trappedMethod = weaveMethod;
        this.errorTrapHandler = errorTrapHandler;
        this.startOfTrapLabelNode = startOfTrapLabelNode;
        this.endOfTrapLabelNode = endOfTrapLabelNode;
        this.startOfOriginalMethodLabelNode = startOfOriginalMethodLabelNode;
        this.endOfOriginalMethodLabelNode = endOfOriginalMethodLabelNode;
    }

    public ErrorTrapWeaveMethodsProcessor(MethodNode weaveMethod, ClassNode errorTrapHandler,
            LabelNode startOfTrapLabelNode, LabelNode endOfTrapLabelNode) {
        this(weaveMethod, errorTrapHandler, startOfTrapLabelNode, endOfTrapLabelNode, null, null);
    }

    /**
     * This method processor will wrap weave code with try/catch blocks. Weaver.callOriginal() will not be wrapped.
     * <p/>
     * 
     * When weave code throws an unexpected exception (an exception thrown without an explicit throw call), the error
     * handler will be invoked and then the original return value will be returned. The error handler may throw an
     * exception to prevent the original return value from being returned.
     * <p/>
     * 
     * Weave code which occurs before callOriginal is the preamble. Weave code which occurs after callOriginal is the
     * postamble.
     *
     * @return
     */
    public MethodNode process() {
        if (errorTrapHandler == ErrorTrapHandler.NO_ERROR_TRAP_HANDLER) {
            return this.trappedMethod;
        }
        Type originalReturnType = Type.getReturnType(trappedMethod.desc);
        boolean isVoid = originalReturnType.equals(Type.VOID_TYPE);

        if ((null == startOfOriginalMethodLabelNode || null == endOfOriginalMethodLabelNode) && !isVoid) {
            // no support for error-trapping non-void methods which do not call Weaver.callOriginal()
            return trappedMethod;
        }

        List<TryCatchBlockNode> priorityTryCatch = new ArrayList<>();
        if (null != trappedMethod.tryCatchBlocks) {
            // weave try/catch blocks take priority over error-trap
            priorityTryCatch.addAll(trappedMethod.tryCatchBlocks);
        }

        // Used to write the bytecode for error trapping
        MethodNode generatorMethod = new SynchronizedMethodNode(WeaveUtils.ASM_API_LEVEL);
        GeneratorAdapter generator = new GeneratorAdapter(trappedMethod.access, new Method(trappedMethod.name,
                trappedMethod.desc), generatorMethod);

        // Any errors which are explicitly thrown will not go through the error trap.
        LocalVariableNode weaveExplicitThrow = null;
        if (isThrowCalled(trappedMethod)) {
            // create a variable to store the explicit throw
            weaveExplicitThrow = new LocalVariableNode("weaveExplicitThrow", Type.getDescriptor(Throwable.class), null,
                    startOfTrapLabelNode, endOfTrapLabelNode, findIndexForNewLocal(trappedMethod));
            trappedMethod.maxLocals++;
            generator.push((String) null);
            generator.storeLocal(weaveExplicitThrow.index, Type.getType(Throwable.class));
            trappedMethod.visitLocalVariable(weaveExplicitThrow.name, weaveExplicitThrow.desc,
                    weaveExplicitThrow.signature, weaveExplicitThrow.start.getLabel(),
                    weaveExplicitThrow.end.getLabel(), weaveExplicitThrow.index);

            ErrorTrapWeaveMethodsProcessor.storeExceptionAtThrowSites(trappedMethod, weaveExplicitThrow.index);

            trappedMethod.instructions.insertBefore(startOfTrapLabelNode, generatorMethod.instructions);
        }

        if (null == startOfOriginalMethodLabelNode || null == endOfOriginalMethodLabelNode) {
            // a void method which does not call Weaver.callOriginal. Probably a constructor.

            // @formatter:off
            /*-
             * Throwable weaveExplicitThrow = null;
             * try{
             *     //weave code
             * }
             * catch(Throwable t){
             *     if(null != weaveExplicitThrow)
             *          throw t;
             *     errorHandle(t);
             *     return;
             * }
             */
            // @formatter:on

            generator.goTo(endOfTrapLabelNode.getLabel());
            Label handler = new Label();
            generator.visitLabel(handler);

            if (null != weaveExplicitThrow) {
                writeRethrowExplicitThrow(generator, weaveExplicitThrow);
            }

            this.writeHandler(generator, errorTrapHandler);

            trappedMethod.instructions.insertBefore(endOfTrapLabelNode, generatorMethod.instructions);
            trappedMethod.visitTryCatchBlock(startOfTrapLabelNode.getLabel(), handler, handler,
                    Type.getInternalName(Throwable.class));
        } else {
            // @formatter:off
            /*-
            * Throwable weaveExplicitThrow = null;
            * boolean weaveThrowableWasThrown = false;
            * try{
            *     //weave preamble
            * }
            * catch(Throwable t){
            *     if(null != weaveExplicitThrow)
            *          throw t;
            *     errorHandle(t)
            *     weaveExceptionWasThrown = true;
            * }
            * Object weaveOriginalReturnVale = Weaver.callOriginal()
            * if(weaveExceptionWasThrown){
            *     return weaveOriginalReturnValue
            * }
            * try{
            *     //weave postamble
            * }
            * catch(Throwable t){
            *     if(null != weaveExplicitThrow)
            *          throw t;
            *     errorHandle(t)
            *     return weaveOriginalReturnValue
            * }
            */
            // @formatter:on

            LocalVariableNode weaveThrowableWasThrown;
            LocalVariableNode weaveOriginalReturnValue = null;

            LabelNode localsStart = WeaveUtils.makeLabelNode();
            trappedMethod.instructions.insert(localsStart);

            initializePreambleLocals(trappedMethod, startOfOriginalMethodLabelNode, localsStart);

            // Store the original return value in a new local variable
            if (!isVoid) {
                weaveOriginalReturnValue = new LocalVariableNode("weaveOriginalReturnValue",
                        originalReturnType.getDescriptor(), null, startOfTrapLabelNode, endOfTrapLabelNode,
                        findIndexForNewLocal(trappedMethod));
                trappedMethod.maxLocals++;
                trappedMethod.visitLocalVariable(weaveOriginalReturnValue.name, weaveOriginalReturnValue.desc,
                        weaveOriginalReturnValue.signature, weaveOriginalReturnValue.start.getLabel(),
                        weaveOriginalReturnValue.end.getLabel(), weaveOriginalReturnValue.index);

                storeOriginalReturnValue(generatorMethod, generator, trappedMethod, startOfOriginalMethodLabelNode,
                        endOfOriginalMethodLabelNode, weaveOriginalReturnValue);
            }

            // add and initialize weaveThrowableWasThrown
            {
                // generator.visitLabel(localsStart.getLabel());
                weaveThrowableWasThrown = new LocalVariableNode("weaveThrowableWasThrown",
                        Type.BOOLEAN_TYPE.getDescriptor(), null, startOfTrapLabelNode, endOfTrapLabelNode,
                        findIndexForNewLocal(trappedMethod));
                trappedMethod.maxLocals++;
                trappedMethod.visitLocalVariable(weaveThrowableWasThrown.name, weaveThrowableWasThrown.desc,
                        weaveThrowableWasThrown.signature, weaveThrowableWasThrown.start.getLabel(),
                        weaveThrowableWasThrown.end.getLabel(), weaveThrowableWasThrown.index);

                writeStoreInitialValue(generator, weaveThrowableWasThrown);
                if (!isVoid) {
                    writeStoreInitialValue(generator, weaveOriginalReturnValue);
                }

                trappedMethod.instructions.insertBefore(startOfTrapLabelNode, generatorMethod.instructions);

            }

            // handler for preabmle
            {
                generator.goTo(startOfOriginalMethodLabelNode.getLabel());
                Label preambleHandler = new Label();
                generator.visitLabel(preambleHandler);

                if (null != weaveExplicitThrow) {
                    writeRethrowExplicitThrow(generator, weaveExplicitThrow);
                }

                writeHandler(generator, errorTrapHandler);

                generator.push(true);
                generator.storeLocal(weaveThrowableWasThrown.index, Type.BOOLEAN_TYPE);

                trappedMethod.instructions.insertBefore(startOfOriginalMethodLabelNode, generatorMethod.instructions);

                trappedMethod.visitTryCatchBlock(startOfTrapLabelNode.getLabel(), preambleHandler, preambleHandler,
                        Type.getInternalName(Throwable.class));
            }

            // return weaveOriginalReturnValue if weaveThrowableWasThrown
            {
                LabelNode continueMethod = WeaveUtils.makeLabelNode();

                generator.push(false);
                generator.loadLocal(weaveThrowableWasThrown.index);
                generator.ifICmp(Opcodes.IFEQ, continueMethod.getLabel());

                if (isVoid) {
                    generator.visitInsn(Opcodes.RETURN);
                } else {
                    generator.loadLocal(weaveOriginalReturnValue.index);
                    generator.returnValue();
                }
                generator.visitLabel(continueMethod.getLabel());

                trappedMethod.instructions.insertBefore(endOfOriginalMethodLabelNode, generatorMethod.instructions);
            }

            // handler for postamble
            {
                Label postambleHandler = new Label();
                generator.visitLabel(postambleHandler);

                if (null != weaveExplicitThrow) {
                    writeRethrowExplicitThrow(generator, weaveExplicitThrow);
                }

                writeHandler(generator, errorTrapHandler);

                if (isVoid) {
                    generator.visitInsn(Opcodes.RETURN);
                } else {
                    generator.loadLocal(weaveOriginalReturnValue.index);
                    generator.returnValue();
                }
                trappedMethod.instructions.insertBefore(endOfTrapLabelNode, generatorMethod.instructions);

                trappedMethod.visitTryCatchBlock(endOfOriginalMethodLabelNode.getLabel(), postambleHandler,
                        postambleHandler, Type.getInternalName(Throwable.class));
            }
        }

        if (trappedMethod.tryCatchBlocks != null && priorityTryCatch.size() > 0) {
            sortTryCatchBlocks(trappedMethod, priorityTryCatch);
        }
        trappedMethod.instructions.resetLabels();

        return trappedMethod;
    }

    /**
     * Find an index where a new local variable can be inserted.
     *
     * @param method
     * @return
     */
    private static int findIndexForNewLocal(MethodNode method) {
        // slot numbers will be recomputed later, so we just have to make sure we don't overlap with another local
        return 2 * method.maxLocals;
    }

    /**
     * 
     * @param methodNode the method node to check.
     * @return true if the method has an ATHROW instruction.
     */
    private static boolean isThrowCalled(MethodNode methodNode) {
        for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
            if (Opcodes.ATHROW == insnNode.getOpcode()) {
                return true;
            }
        }
        return false;
    }

    /**
     * When we see an ATHROW instruction, store the exception and reload it so that we can rethrow it at the end of the
     * method.
     * <p/>
     * 
     * This allows us to not error trap explicit throw calls.
     *
     * @param rethrowExceptionIndex
     */
    private static void storeExceptionAtThrowSites(MethodNode methodNode, int rethrowExceptionIndex) {
        for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
            if (Opcodes.ATHROW == insnNode.getOpcode()) {
                // store and reload the exception
                methodNode.instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ASTORE, rethrowExceptionIndex));
                methodNode.instructions.insertBefore(insnNode, new VarInsnNode(Opcodes.ALOAD, rethrowExceptionIndex));
            }
        }
    }

    /**
     * Writes bytecode. Rethrow exceptions which were explicitly thrown from weaved code.
     */
    private static void writeRethrowExplicitThrow(GeneratorAdapter generator, LocalVariableNode weaveExplicitThrow) {
        Label trapStart = new Label();
        generator.loadLocal(weaveExplicitThrow.index);
        generator.ifNull(trapStart);
        generator.throwException();
        generator.visitLabel(trapStart);
    }

    /**
     * Writes bytecode. Generates instructions to initialize a variable.
     * <p/>
     * Objects are initialized to null, numbers to zero, and booleans to false.
     * 
     * @param generator
     * @param local
     */
    private static void writeStoreInitialValue(GeneratorAdapter generator, LocalVariableNode local) {
        Type type = Type.getType(local.desc);
        switch (type.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            generator.push((String) null);
            break;
        case Type.SHORT:
        case Type.BYTE:
        case Type.CHAR:
        case Type.BOOLEAN:
            generator.push(false);
            break;
        case Type.INT:
            generator.push(0);
            break;
        case Type.LONG:
            generator.push(0l);
            break;
        case Type.FLOAT:
            generator.push(0f);
            break;
        case Type.DOUBLE:
            generator.push(0d);
            break;
        }
        generator.storeLocal(local.index, type);
    }

    /**
     * Find the place before callOriginal() where the stack size is zero.
     *
     * @param methodNode
     * @return
     */
    private static int getNewOriginalInsertPoint(MethodNode methodNode, LabelNode startOfOriginalMethodLabelNode) {
        AnalyzerAdapter stackAnalyzer = new AnalyzerAdapter("DoesNotMatter", methodNode.access, methodNode.name,
                methodNode.desc, new MethodVisitor(WeaveUtils.ASM_API_LEVEL) {
                });

        int lastStackZeroIndex = 0;
        AbstractInsnNode[] inst = methodNode.instructions.toArray();
        for (int instructionIndex = 0; instructionIndex < inst.length; instructionIndex++) {
            int stackSize = stackAnalyzer.stack == null ? 0 : stackAnalyzer.stack.size();
            if (stackSize == 0) {
                lastStackZeroIndex = instructionIndex;
            }
            inst[instructionIndex].accept(stackAnalyzer);
            if (inst[instructionIndex] == startOfOriginalMethodLabelNode) {
                if (stackSize > 0) {
                    return lastStackZeroIndex;
                }
                return -1;
            }
        }
        return -1;
    }

    /**
     * Store the result of callOriginal() in a local variable.
     */
    private static void storeOriginalReturnValue(MethodNode generatorMethod, GeneratorAdapter generator,
            MethodNode methodNode, LabelNode startOfOriginalMethodLabelNode, LabelNode endOfOriginalMethodLabelNode,
            LocalVariableNode localToStoreIn) {
        Type originalReturnType = Type.getReturnType(methodNode.desc);

        // 1. insert local store after callOriginal()
        Label localStore = new Label();
        generator.visitLabel(localStore);
        generator.storeLocal(localToStoreIn.index, originalReturnType);
        methodNode.instructions.insertBefore(endOfOriginalMethodLabelNode, generatorMethod.instructions);

        // 2. insert local load after callOriginal()
        generator.loadLocal(localToStoreIn.index, originalReturnType);
        methodNode.instructions.insert(endOfOriginalMethodLabelNode, generatorMethod.instructions);

        // 3. move callOriginal() if needed
        int newOriginalInsertPoint = getNewOriginalInsertPoint(methodNode, startOfOriginalMethodLabelNode);
        if (newOriginalInsertPoint >= 0) {
            AbstractInsnNode insertPoint = methodNode.instructions.get(newOriginalInsertPoint);

            AbstractInsnNode current = startOfOriginalMethodLabelNode;
            AbstractInsnNode next = null;
            AbstractInsnNode stop = endOfOriginalMethodLabelNode.getNext();
            while (current != stop) {
                next = current.getNext();
                methodNode.instructions.remove(current);
                generatorMethod.instructions.add(current);
                current = next;
            }

            methodNode.instructions.insertBefore(insertPoint, generatorMethod.instructions);
        }
    }

    /**
     * Write the bytecode which invokes the error handler.
     * 
     * This method assumes a throwable is on the stack.
     * 
     * @param generator
     * @param handler
     */
    private void writeHandler(GeneratorAdapter generator, ClassNode handler) {
        generator.visitMethodInsn(Opcodes.INVOKESTATIC, handler.name, ErrorTrapHandler.HANDLER_METHOD_NAME,
                ErrorTrapHandler.HANDLER_METHOD_DESC, false);
    }

    // Many of the methods below were brought in directly from mergeMethodNode. It might make sense to break them up
    // differently.

    /**
     * Weaved methods have preambles and postambles - code before and after the original method call. We wrap both of
     * these sections of code with try..catches, screwing up the scope of the local variables. For example:
     *
     * <pre>
     * String test = &quot;value&quot;;
     * callOriginal();
     * return test;
     * </pre>
     *
     * becomes:
     *
     * <pre>
     * try { String test = "value"; } ...
     * String originalReturn = callOriginal();
     * try {
     * return test;
     * } catch (Throwable t) {
     * return originalReturn;
     * }
     * </pre>
     *
     * Which is not valid because of the modified scope of the test variable.
     *
     * This method adds code to initialize the values of the local variables created before the original method body and
     * scoped for use after the method body. The above example becomes:
     *
     *
     * <pre>
     * String test = null;
     * try { test = "value"; } ...
     * String originalReturn = callOriginal();
     * try {
     * return test;
     * } catch (Throwable t) {
     * return originalReturn;
     * }
     * </pre>
     *
     * @param startOfOriginalMethodLabelNode
     */
    private void initializePreambleLocals(MethodNode method, LabelNode startOfOriginalMethodLabelNode,
            LabelNode localsStart) {
        List<LocalVariableNode> localsInPreamble = getLocalsInPreamble(method, startOfOriginalMethodLabelNode);
        if (!localsInPreamble.isEmpty()) {
            // get the index of the first local variable after the 'special' ones
            int firstLocalIndex;
            {
                Type[] argumentTypes = Type.getArgumentTypes(method.desc);
                firstLocalIndex = (Opcodes.ACC_STATIC & method.access) == 0 ? 1 : 0;
                for (int i = 0; i < argumentTypes.length; i++) {
                    firstLocalIndex += argumentTypes[i].getSize();
                }
            }

            for (LocalVariableNode local : localsInPreamble) {
                // be careful not to initialize 'this' or the method arguments
                if (local.index >= firstLocalIndex) {
                    changeLocalVariableScopeStart(method, local, localsStart);
                }
            }
        }
    }

    /**
     * Returns the local variables which are scoped before and after the given instruction.
     *
     * @param insertPoint
     * @return
     */
    private List<LocalVariableNode> getLocalsInPreamble(MethodNode method, AbstractInsnNode insertPoint) {
        int insertPointIndex = method.instructions.indexOf(insertPoint);
        if (insertPointIndex < 0) {
            return Collections.emptyList();
        }

        List<LocalVariableNode> locals = new ArrayList<>();
        for (LocalVariableNode local : (method.localVariables)) {
            int startIndex = method.instructions.indexOf(local.start);
            int end = method.instructions.indexOf(local.end);

            if (startIndex <= insertPointIndex && insertPointIndex < end) {
                locals.add(local);
            }
        }

        return locals;
    }

    /**
     * Give the local var a new start.
     *
     * @param newStart
     */
    private static void changeLocalVariableScopeStart(MethodNode method, LocalVariableNode local, LabelNode newStart) {
        Type type = Type.getType(local.desc);
        local.start = newStart;

        // Since we just changed the scope of the local var, we may have introduced a slot collision
        List<LocalVariableNode> collidingLocals = getCollidingVariables(local, method.localVariables);
        if (!collidingLocals.isEmpty()) {
            // move all the colliding locals into a different slot
            int newIndex = findIndexForNewLocal(method);
            method.maxLocals++;
            for (LocalVariableNode collidingLocal : collidingLocals) {
                collidingLocal.index = newIndex;
                changeLocalSlot(local.index, newIndex, collidingLocal.start, collidingLocal.end);
            }
        }

        AbstractInsnNode initialValue = getInitialValueInstruction(type);
        if (initialValue != null) {
            // this looks out of order because we're inserting into the top of the stack
            method.instructions.insert(newStart, new VarInsnNode(type.getOpcode(Opcodes.ISTORE), local.index));
            method.instructions.insert(newStart, initialValue);
        }
    }

    /**
     * Returns all the set of otherLocals which will try to inhabit the same slot at the same time as the local.
     * 
     * Used to detect a slot collision after changing the scope of the variable
     * 
     * @param local
     * @param otherLocals
     * @return
     */
    private static List<LocalVariableNode> getCollidingVariables(LocalVariableNode local,
            List<LocalVariableNode> otherLocals) {
        List<LocalVariableNode> collisions = new ArrayList<>();
        for (LocalVariableNode otherLocal : otherLocals) {
            if (local.name.equals(otherLocal.name) && local.desc.equals(otherLocal.desc)) {
                continue; // local doesn't collide with itself
            }
            if (shareSlot(local, otherLocal) && scopesOverlap(local, otherLocal)) {
                collisions.add(otherLocal);
            }
        }
        return collisions;
    }

    /**
     * Returns the instruction to initialize the given type.
     *
     * @param type
     * @return
     */
    private static AbstractInsnNode getInitialValueInstruction(Type type) {
        switch (type.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
            return new InsnNode(Opcodes.ACONST_NULL);
        case Type.SHORT:
        case Type.BYTE:
        case Type.CHAR:
        case Type.BOOLEAN:
        case Type.INT:
            return new InsnNode(Opcodes.ICONST_0);
        case Type.LONG:
            return new InsnNode(Opcodes.LCONST_0);
        case Type.FLOAT:
            return new InsnNode(Opcodes.FCONST_0);
        case Type.DOUBLE:
            return new InsnNode(Opcodes.DCONST_0);
        }
        return null;
    }

    /**
     * Returns true if the local inhabits the same slot as the other local.
     *
     * A local inhabits the same slot as itself.
     *
     * @param local
     * @param otherLocal
     * @return
     */
    private static boolean shareSlot(LocalVariableNode local, LocalVariableNode otherLocal) {
        return local.index == otherLocal.index;
    }

    /**
     * Returns true if the two locals have any overlap in scope.
     *
     * A local's scope overlaps with itself.
     *
     * @param local
     * @param otherLocal
     * @return
     */
    private static boolean scopesOverlap(LocalVariableNode local, LocalVariableNode otherLocal) {
        return scopeContainsAnyPartOf(local, otherLocal) || scopeContainsAnyPartOf(otherLocal, local);
    }

    /**
     * Returns true if local's scope sees the start or end of otherLocal's scope.
     *
     * @param local
     * @param otherLocal
     * @return
     */
    private static boolean scopeContainsAnyPartOf(LocalVariableNode local, LocalVariableNode otherLocal) {
        AbstractInsnNode currentNode = local.start;
        while (currentNode != null && !currentNode.equals(local.end)) {
            if (currentNode.equals(otherLocal.start)) {
                return true;
            }
            if (currentNode.equals(otherLocal.end)) {
                return !currentNode.equals(local.start);
            }
            currentNode = currentNode.getNext();
        }
        return false;
    }

    /**
     * Change a local variable's slot index between the two given labels
     *
     * @param oldSlot
     * @param newSlot
     * @param start
     * @param end
     */
    private static void changeLocalSlot(int oldSlot, int newSlot, LabelNode start, LabelNode end) {
        // look back one instruction from the start label. The previous instruction loaded the var into the slot.
        AbstractInsnNode currentNode = null == start.getPrevious() ? start : start.getPrevious();
        while (null != currentNode && !currentNode.equals(end)) {
            if (currentNode.getType() == AbstractInsnNode.VAR_INSN) { // && ((VarInsnNode) currentNode).var == oldSlot)
                VarInsnNode currentInsn = (VarInsnNode) currentNode;
                if (currentInsn.var == oldSlot) {
                    currentInsn.var = newSlot;
                }
            }
            currentNode = currentNode.getNext();
        }
    }

    /**
     * Sort try/catch blocks so that the error trap try/catch blocks have lowest priority.
     * 
     * @param trappedMethod
     * @param priorityTryCatch
     */
    public static void sortTryCatchBlocks(final MethodNode trappedMethod, final List<TryCatchBlockNode> priorityTryCatch) {
        Comparator<TryCatchBlockNode> comp = new Comparator<TryCatchBlockNode>() {
            public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
                boolean isT1Priority = priorityTryCatch.contains(t1);
                boolean isT2Priority = priorityTryCatch.contains(t2);
                if (isT1Priority && isT2Priority) {
                    return trappedMethod.instructions.indexOf(t2.start) - trappedMethod.instructions.indexOf(t1.start);
                } else if (isT1Priority) {
                    return -1;
                } else if (isT2Priority) {
                    return 1;
                } else {
                    return trappedMethod.instructions.indexOf(t2.start) - trappedMethod.instructions.indexOf(t1.start);
                }
            }
        };
        Collections.sort(trappedMethod.tryCatchBlocks, comp);
        // Updates the 'target' of each try catch block annotation.
        for (int i = 0; i < trappedMethod.tryCatchBlocks.size(); ++i) {
            trappedMethod.tryCatchBlocks.get(i).updateIndex(i);
        }
    }
}
