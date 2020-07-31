/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.classmatchers;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.Strings;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.agent.util.asm.MissingResourceException;
import com.newrelic.agent.util.asm.Utils;

public class InterfaceMatcher extends ClassMatcher {
    private final Type type;
    private final String internalName;

    public InterfaceMatcher(String interfaceName) {
        this.type = Type.getObjectType(Strings.fixInternalClassName(interfaceName));
        this.internalName = type.getInternalName();
    }

    /**
     * Matches classes implementing the given interface, but not the interface itself.
     */
    @Override
    public boolean isMatch(ClassLoader loader, ClassReader cr) {
        if (loader == null) {
            loader = AgentBridge.getAgent().getClass().getClassLoader();
        }
        if ((cr.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
            return false;
        }

        // if the classloader can't resolve the interface nothing's going to implement it
        if (Utils.getClassResource(loader, type) == null) {
            return false;
        }

        String[] interfaces = cr.getInterfaces();
        if (isInterfaceMatch(loader, interfaces)) {
            return true;
        }

        String superName = cr.getSuperName();
        if (superName != null && !superName.equals(JAVA_LANG_OBJECT_INTERNAL_NAME)) {
            try {
                cr = Utils.readClass(loader, superName);
                return isMatch(loader, cr);
            } catch (MissingResourceException e) {
                if (Agent.LOG.isFinestEnabled()) {
                    Agent.LOG.finest(MessageFormat.format("Unable to load class {0}: {1}", superName, e));
                }
            } catch (BenignClassReadException ex) {
                // ignore
            } catch (IOException ex) {
                Agent.LOG.log(Level.FINEST, "Unable to match " + internalName, ex);
            }
        }

        return false;
    }

    private boolean isInterfaceMatch(ClassLoader loader, String[] interfaces) {
        // first try a simple name check
        if (isNameMatch(interfaces)) {
            return true;
        }
        // then try the hard core resource loading
        for (String interfaceName : interfaces) {
            if (isInterfaceMatch(loader, interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNameMatch(String[] interfaces) {
        for (String interfaceName : interfaces) {
            if (this.internalName.equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInterfaceMatch(ClassLoader loader, String interfaceName) {

        try {
            ClassReader reader = Utils.readClass(loader, interfaceName);
            return isInterfaceMatch(loader, reader.getInterfaces());
        } catch (MissingResourceException e) {
            if (Agent.LOG.isFinestEnabled()) {
                Agent.LOG.finest(MessageFormat.format("Unable to load interface {0}: {1}", interfaceName, e));
            }
            return false;
        } catch (BenignClassReadException e) {
            return false;
        } catch (Exception e) {
            String msg = MessageFormat.format("Unable to load interface {0}: {1}", interfaceName, e);
            if (Agent.LOG.isFinestEnabled()) {
                if (interfaceName.startsWith("com/newrelic/agent/") || interfaceName.startsWith("com/newrelic/weave/")) {
                    Agent.LOG.log(Level.FINEST, msg);
                } else {
                    Agent.LOG.log(Level.FINEST, msg, e);
                }
            } else {
                Agent.LOG.finer(msg);
            }
        }
        return false;
    }

    @Override
    public boolean isMatch(Class<?> clazz) {
        try {
            ClassLoader classLoader = clazz.getClassLoader();
            if (classLoader == null) {
                classLoader = AgentBridge.getAgent().getClass().getClassLoader();
            }
            if (Utils.getClassResource(classLoader, type) == null) {
                return false;
            }
            Class<?> interfaceClass = classLoader.loadClass(type.getClassName());
            if (interfaceClass.isInterface() && interfaceClass.isAssignableFrom(clazz)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public String toString() {
        return internalName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InterfaceMatcher other = (InterfaceMatcher) obj;
        if (type == null) {
            return other.type == null;
        }
        return type.equals(other.type);
    }

    @Override
    public Collection<String> getClassNames() {
        return Arrays.asList(internalName);
    }
}
