/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave;

import com.newrelic.weave.utils.ReferenceUtils;
import com.newrelic.weave.utils.SynchronizedClassNode;
import com.newrelic.weave.utils.WeaveUtils;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Extension classes are used to hold new fields. This class generates an extension class and rewrites weave methods to
 * use them for accessing new fields.
 *
 * Backing storage of new fields is provided by a {@link ClassNode} with the appropriate static methods from
 * {@link ExtensionClassTemplate}. The default template can be found in
 * {@link ExtensionClassTemplate#DEFAULT_EXTENSION_TEMPLATE}. The agent provides its own, allowing a higher
 * performing guava cache to be used instead.
 */
public class PreparedExtension {
    public static final String RESET_CHECK_NAME = "gen_shouldResetExtensionClass";
    public static final String RESET_CHECK_DESC = "()Z";

    private static final String EXTENSION_CLASS_FORMAT = "com/newrelic/weave/%s_%d_nr_ext";
    private static final int NO_EXTENSION_CACHE = -1;

    private final ClassMatch match;
    private final String extensionClassName;
    private final Type extensionClassType;
    private final Type originalType;
    private final ClassNode extensionTemplate;
    private final Set<String> extensionNodeFieldNames; // used to optimize field collision checking

    /**
     * Create a {@link PreparedExtension} for the speicifed {@link ClassMatch} and template.
     *
     * @param match a fully valid {@link ClassMatch}
     * @param template template class to use for storage of new fields
     * @see ExtensionClassTemplate for examples of how to provide templates
     */
    public PreparedExtension(ClassMatch match, ClassNode template) {
        this.match = match;
        extensionClassName = String.format(EXTENSION_CLASS_FORMAT, match.getOriginal().name, System.identityHashCode(match));
        this.extensionClassType = Type.getObjectType(this.extensionClassName);
        this.originalType = Type.getObjectType(match.getOriginal().name);
        this.extensionTemplate = template;
        extensionNodeFieldNames = new HashSet<>(extensionTemplate.fields.size());
        for (FieldNode fieldNode : extensionTemplate.fields) {
            extensionNodeFieldNames.add(fieldNode.name);
        }
    }

    /**
     * Generate the {@link ClassNode} for the extension class.
     *
     * @return the {@link ClassNode} for the extension class
     */
    public ClassNode generateExtensionClass() {
        // create the extension node
        ClassNode extension;
        {
            extension = new SynchronizedClassNode();
            Map<String, String> oldToNew = new HashMap<>(1);
            // first set the class name to the generated name
            oldToNew.put(extensionTemplate.name, extensionClassName);
            // supertype is just a marker. Remove for generated class
            oldToNew.put(extensionTemplate.superName, "java/lang/Object");
            ClassVisitor visitor = ReferenceUtils.getRenamingVisitor(oldToNew, extension);
            // update and methods or fields that collide with the @NewField names
            for (String newFieldName : match.getNewFields()) {
                visitor = updateCollidingFieldNames(visitor, newFieldName);
            }
            extensionTemplate.accept(visitor); // copy extensionNode (with renames) into extension
        }
        extension.visit(WeaveUtils.RUNTIME_MAX_SUPPORTED_CLASS_VERSION, ACC_PUBLIC + ACC_SUPER, extensionClassName,
                null, "java/lang/Object", extensionTemplate.interfaces.toArray(new String[extensionTemplate.interfaces.size()]));
        FieldVisitor fv;

        ClassNode weaveClass = match.getWeave();
        // add new fields
        for (String newFieldName : match.getNewFields()) {
            FieldNode newField = WeaveUtils.findRequiredMatch(weaveClass.fields, newFieldName);

            // make the fields public since they'll be accessed from a different object
            // clear private, protected and final flags
            int access = newField.access;
            access |= Opcodes.ACC_PUBLIC;
            access &= ~(Opcodes.ACC_PRIVATE + Opcodes.ACC_PROTECTED + Opcodes.ACC_FINAL);

            fv = extension.visitField(access, newField.name, newField.desc, newField.signature, newField.value);
            fv.visitEnd();
        }

        if (match.getExtensionClassInit() != null) {
            // static init
            MethodNode clinitMethodNode = WeaveUtils.getMethodNode(extension, "<clinit>", "()V");
            if (null == clinitMethodNode) { // no clinit, let's make an empty one
                extension.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                clinitMethodNode = WeaveUtils.getMethodNode(extension, "<clinit>", "()V");

                clinitMethodNode.visitCode();
                clinitMethodNode.visitInsn(RETURN);
                clinitMethodNode.visitMaxs(0, 0); // these get recomputed later
                clinitMethodNode.visitEnd();
            }
            // initialize new fields from weaver class initializer
            Set<MethodNode> toInline = new HashSet<>();
            MethodNode classInit = rewriteNewFieldCalls(match.getExtensionClassInit());
            for (Method newMethod : match.getNewMethods()) {
                MethodNode newMethodNode = WeaveUtils.findMatch(match.getWeave().methods, newMethod);
                if (newMethodNode != null) {
                    toInline.add(newMethodNode);
                }
            }
            classInit = MethodProcessors.inlineMethods(weaveClass.name, toInline, extensionClassName, classInit);

            AbstractInsnNode retInsn = clinitMethodNode.instructions.getLast();
            while (retInsn != null && retInsn.getOpcode() != Opcodes.RETURN) {
                retInsn = retInsn.getPrevious();
            }
            if (null != retInsn) {
                clinitMethodNode.instructions.remove(retInsn);
            }
            clinitMethodNode.instructions.add(classInit.instructions);
            clinitMethodNode.instructions.resetLabels();
        }
        extension.visitEnd();

        extension.visitMethod(ACC_PUBLIC, RESET_CHECK_NAME, RESET_CHECK_DESC, null, null);
        MethodNode resetMethod = WeaveUtils.getMethodNode(extension, RESET_CHECK_NAME, RESET_CHECK_DESC);
        final Label FALSE = new Label();
        for (String newFieldName : match.getNewFields()) {
            FieldNode newFieldNode = WeaveUtils.findMatch(match.getWeave().fields, newFieldName);
            if ((newFieldNode.access & Opcodes.ACC_STATIC) == 0) {
                writeFieldResetCheck(resetMethod, newFieldNode, FALSE);
            }
        }
        resetMethod.visitInsn(Opcodes.ICONST_1);
        resetMethod.visitInsn(IRETURN);

        resetMethod.visitLabel(FALSE);
        resetMethod.visitInsn(Opcodes.ICONST_0);
        resetMethod.visitInsn(IRETURN);
        resetMethod.visitMaxs(0, 0);
        resetMethod.visitEnd();

        return extension;
    }

    /**
     * Generate bytecode which will check a field (of any type) for a null (or zero) value.
     */
    private void writeFieldResetCheck(MethodNode methodNode, FieldNode fieldNode, final Label FALSE) {
        final Type fieldType = Type.getType(fieldNode.desc);

        methodNode.visitVarInsn(Opcodes.ALOAD, 0);
        methodNode.visitFieldInsn(Opcodes.GETFIELD, this.getExtensionClassName(), fieldNode.name, fieldNode.desc);
        switch (fieldType.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                methodNode.visitJumpInsn(Opcodes.IFNONNULL, FALSE);
                break;
            case Type.SHORT:
            case Type.BYTE:
            case Type.CHAR:
            case Type.BOOLEAN:
            case Type.INT:
                methodNode.visitInsn(Opcodes.ICONST_0);
                methodNode.visitJumpInsn(Opcodes.IF_ICMPNE, FALSE);
                break;
            case Type.LONG:
                methodNode.visitInsn(Opcodes.LCONST_0);
                methodNode.visitInsn(Opcodes.LCMP); // will push zero if the two longs are equal
                methodNode.visitInsn(Opcodes.ICONST_0);
                methodNode.visitJumpInsn(Opcodes.IF_ICMPNE, FALSE);
                break;
            case Type.FLOAT:
                methodNode.visitInsn(Opcodes.FCONST_0);
                methodNode.visitInsn(Opcodes.FCMPL);
                methodNode.visitInsn(Opcodes.ICONST_0);
                methodNode.visitJumpInsn(Opcodes.IF_ICMPNE, FALSE);
                break;
            case Type.DOUBLE:
                methodNode.visitInsn(Opcodes.DCONST_0);
                methodNode.visitInsn(Opcodes.DCMPL);
                methodNode.visitInsn(Opcodes.ICONST_0);
                methodNode.visitJumpInsn(Opcodes.IF_ICMPNE, FALSE);
                break;
        }
    }

    /**
     * Updates a new field name to avoid collisions with fields in an original class.
     *
     * @param visitor delegate {@link ClassVisitor}
     * @param newFieldName new field to check
     * @return {@link ClassVisitor} that will update names when visited
     */
    private ClassVisitor updateCollidingFieldNames(ClassVisitor visitor, String newFieldName) {
        if (extensionNodeFieldNames.contains(newFieldName)) {
            // If we get into here it's going to be slow to check for all possible name collisions.
            // Collisions should be very rare and further optimizations would muddy the code for a one-time hit which is
            // rarely taken.
            String renamedField = "ext_" + newFieldName;
            while (extensionNodeFieldNames.contains(renamedField) || match.getNewFields().contains(renamedField)) {
                renamedField = "ext_" + renamedField;
            }
            extensionNodeFieldNames.remove(newFieldName);
            extensionNodeFieldNames.add(renamedField);
            visitor = ReferenceUtils.getFieldRenamingVisitor(visitor, extensionTemplate.name, newFieldName,
                    renamedField);
        }
        // make sure field names don't collide
        FieldNode collidingField = WeaveUtils.findMatch(extensionTemplate.fields, newFieldName);
        if (null != collidingField) {
            String renamedField = "ext_" + collidingField.name;
            while (null != WeaveUtils.findMatch(extensionTemplate.fields, renamedField)
                    || match.getNewFields().contains(renamedField)) {
                renamedField = "ext_" + collidingField.name;
            }
            visitor = ReferenceUtils.getFieldRenamingVisitor(visitor, extensionTemplate.name, newFieldName,
                    renamedField);
        }
        return visitor;
    }

    /**
     * Rewrite GETFIELD and PUTFIELD calls for new fields to use the extension class. If a weave class has any new
     * fields, all methods, including constructors and initializers, should be processed by this method.
     *
     * @param weaveMethod method on weave class to rewrite
     * @return method that uses the extension class for field access
     */
    public MethodNode rewriteNewFieldCalls(final MethodNode weaveMethod) {
        if (null != weaveMethod.instructions) {
            List<FieldInsnNode> newFieldInstructions = new ArrayList<>();
            // look for all NewField instructions (or NewField generated accessors)
            for (AbstractInsnNode current = weaveMethod.instructions.getFirst(); null != current; current = current.getNext()) {
                if (AbstractInsnNode.METHOD_INSN == current.getType()) {
                    MethodInsnNode methodInsn = (MethodInsnNode) current;
                    if (methodInsn.owner.equals(match.getWeave().name)) {
                        Method key = new Method(methodInsn.name, methodInsn.desc);
                        GeneratedNewFieldMethod newFieldMethod = match.getGeneratedNewFieldMethods().get(key);
                        if (null != newFieldMethod) {
                            current = this.generatedAccessorCall(weaveMethod.instructions, methodInsn, newFieldMethod);
                            // now current is a fieldInsn and will be added to newFieldInstructions below
                        }
                    }
                }
                if (AbstractInsnNode.FIELD_INSN == current.getType()) {
                    FieldInsnNode fieldInsn = (FieldInsnNode) current;
                    if (fieldInsn.owner.equals(match.getWeave().name) && match.getNewFields().contains(
                            fieldInsn.name)) {
                        newFieldInstructions.add(fieldInsn);
                    }
                }
            }
            int cachedExtSlot = NO_EXTENSION_CACHE;
            if (newFieldInstructions.size() > 1) {
                // cache the extension class in a local variable to avoid additional map hits
                cachedExtSlot = createNewLocal(weaveMethod);
                InsnNode pushNull = new InsnNode(Opcodes.ACONST_NULL);
                weaveMethod.instructions.insert(pushNull);
                weaveMethod.instructions.insert(pushNull, new VarInsnNode(Opcodes.ASTORE, cachedExtSlot));
            }
            for (FieldInsnNode fieldNode : newFieldInstructions) {
                newFieldCall(weaveMethod.instructions, fieldNode, cachedExtSlot);
            }
            if (newFieldInstructions.size() > 0) {
                weaveMethod.instructions.resetLabels();
            }
        }
        return weaveMethod;
    }

    /**
     * Nested classes may access NewField calls through generated accessors (e.g. access$300). This changes the calls to
     * the accessor to field calls.
     *
     * @param instructions Instructions to modify
     * @param methodInsn The MethodInsnNode which is calling the accessor
     * @param newFieldMethod {@link GeneratedNewFieldMethod} created by {@link ClassMatch}.
     * @return The newly created {@link FieldInsnNode} which contins the NewField op
     */
    private FieldInsnNode generatedAccessorCall(InsnList instructions, MethodInsnNode methodInsn,
            GeneratedNewFieldMethod newFieldMethod) {
        // most generated new field PUT mutators return the value of the field
        // this checks for that and DUPs the value on the stack as appropriate
        if (newFieldMethod.returnsPutValue) {
            int dupOpcode;
            if (newFieldMethod.opcode == PUTSTATIC) {
                int dupSize = newFieldMethod.method.getArgumentTypes()[0].getSize();
                dupOpcode = dupSize == 1 ? Opcodes.DUP : Opcodes.DUP2;
            } else {
                int dupSize = newFieldMethod.method.getArgumentTypes()[1].getSize();
                dupOpcode = dupSize == 1 ? Opcodes.DUP_X1 : Opcodes.DUP2_X1;
            }
            instructions.insertBefore(methodInsn, new InsnNode(dupOpcode));
        }
        FieldInsnNode fieldInsn = new FieldInsnNode(newFieldMethod.opcode, methodInsn.owner,
                newFieldMethod.newFieldName, newFieldMethod.newFieldDesc);
        instructions.insert(methodInsn, fieldInsn);
        instructions.remove(methodInsn);
        return fieldInsn;
    }

    /**
     * Change a field op on a NewField to use the extension class.
     *
     * @param instructions Instructions to modify.
     * @param fieldInsn The field instruction to modify
     * @param cachedExtSlot The slot number to access the cached extension. If passed {@link #NO_EXTENSION_CACHE}
     * caching will not take place.
     */
    private void newFieldCall(InsnList instructions, FieldInsnNode fieldInsn, int cachedExtSlot) {
        if (PUTFIELD == fieldInsn.getOpcode() || GETFIELD == fieldInsn.getOpcode()) {
            Type fieldType = Type.getType(fieldInsn.desc);

            // map operation markers used by extension cache
            LabelNode beforeMapLoad = new LabelNode(new Label());
            LabelNode afterMapLoad = new LabelNode(new Label());

            // marks places the original or extension object will be on the top of the stack.
            // These values will be dup'd so they can be passed to the map-removal checking bytecode
            List<AbstractInsnNode> removalCheckDups = new ArrayList<>();

            // load from map and cache
            instructions.insertBefore(fieldInsn, beforeMapLoad);
            if (PUTFIELD == fieldInsn.getOpcode()) {
                swap(instructions, fieldInsn, fieldType, originalType);
                removalCheckDups.add(fieldInsn.getPrevious());
            }
            MethodInsnNode getExtensionClassInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, extensionClassName,
                    ExtensionClassTemplate.GET_EXTENSION_METHOD, getExtensionMethodDesc(), false);
            instructions.insertBefore(fieldInsn, getExtensionClassInsn);
            instructions.insertBefore(fieldInsn, afterMapLoad);

            writeLocalVarCaching(instructions, fieldInsn, beforeMapLoad, afterMapLoad, removalCheckDups, cachedExtSlot);
            if (PUTFIELD == fieldInsn.getOpcode()) {
                removalCheckDups.add(fieldInsn.getPrevious());
                swap(instructions, fieldInsn, extensionClassType, fieldType);

                LabelNode afterMapRemoval = writeRemovalCheck(instructions, fieldInsn, removalCheckDups);
                invalidateLocalVarCache(instructions, afterMapRemoval, cachedExtSlot);
            }
        }
        fieldInsn.owner = extensionClassName;
    }

    /**
     * Write they bytecode to save the extension class in a local variable. Generally looks like: ExtensionClass
     * cachedExt = null //... if (null == cachedExt) { cachedExt = getExtension(original) } cachedExt.newField
     */
    private void writeLocalVarCaching(InsnList instructions, FieldInsnNode fieldInsn, LabelNode beforeMapLoad,
            LabelNode afterMapLoad, List<AbstractInsnNode> removalCheckDups, int cachedExtSlot) {
        Type fieldType = Type.getType(fieldInsn.desc);

        // if cachedExt == null
        if (cachedExtSlot != NO_EXTENSION_CACHE) {
            instructions.insertBefore(beforeMapLoad, new VarInsnNode(Opcodes.ALOAD, cachedExtSlot));
            instructions.insertBefore(beforeMapLoad, new JumpInsnNode(Opcodes.IFNULL, beforeMapLoad));

            if (PUTFIELD == fieldInsn.getOpcode()) {
                // PUTFIELD has the value to put on the top of the stack, so we need to do some swapping to pop off
                // the original owner
                swap(instructions, beforeMapLoad, fieldType, extensionClassType);
                removalCheckDups.add(beforeMapLoad.getPrevious());
            }
            instructions.insertBefore(beforeMapLoad, new InsnNode(Opcodes.POP));
            // original is popped
            instructions.insertBefore(beforeMapLoad, new JumpInsnNode(Opcodes.GOTO, afterMapLoad));

            // store after map
            instructions.insertBefore(afterMapLoad, new VarInsnNode(Opcodes.ASTORE, cachedExtSlot));
            // load from cached var
            instructions.insert(afterMapLoad, new VarInsnNode(Opcodes.ALOAD, cachedExtSlot));
        }
    }

    /**
     * Write bytecode to set the local var that is caching the extension instance to null.
     */
    private void invalidateLocalVarCache(InsnList instructions, AbstractInsnNode afterMapRemoval, int cachedExtSlot) {
        if (cachedExtSlot != NO_EXTENSION_CACHE) {
            instructions.insertBefore(afterMapRemoval, new InsnNode(Opcodes.ACONST_NULL));
            instructions.insertBefore(afterMapRemoval, new VarInsnNode(Opcodes.ASTORE, cachedExtSlot));
        }
    }

    /**
     * After PUTFIELD ops, write bytecode to remove the extension class from the backing map if all NewFields have been
     * reset (set to null or default value). This was done to allow instrumentation to reclaim memory more quickly,
     * rather than relying on the GC to collect weak references.
     */
    private LabelNode writeRemovalCheck(InsnList instructions, FieldInsnNode fieldInsn,
            List<AbstractInsnNode> removalCheckDups) {
        Type fieldType = Type.getType(fieldInsn.desc);

        for (AbstractInsnNode insertPoint : removalCheckDups) {
            // we need to dup the original object so we can pass it as an arg to remove from the map
            dup_two_below(instructions, insertPoint.getNext(), originalType, fieldType);
        }

        // on PUTFILD ops, we check to see if all newfields have been nulled or zeroed out.
        // if they have, we can remove the extension class from the map

        // these instructions will be placed after the newfield operation
        // stack: extensionInstance, newFieldValue
        final LabelNode nfOpComplete = new LabelNode(new Label());
        final LabelNode popOriginal = new LabelNode(new Label());
        final LabelNode afterRemovalMapOp = new LabelNode(new Label());
        instructions.insert(fieldInsn, nfOpComplete);

        MethodInsnNode shouldResetInsn = new MethodInsnNode(Opcodes.INVOKEVIRTUAL, extensionClassName, RESET_CHECK_NAME,
                RESET_CHECK_DESC, false);
        instructions.insertBefore(nfOpComplete, shouldResetInsn);
        instructions.insertBefore(nfOpComplete, new InsnNode(Opcodes.ICONST_0));
        instructions.insertBefore(nfOpComplete, new JumpInsnNode(Opcodes.IF_ICMPEQ, popOriginal));

        MethodInsnNode removeExtensionClassInsn = new MethodInsnNode(Opcodes.INVOKESTATIC, extensionClassName,
                ExtensionClassTemplate.GET_AND_REMOVE_EXTENSION_METHOD, getExtensionMethodDesc(), false);
        instructions.insertBefore(nfOpComplete, removeExtensionClassInsn);
        instructions.insertBefore(nfOpComplete, afterRemovalMapOp);
        instructions.insertBefore(nfOpComplete, new InsnNode(Opcodes.POP));
        instructions.insertBefore(nfOpComplete, new JumpInsnNode(Opcodes.GOTO, nfOpComplete));

        instructions.insertBefore(nfOpComplete, popOriginal);
        instructions.insertBefore(nfOpComplete, new InsnNode(Opcodes.POP));
        return afterRemovalMapOp;
    }

    // take the value on the top of the stack, dup it and insert it two below the top of the stack
    private static void dup_two_below(InsnList instructions, AbstractInsnNode insertPoint, Type stackTop,
            Type belowTop) {
        if (stackTop.getSize() == 1) {
            if (belowTop.getSize() == 1) {
                // Top = 1, below = 1
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP_X1));
            } else {
                // Top = 1, below = 2
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP_X2));
            }
        } else {
            if (belowTop.getSize() == 1) {
                // Top = 2, below = 1
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP2_X1));
            } else {
                // Top = 2, below = 2
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP2_X2));
            }
        }
    }

    /**
     * Insert bytecode instructions which swap the top two stack values. This works category 1 and 2 types (i.e. it
     * works even if one or both of the values are a double or long).
     *
     * @param instructions Instructions to insert the instructions
     * @param insertPoint Swap instructions will be inserted before this instruction
     * @param stackTop The type of the top of the stack
     * @param belowTop The type of the value below the top of the stack
     */
    private static void swap(InsnList instructions, AbstractInsnNode insertPoint, Type stackTop, Type belowTop) {
        if (stackTop.getSize() == 1) {
            if (belowTop.getSize() == 1) {
                // Top = 1, below = 1
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.SWAP));
            } else {
                // Top = 1, below = 2
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP_X2));
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.POP));
            }
        } else {
            if (belowTop.getSize() == 1) {
                // Top = 2, below = 1
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP2_X1));
            } else {
                // Top = 2, below = 2
                instructions.insertBefore(insertPoint, new InsnNode(Opcodes.DUP2_X2));
            }
            instructions.insertBefore(insertPoint, new InsnNode(Opcodes.POP2));
        }
    }

    private static int createNewLocal(MethodNode method) {
        // slot numbers will be recomputed later, so we just have to make sure we don't overlap with another local
        int newLocalIndex = 2 * method.maxLocals;
        method.maxLocals++;
        return newLocalIndex;
    }

    /**
     * The name of the extension class, or <code>null</code> if none was generated.
     *
     * @return name of the extension class, or <code>null</code> if none was generated
     */
    public String getExtensionClassName() {
        return this.extensionClassName;
    }

    private String getExtensionMethodDesc() {
        return "(Ljava/lang/Object;)L" + extensionClassName + ";";
    }

}
