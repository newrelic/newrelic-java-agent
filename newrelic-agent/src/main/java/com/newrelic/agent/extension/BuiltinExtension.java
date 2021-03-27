/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.extension;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.service.ServiceFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * An agent extension that is bundled into the agent rather than being loaded through the external extensions directory.
 *
 * Rather than dealing with a jar file we can instead directly invoke the premain method of the builtin extension from the entry class shadowed into the agent.
 */
public class BuiltinExtension {

    private final ClassLoader classloader = AgentBridge.getAgent().getClass().getClassLoader();
    private final IAgentLogger logger;
    private final String extensionEntryClass;
    private final String extensionName;

    public BuiltinExtension(IAgentLogger logger, String extensionName, String extensionEntryClass) {
        this.logger = logger;
        this.extensionName = extensionName;
        this.extensionEntryClass = extensionEntryClass;
    }

    public ClassLoader getClassloader() {
        return classloader;
    }

    public IAgentLogger getLogger() {
        return logger;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getExtensionEntryClass() {
        return extensionEntryClass;
    }

    /**
     * Invoke the premain method for the given extension entry class.
     *
     * The extension premain method should check its config and avoid starting the extension if it is disabled or missing required resources.
     *
     * @see Instrumentation
     */
    public void invokePremainMethod() {
        logger.fine(MessageFormat.format("Loading built-in agent extension \"{0}\"", extensionName));
        try {
            Class<?> clazz = classloader.loadClass(extensionEntryClass);
            logger.log(Level.FINE, "Invoking {0}.premain method", extensionEntryClass);
            Method method = clazz.getDeclaredMethod("premain", String.class, Instrumentation.class);
            String agentArgs = "";
            method.invoke(null, agentArgs, ServiceFactory.getClassTransformerService().getExtensionInstrumentation());
        } catch (ClassNotFoundException | SecurityException e) {
            logger.log(Level.INFO, "Unable to load {0}", extensionEntryClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.INFO, "{0} has no premain method", extensionEntryClass);
            logger.log(Level.FINEST, e, e.getMessage());
        } catch (Exception e) {
            logger.log(Level.INFO, "Unable to invoke {0}.premain", extensionEntryClass);
            logger.log(Level.FINEST, e, e.getMessage());
        }
    }
}
