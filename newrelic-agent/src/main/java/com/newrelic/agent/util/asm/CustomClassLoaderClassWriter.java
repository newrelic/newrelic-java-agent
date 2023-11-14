package com.newrelic.agent.util.asm;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.util.logging.Level;

public class CustomClassLoaderClassWriter extends PatchedClassWriter {

    private final ClassLoader classLoader;
    private final ClassResolver classResolver;

    public CustomClassLoaderClassWriter(int flags, ClassLoader classLoader) {
        super(flags, classLoader);
        this.classLoader = classLoader;
        this.classResolver = ClassResolvers.getClassLoaderResolver(classLoader == null ?
                AgentBridge.getAgent().getClass().getClassLoader() : classLoader);
    }

    private Class loadClass(String type) throws ClassNotFoundException {
        Class result = null;
        try {
            // try the custom classloader first
            result = classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            Agent.LOG.log(Level.FINEST, "class not found in custom classloader: "+type);
            try {
                // now try the base classloader
                result = this.getClass().getClassLoader().loadClass(type);
            } catch (ClassNotFoundException e2) {
                Agent.LOG.log(Level.FINEST, "class not found in base classloader: "+type);
                try {
                    // if all else fails, let's try the hard way
                    // this case exists because of continued TypeNotPresentExceptions when instrumenting Scala
                    ClassReader classReader = getClassReader(type);
                    if (classReader != null) {
                        result = classReader.getClass();
                    }
                } catch (IOException ioe) {
                    Agent.LOG.log(Level.FINEST, ioe.toString(), ioe);
                    throw new ClassNotFoundException("Could not find class via ClassReader: "+type);
                }
            }
        }

        return result;
    }

    protected String getCommonSuperClass(String type1, String type2) {
        Class class1;
        try {
            class1 = loadClass(type1);
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException(type1, e);
        }

        Class class2;
        try {
            class2 = loadClass(type2);
        } catch (ClassNotFoundException e) {
            throw new TypeNotPresentException(type2, e);
        }

        if (class1 == null || class2 == null) {
            return JAVA_LANG_OBJECT;
        }

        if (class1.isAssignableFrom(class2)) {
            return type1;
        } else if (class2.isAssignableFrom(class1)) {
            return type2;
        } else if (!class1.isInterface() && !class2.isInterface()) {
            do {
                class1 = class1.getSuperclass();
            } while(!class1.isAssignableFrom(class2));

            return class1.getName().replace('.', '/');
        } else {
            return "java/lang/Object";
        }
    }

}
