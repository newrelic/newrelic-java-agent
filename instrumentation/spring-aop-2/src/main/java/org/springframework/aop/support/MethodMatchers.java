/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.springframework.aop.support;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.springframework.aop.MethodMatcher;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave
public abstract class MethodMatchers {
    public static boolean matches(MethodMatcher mm, Method method, Class<?> targetClass, boolean hasIntroductions) {
        AgentBridge.getAgent().getLogger().log(Level.FINER, "spring-aop entry: {0}.{1}", targetClass, method);
        boolean matched = Weaver.callOriginal();
        if (matched && targetClass != null && !targetClass.isInterface()) { // Probably never an interface...
            try {
                Method concreteMethod = targetClass.getDeclaredMethod(method.getName(), method.getParameterTypes());

                Class<?> declaringClass = concreteMethod.getDeclaringClass();
                String className = declaringClass.getName();
                boolean coreClass = className == null || className.startsWith("java") || className.startsWith("javax")
                        || className.startsWith("sun") || className.startsWith("com.sun");
                if (!coreClass) {
                    AgentBridge.instrumentation.instrument(concreteMethod, "Spring/Java/");
                }
            } catch (NoSuchMethodException e) {

            } catch (Exception e) {
                NewRelic.getAgent().getLogger().log(Level.FINER, "Spring AOP Instrumentation Error: {0}", e);
            }
            return matched;
        } else {
            return matched;
        }
    }
}