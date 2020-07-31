/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.bridge;

import java.util.logging.Level;

/**
 * Called by the agent's weaver extension class template to generate an {@link ExtensionHolder}. The interface lives on
 * the bridge so it is accessible on all classloaders. The implementation is provided by the agent by setting
 * {@link AgentBridge#extensionHolderFactory}
 */
public interface ExtensionHolderFactory {
    /**
     * Build a new {@link ExtensionHolder}
     */
    public <T> ExtensionHolder<T> build();

    public static class NoOpExtensionHolderFactory implements ExtensionHolderFactory {
        @Override
        public <T> ExtensionHolder<T> build() {
            // this should never be invoked
            AgentBridge.getAgent().getLogger().log(Level.SEVERE,
                    "No ExtensionHolder has been set on the bridge. This is a bug in the agent. Instrumentation will not work until it is resolved.");
            throw new RuntimeException("No ExtensionHolder has been set on the bridge.");
        }
    }

}
