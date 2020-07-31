/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.weave.weavepackage;

import com.newrelic.weave.PreparedExtension;
import com.newrelic.weave.utils.WeaveUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.IdentityHashMap;

/**
 * Extending this class allows users of the Weaver to provide a customized extension class.
 * <p/>
 *
 * To use:
 * <ol>
 * <li>Create a class which <b>directly</b> extends this class</li>
 * <li>Provide only one constructor. The constructor must take no parameters.</li>
 * <li>Provide a static method with the signature <i>public static MyClass getExtension(Object o)</i></li>
 * <li>Provide a static method with the signature <i>public static MyClass getAndRemoveExtension(Object o)</i></li>
 * <li>The class may contain other fields/methods, but should not reference any classes which will not be available on
 * every classloader.</li>
 * <li>Convert the class into a {@link ClassNode} and configure it in {@link WeavePackageConfig}</li>
 * </ol>
 *
 * Examine {@link DefaultExtension} for an example.
 */
public abstract class ExtensionClassTemplate {
    public static final String GET_EXTENSION_METHOD = "getExtension";
    public static final String GET_AND_REMOVE_EXTENSION_METHOD = "getAndRemoveExtension";

    /**
     * All ExtenstionMapTemplates must provide this method.<br/>
     *
     * This is the method which will be invoked to get the extension class for a weaved class. If there is no extension
     * class it will be created.
     *
     * @param instance an instance of a weaved class.
     * @return The extension class for the weaved instance.
     */
    public static ExtensionClassTemplate getExtension(Object instance) {
        return null;
    }

    /**
     * All ExtenstionMapTemplates must provide this method.<br/>
     *
     * This is the method which will be invoked when all NewFields have been set to null or zero.
     * This will return the extension instance for the original instance and remove the extension instance from the backing map.
     *
     * @param instance an instance of a weaved class.
     * @return The extension class for the weaved instance.
     */
    public static ExtensionClassTemplate getAndRemoveExtension(Object instance) {
        return null;
    }

    /**
     * ClassNode for the {@link DefaultExtension}
     */
    public static final ClassNode DEFAULT_EXTENSION_TEMPLATE;
    static {
        try {
            DEFAULT_EXTENSION_TEMPLATE = WeaveUtils.convertToClassNode(WeaveUtils.getClassBytesFromClassLoaderResource(
                    WeaveUtils.getClassResourceName(DefaultExtension.class.getName()),
                    DefaultExtension.class.getClassLoader()));
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * This method will be generated into the implementing class at runtime. Returns true if all NewFields are reset (zero or null).
     */
    public final boolean gen_shouldResetExtensionClass() {
        return false;
    }

    /**
     * This is the {@link ExtensionClassTemplate} used by default in the weaver.<br/>
     * Since this class will be injected into every classloader, it only references bootstrap classes which are globally
     * available.
     * <p/>
     *
     * The weave to extension mapping is implemented with a synchronized WeakHashMap.
     */
    private static class DefaultExtension extends ExtensionClassTemplate {
        public static IdentityHashMap<Object, DefaultExtension> WEAVER_INSTANCE_MAP =
                new IdentityHashMap<>();

        public static synchronized DefaultExtension getAndRemoveExtension(Object instance) {
            return WEAVER_INSTANCE_MAP.remove(instance);
        }

        public static synchronized DefaultExtension getExtension(Object instance) {
            DefaultExtension result = WEAVER_INSTANCE_MAP.get(instance);
            if (result == null) {
                result = new DefaultExtension();
                WEAVER_INSTANCE_MAP.put(instance, result);
            }
            return result;
        }
    }

    /**
     * Validate that an extensionTemplate ClassNode is valid as defined by the spec in {@link ExtensionClassTemplate}.
     *
     * @param extensionNode The node to validate
     * @return true if the class node has the correct constructor and method
     */
    public static boolean isValidExtensionNode(ClassNode extensionNode) {
        final String requiredSuperClass = WeaveUtils.getClassInternalName(ExtensionClassTemplate.class.getName());
        if (!requiredSuperClass.equals(extensionNode.superName)) {
            return false;
        }
        final String getExtensionDesc = "(Ljava/lang/Object;)L" + extensionNode.name + ";";
        if (null == WeaveUtils.getMethodNode(extensionNode, GET_EXTENSION_METHOD, getExtensionDesc)) {
            return false;
        }
        final String getAndRemoveExtensionDesc = "(Ljava/lang/Object;)L" + extensionNode.name + ";";
        if (null == WeaveUtils.getMethodNode(extensionNode, GET_AND_REMOVE_EXTENSION_METHOD, getAndRemoveExtensionDesc)) {
            return false;
        }
        if (null != WeaveUtils.getMethodNode(extensionNode, PreparedExtension.RESET_CHECK_NAME, PreparedExtension.RESET_CHECK_DESC)) {
            return false;
        }
        int numConstructors = 0;
        for (MethodNode methodNode : extensionNode.methods) {
            if (WeaveUtils.INIT_NAME.equals(methodNode.name)) {
                numConstructors++;
                if (numConstructors != 1) {
                    return false;
                }
                if (!"()V".equals(methodNode.desc)) {
                    return false;
                }
            }
        }
        return true;
    }
}
