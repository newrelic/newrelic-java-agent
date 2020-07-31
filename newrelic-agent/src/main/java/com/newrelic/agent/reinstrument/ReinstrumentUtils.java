/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.reinstrument;

import com.newrelic.agent.Agent;
import com.newrelic.agent.extension.beans.Extension;
import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut;
import com.newrelic.agent.extension.beans.MethodParameters;
import com.newrelic.agent.extension.util.ExtensionConversionUtility;
import com.newrelic.agent.extension.util.MethodMapper;
import com.newrelic.agent.extension.util.MethodMatcherUtility;
import com.newrelic.agent.instrumentation.custom.ExtensionClassAndMethodMatcher;
import com.newrelic.agent.instrumentation.methodmatchers.MethodMatcher;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.util.asm.ClassStructure;
import org.objectweb.asm.commons.Method;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ReinstrumentUtils {

    public static void checkClassExistsAndRetransformClasses(ReinstrumentResult result, List<ExtensionClassAndMethodMatcher> pcs,
            Extension ext, Set<Class<?>> classesToRetransform) {
        if (!pcs.isEmpty()) {
            Set<ClassLoader> loaders = new HashSet<>();
            Map<String, Class<?>> toRetransform = new HashMap<>();
            getLoadedClassData(pcs, loaders, toRetransform);
            checkInputClasses(result, loaders, ext, toRetransform);
        }
        retransform(result, classesToRetransform);
    }

    private static void getLoadedClassData(List<ExtensionClassAndMethodMatcher> pcs, Set<ClassLoader> loaders, Map<String, Class<?>> toRetransform) {
        Class<?>[] allLoadedClasses = ServiceFactory.getCoreService().getInstrumentation().getAllLoadedClasses();
        if (allLoadedClasses != null) {
            for (Class<?> current : allLoadedClasses) {
                try {
                    if (current != null) {
                        if (current.getClassLoader() != null) {
                            loaders.add(current.getClassLoader());
                        }
                        if (shouldTransform(current, pcs)) {
                            toRetransform.put(current.getName(), current);
                        }
                    }
                } catch (Exception e) {
                    Agent.LOG.log(Level.FINE, "An unexpected exception occurred examining a class for retransformation.");
                    if (Agent.LOG.isFinestEnabled()) {
                        Agent.LOG.log(Level.FINEST, "An exception occurred examining a class for retransformation.", e);
                    }
                }
            }
        }
    }

    public static void retransform(ReinstrumentResult result, Set<Class<?>> classesToRetransform) {
        try {
            if (!classesToRetransform.isEmpty()) {
                ServiceFactory.getCoreService().getInstrumentation().retransformClasses(classesToRetransform.toArray(new Class[classesToRetransform.size()]));
                result.setRetranformedInitializedClasses(getClassNames(classesToRetransform));
            }
        } catch (Exception e) {
            handleError(result, MessageFormat.format("Attempt to retransform classes failed. Message: {0}.", e.getMessage()), e);
        }
    }

    private static Set<String> getClassNames(Set<Class<?>> classes) {
        Set<String> names = new HashSet<>();
        for (Class<?> clazz : classes) {
            names.add(clazz.getName());
        }
        return names;
    }

    private static void performRetransformations(ReinstrumentResult result, Map<String, Class<?>> toRetransform) {
        try {
            int size = toRetransform.size();
            if (size > 0) {
                ServiceFactory.getCoreService().getInstrumentation().retransformClasses(toRetransform.values().toArray(new Class[size]));
                result.setRetranformedInitializedClasses(toRetransform.keySet());
            }
        } catch (Exception e) {
            handleError(result, MessageFormat.format("Attempt to retransform classes failed. Message: {0}.", e.getMessage()), e);
        }
    }

    private static void handleError(ReinstrumentResult result, String msg, Exception e) {
        result.addErrorMessage(msg);
        Agent.LOG.log(Level.INFO, msg);
        if (Agent.LOG.isFinestEnabled()) {
            Agent.LOG.log(Level.FINEST, msg, e);
        }
    }

    protected static void handleErrorPartialInstrumentation(ReinstrumentResult result, List<Exception> msgs, String pXml) {
        if (msgs != null && msgs.size() > 0) {
            for (Exception msg : msgs) {
                result.addErrorMessage(msg.getMessage());
                Agent.LOG.log(Level.INFO, msg.getMessage());
            }
            if (Agent.LOG.isFinerEnabled()) {
                Agent.LOG.log(Level.FINER, MessageFormat.format("Errors occurred when processing this xml: {0}.", pXml));
            }
        }
    }

    protected static void handleErrorPartialInstrumentation(ReinstrumentResult result, String msg) {
        Agent.LOG.log(Level.INFO, msg);
        result.addErrorMessage(msg);
        if (Agent.LOG.isFinerEnabled()) {
            Agent.LOG.log(Level.FINER, MessageFormat.format("Errors occurred when processing this xml: {0}.", msg));
        }
    }

    protected static void checkInputClasses(ReinstrumentResult result, Set<ClassLoader> loaders, Extension ext,
            Map<String, Class<?>> toRetransform) {
        if (ext.getInstrumentation() != null) {
            List<Pointcut> pcs = ext.getInstrumentation().getPointcut();
            for (Pointcut pointcut : pcs) {
                if (pointcut.getMethodAnnotation() == null) {
                    checkForClassAndMethods(result, loaders, ExtensionConversionUtility.getClassName(pointcut), toRetransform, pointcut);
                }
            }
        }
    }

    private static void checkForClassAndMethods(ReinstrumentResult result, Set<ClassLoader> loaders, String className,
            Map<String, Class<?>> toRetransform, Pointcut pc) {
        if (className != null) {
            Class<?> current = toRetransform.get(className);
            // first check if the class is on the reload list
            if (current != null) {
                // the class is present
                checkMethodsInClass(result, ClassStructure.getClassStructure(current), pc);
            } else {
                // lets check all of the classloaders
                for (ClassLoader loader : loaders) {
                    // loader could be null if the class was loaded from the bootstrap loader
                    if (loader != null) {
                        URL resource = loader.getResource(className.replace(".", "/") + ".class");
                        if (resource != null) {
                            try {
                                checkMethodsInClass(result, ClassStructure.getClassStructure(resource), pc);
                                return;
                            } catch (IOException e) {
                                Agent.LOG.log(Level.FINER, "Error validating class " + className, e);
                            }
                        }
                    }
                }
                handleErrorPartialInstrumentation(result, MessageFormat.format(
                        "The class {0} does not match a loaded class in the JVM. Either the class has not been loaded yet or it does not exist.", className));
            }
        }
    }

    private static void checkMethodsInClass(ReinstrumentResult result, ClassStructure classStructure, Pointcut pc) {
        List<com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method> desiredMethods = pc.getMethod();
        if (desiredMethods != null) {
            Set<Method> actualMethods = classStructure.getMethods();
            for (com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method m : desiredMethods) {
                if (!foundMethod(m, actualMethods)) {
                    handleErrorPartialInstrumentation(result, MessageFormat.format(
                            "The method {0} with parameter type {1} on class {2} is not present and therefore will never match anything.",
                            m.getName(), MethodParameters.getDescriptor(m.getParameters()), ExtensionConversionUtility.getClassName(pc)));
                }
            }
        }
    }

    private static boolean foundMethod(com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method method, Set<Method> actualMethods) {
        try {
            MethodMatcher methodMatcher = MethodMatcherUtility.createMethodMatcher("BogusClass", method, new HashMap<String, MethodMapper>(), "");
            for (Method m : actualMethods) {
                if (methodMatcher.matches(MethodMatcher.UNSPECIFIED_ACCESS, m.getName(), m.getDescriptor(), MethodMatcher.UNSPECIFIED_ANNOTATIONS)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            Agent.LOG.log(Level.FINEST, ex.getMessage());
        }
        return false;
    }

    private static boolean shouldTransform(Class<?> clazz, List<ExtensionClassAndMethodMatcher> newPcs) {
        for (ExtensionClassAndMethodMatcher pc : newPcs) {
            if (pc.getClassMatcher().isMatch(clazz)) {
                return true;
            }
        }
        return false;
    }

}
