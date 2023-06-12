package com.newrelic.weave.weavepackage;

import com.newrelic.weave.utils.WeaveUtils;
import org.junit.Assert;
import org.junit.Test;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;

public class ExtensionClassTemplateTest {

    @Test
    public void testIsValidExtensionNode_success() {
        Assert.assertTrue(ExtensionClassTemplate.isValidExtensionNode(ExtensionClassTemplate.DEFAULT_EXTENSION_TEMPLATE));
    }

    @Test
    public void testIsValidExtensionNode_wrongSuperClass() {
        ClassNode wrongSuper = new ClassNode();
        Assert.assertFalse(ExtensionClassTemplate.isValidExtensionNode(wrongSuper));
    }

    @Test
    public void testIsValidExtensionNode_no_getExtension_Method() throws IOException  {
        ClassNode myExtClass = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                WeaveUtils.getClassResourceName(MyExtensionClassNoMethods.class.getName()),
                MyExtensionClassNoMethods.class.getClassLoader()));
        Assert.assertFalse(ExtensionClassTemplate.isValidExtensionNode(myExtClass));
    }

    @Test
    public void testIsValidExtensionNode_no_getAndRemoveExtension_Method() throws IOException  {
        ClassNode myExtClass = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                WeaveUtils.getClassResourceName(MyExtensionClassOnlyFirstMethod.class.getName()),
                MyExtensionClassOnlyFirstMethod.class.getClassLoader()));
        Assert.assertFalse(ExtensionClassTemplate.isValidExtensionNode(myExtClass));
    }

    @Test
    public void testIsValidExtensionNode_parameterizedConstructor() throws IOException  {
        ClassNode myExtClass = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                WeaveUtils.getClassResourceName(MyExtensionClassParameterizedConstructorMethods.class.getName()),
                MyExtensionClassParameterizedConstructorMethods.class.getClassLoader()));
        Assert.assertFalse(ExtensionClassTemplate.isValidExtensionNode(myExtClass));
    }

    @Test
    public void testIsValidExtensionNode_twoConstructors() throws IOException  {
        ClassNode myExtClass = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                WeaveUtils.getClassResourceName(MyExtensionClassTwoConstructorsMethods.class.getName()),
                MyExtensionClassTwoConstructorsMethods.class.getClassLoader()));
        Assert.assertFalse(ExtensionClassTemplate.isValidExtensionNode(myExtClass));
    }

    public static class MyExtensionClassNoMethods extends ExtensionClassTemplate {}
    public static class MyExtensionClassOnlyFirstMethod extends ExtensionClassTemplate {
        public static MyExtensionClassOnlyFirstMethod getExtension(Object instance) { return null; }
    }
    public static class MyExtensionClassParameterizedConstructorMethods extends ExtensionClassTemplate {
        public MyExtensionClassParameterizedConstructorMethods(Object o) {}
        public static MyExtensionClassParameterizedConstructorMethods getExtension(Object instance) { return null; }
        public static MyExtensionClassParameterizedConstructorMethods getAndRemoveExtension(Object instance) { return null; }
    }
    public static class MyExtensionClassTwoConstructorsMethods extends ExtensionClassTemplate {
        public MyExtensionClassTwoConstructorsMethods() {}
        public MyExtensionClassTwoConstructorsMethods(Object o) {}
        public static MyExtensionClassTwoConstructorsMethods getExtension(Object instance) { return null; }
        public static MyExtensionClassTwoConstructorsMethods getAndRemoveExtension(Object instance) { return null; }
    }
}
