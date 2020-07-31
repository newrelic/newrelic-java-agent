/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.utils;

import com.newrelic.weave.WeaveTestUtils;
import com.newrelic.weave.weavepackage.Reference;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReferenceUtilsTest {
    private static final ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    /**
     * Does the following<ul>
     * <li>Load the given class names into class nodes</li>
     * <li>Rename oldName to newName on all class nodes</li>
     * <li>Assert that all non-renamed references are unchanged</li>
     * <li>Assert that all oldName references are updated to newName</li>
     * </ul>
     */
    private void renameAndAssert(final String oldName, final String newName, final String[] classesToRead) throws IOException {
        List<ClassNode> classNodes = new ArrayList<>(classesToRead.length);
        for (String className : classesToRead) {
            ClassNode node = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(className,
                    classloader));
            Assert.assertNotNull(node);
            classNodes.add(node);
        }
        Map<String, Integer> oldNameCounts = this.getReferenceNameCounts(classNodes);
        Assert.assertTrue("No references to rename", oldNameCounts.get(oldName) != null && oldNameCounts.get(oldName) > 0);
        Assert.assertEquals("Should be no references to new name before the rename: "+newName, 0, oldNameCounts.get(newName) == null ? 0 : oldNameCounts.get(newName));
        List<ClassNode> newNodes = new ArrayList<>(classNodes.size());
        for (ClassNode node : classNodes) {
            newNodes.add(renameClassReferenceOnClassNode(node, oldName, newName));
        }
        classNodes = newNodes;
        Map<String, Integer> newNameCounts = this.getReferenceNameCounts(classNodes);
        Assert.assertEquals("Should be no references to old name after the rename: "+oldName, 0, newNameCounts.get(oldName) == null ? 0 : newNameCounts.get(oldName));

        // assert only oldName was updated
        for (String className : oldNameCounts.keySet()) {
            if (oldName.equals(className)) {
                Assert.assertEquals(String.format("All %s should be updated to an equal number of %s", oldName, newName), oldNameCounts.get(className), newNameCounts.get(newName));
                // System.out.println(String.format("renamed counts of %s: %d", className, oldNameCounts.get(oldName)));
            } else {
                Assert.assertEquals(String.format("Reference count of %s should be unchanged.", className), oldNameCounts.get(className), newNameCounts.get(className));
                // System.out.println(String.format("counts of %s: %d", className, oldNameCounts.get(className)));
            }
        }
    }

    private static ClassNode renameClassReferenceOnClassNode(final ClassNode node, final String oldName, final String newName) {
        ClassNode copy = new ClassNode();
        Map<String, String> oldToNew = new HashMap<>(1);
        oldToNew.put(oldName, newName);
        ClassVisitor visitor = node;
        visitor = ReferenceUtils.getRenamingVisitor(oldToNew, copy);
        node.accept(visitor);
        return copy;
    }

    private Map<String, Integer> getReferenceNameCounts(final List<ClassNode> classNodes) {
        Map<String, Integer> nameCounts = new HashMap<>();
        for (ClassNode node : classNodes) {
            Set<Reference> refs = Reference.create(node);
            for (Reference ref : refs) {
                if (!nameCounts.containsKey(ref.className)) {
                    nameCounts.put(ref.className, 0);
                }
                nameCounts.put(ref.className, nameCounts.get(ref.className)+1);
            }
        }
        return nameCounts;
    }

    @Test
    public void testExactAndInnerClass() throws IOException {
        final String oldName = WeaveUtils.getClassInternalName(OldName.class.getName());
        final String newName = oldName.replaceFirst("OldName", "NewName");
        final String[] classesToRead = { OldName.class.getName(), OldName.Inner.class.getName() };
        this.renameAndAssert(oldName, newName, classesToRead);
    }

    @Test
    public void testAnotherClassRefs() throws IOException {
        final String oldName = WeaveUtils.getClassInternalName(OldName.class.getName());
        final String newName = oldName.replaceFirst("OldName", "NewName");
        final String[] classesToRead = { AnotherClass.class.getName() };
        this.renameAndAssert(oldName, newName, classesToRead);
    }

    public static class OldName {
        public final String afield = "afield";
        public void noop() {
        }

        public class Inner {
            // Inner has a reference to the outer class (created by compiler)
        }
    }

    public static class AnotherClass extends OldName {
        public OldName fieldRef = null;
        public OldName[] fieldArray = null;
        public OldName[][] fieldDoubleArray = null; // what does it mean?!?

        public OldName methodRefs(OldName name) {
            name.noop();
            name.afield.toString();
            return name;
        }
    }

    @Test
    public void renameInterface() throws IOException {
        final String oldName = WeaveUtils.getClassInternalName(OldNameInterface.class.getName());
        final String newName = oldName.replaceFirst("OldName", "NewName");
        final String[] classesToRead = { OldNameInterface.class.getName(), ImplClass.class.getName() };
        this.renameAndAssert(oldName, newName, classesToRead);
    }

    public static interface OldNameInterface {
        void foo();
    }

    public static class ImplClass implements OldNameInterface {
        @Override
        public void foo() {
        }

        public void invokesInterface(OldNameInterface oni) {
            oni.foo();
        }
    }

    @Test
    public void testNotRenamed() throws IOException {
        final String oldName = WeaveUtils.getClassInternalName(AClassToRename.class.getName());
        final String newName = oldName.replaceFirst("AClassToRename", "NewClassName");
        final String[] classesToRead = { AClassToRename.class.getName(), AClassToRenameNot.class.getName() };
        this.renameAndAssert(oldName, newName, classesToRead);
    }

    // should be renamed
    public static class AClassToRename {
        public final String afield = "afield";
        private AClassToRename renamedField;
        private AClassToRenameNot untouchedField;

        public AClassToRename methodRefs(AClassToRename name, AClassToRenameNot not) {
            name.noop();
            name.afield.toString();
            not.noop();
            not.afield.toString();
            Object o = new AClassToRename();
            if (o instanceof AClassToRename) {
                ((AClassToRename) o).noop();
            }
            AClassToRename[][] arrayInsn = { { new AClassToRename() } };
            arrayInsn.getClass().getName();

            return name;
        }

        public void noop(){
        }
    }
    // should not be renamed
    public static class AClassToRenameNot {
        public final String afield = "afield";

        public AClassToRename methodRefs(AClassToRename name, AClassToRenameNot not) {
            name.noop();
            name.afield.toString();
            not.noop();
            not.afield.toString();
            return name;
        }

        public void noop(){
        }
    }

    @Test
    public void testRenameMethdod() throws IOException {
        ClassNode oldName = WeaveUtils.convertToClassNode(WeaveTestUtils.getClassBytes(OldName.class.getName()));
        ClassNode anotherClass = WeaveUtils.convertToClassNode(WeaveTestUtils.getClassBytes(
                AnotherClass.class.getName()));

        Set<Reference> beforeReferences = Reference.create(oldName);
        beforeReferences.addAll(Reference.create(anotherClass));
        beforeReferences = mergeDuplicateRefs(beforeReferences);

        ClassNode afterOldName = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        {
            oldName.accept(ReferenceUtils.getMethodRenamingVisitor(afterOldName, oldName.name, "noop", "()V",
                    "notAnOp"));
        }
        ClassNode afterAnotherClass = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        {
            anotherClass.accept(ReferenceUtils.getMethodRenamingVisitor(afterAnotherClass, oldName.name, "noop", "()V",
                    "notAnOp"));
        }
        Set<Reference> afterReferences = Reference.create(afterOldName);
        afterReferences.addAll(Reference.create(afterAnotherClass));
        afterReferences = mergeDuplicateRefs(afterReferences);

        Reference oldRef = null;
        for (Reference ref : beforeReferences) {
            if (ref.className.equals(WeaveUtils.getClassInternalName(OldName.class.getName()))) {
                oldRef = ref;
                break;
            }
        }
        Reference newRef = null;
        for (Reference ref : afterReferences) {
            if (ref.className.equals(WeaveUtils.getClassInternalName(OldName.class.getName()))) {
                newRef = ref;
                break;
            }
        }
        Assert.assertNotNull(oldRef);
        Assert.assertTrue(oldRef.getMethods().containsKey(new Method("noop", "()V")));
        Assert.assertFalse(oldRef.getMethods().containsKey(new Method("notAnOp", "()V")));

        Assert.assertNotNull(newRef);
        Assert.assertTrue(newRef.getMethods().containsKey(new Method("notAnOp", "()V")));
        Assert.assertFalse(newRef.getMethods().containsKey(new Method("noop", "()V")));
    }

    @Test
    public void testRenameField() throws IOException {
        ClassNode oldName = WeaveUtils.convertToClassNode(WeaveTestUtils.getClassBytes(OldName.class.getName()));

        ClassNode afterOldName = new ClassNode(WeaveUtils.ASM_API_LEVEL);
        {
            oldName.accept(ReferenceUtils.getFieldRenamingVisitor(afterOldName, oldName.name, "afield", "renamedField"));
        }
        Assert.assertNotNull(oldName.fields);
        FieldNode oldField = null;
        for (FieldNode fnode : oldName.fields) {
            if ("afield".equals(fnode.name)) {
                oldField = fnode;
            } else if ("renamedField".equals(fnode.name)) {
                Assert.fail("Found reference to renamed filed in old node.");
            }
        }
        Assert.assertNotNull(oldField);

        Assert.assertNotNull(afterOldName.fields);
        FieldNode newField = null;
        for (FieldNode fnode : afterOldName.fields) {
            if ("renamedField".equals(fnode.name)) {
                newField = fnode;
            } else if ("afield".equals(fnode.name)) {
                Assert.fail("Found reference to old filed in new node.");
            }
        }
        Assert.assertNotNull(newField);
    }

    public static Set<Reference> mergeDuplicateRefs(Set<Reference> references) {
        Map<String, Reference> refMap = new HashMap<>(references.size());
        for (Reference ref : references) {
            if (!refMap.containsKey(ref.className)) {
                refMap.put(ref.className, ref);
            } else {
                if (!refMap.get(ref.className).merge(ref)) {
                    throw new RuntimeException("Unable to merge reference " + refMap.get(ref.className)
                            + " with reference " + ref);
                }
            }
        }
        return new HashSet<>(refMap.values());
    }
}
