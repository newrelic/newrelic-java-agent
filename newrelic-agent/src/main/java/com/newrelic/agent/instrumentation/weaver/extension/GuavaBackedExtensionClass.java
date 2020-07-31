/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.weaver.extension;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExtensionHolder;
import com.newrelic.weave.weavepackage.ExtensionClassTemplate;

import java.util.concurrent.Callable;

/**
 * This class provides the custom bytecode template which will form the extension class for weaved classes.
 *
 * @see ExtensionHolderFactoryImpl provides the agent's implementation using guava
 */
public class GuavaBackedExtensionClass extends ExtensionClassTemplate implements Callable<GuavaBackedExtensionClass> {
    private static final ExtensionHolder<GuavaBackedExtensionClass> AGENT_EXTENSION_HOLDER = AgentBridge.extensionHolderFactory.build();
    private static final Callable<GuavaBackedExtensionClass> AGENT_VALUE_LOADER = new GuavaBackedExtensionClass();

    public static GuavaBackedExtensionClass getAndRemoveExtension(Object instance) {
        return AGENT_EXTENSION_HOLDER.getAndRemoveExtension(instance);
    }

    public static GuavaBackedExtensionClass getExtension(Object instance) {
        try {
            return AGENT_EXTENSION_HOLDER.getExtension(instance, AGENT_VALUE_LOADER);
        } catch (Throwable t) {
            // This should never happen. But if it does the agent has already logged the appropriate messages.
            // We need to return something non-null here since untrapped code could be invoking this method.
            return new GuavaBackedExtensionClass();
        }
    }

    /**
     * This class may not reference non-bootstrap classes other than itself.<br/>
     * So we implement the valueLoader here instead of in a nested class.
     */
    @Override
    public GuavaBackedExtensionClass call() {
        return new GuavaBackedExtensionClass();
    }
}
