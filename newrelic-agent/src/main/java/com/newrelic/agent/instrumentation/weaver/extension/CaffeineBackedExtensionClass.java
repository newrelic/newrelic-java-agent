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

import java.util.function.Supplier;

/**
 * This class provides the custom bytecode template which will form the extension class for weaved classes.
 *
 * @see ExtensionHolderFactoryImpl provides the agent's implementation using Caffeine
 */
public class CaffeineBackedExtensionClass extends ExtensionClassTemplate implements Supplier<CaffeineBackedExtensionClass> {
    private static final ExtensionHolder<CaffeineBackedExtensionClass> AGENT_EXTENSION_HOLDER = AgentBridge.extensionHolderFactory.build();
    private static final Supplier<CaffeineBackedExtensionClass> AGENT_VALUE_LOADER = new CaffeineBackedExtensionClass();

    public static CaffeineBackedExtensionClass getAndRemoveExtension(Object instance) {
        return AGENT_EXTENSION_HOLDER.getAndRemoveExtension(instance);
    }

    public static CaffeineBackedExtensionClass getExtension(Object instance) {
        try {
            return AGENT_EXTENSION_HOLDER.getExtension(instance, AGENT_VALUE_LOADER);
        } catch (Throwable t) {
            // This should never happen. But if it does the agent has already logged the appropriate messages.
            // We need to return something non-null here since untrapped code could be invoking this method.
            return new CaffeineBackedExtensionClass();
        }
    }

    /**
     * This class may not reference non-bootstrap classes other than itself.<br/>
     * So we implement the valueLoader here instead of in a nested class.
     */
    @Override
    public CaffeineBackedExtensionClass get() {
        return new CaffeineBackedExtensionClass();
    }
}
