/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.instrumentation.context;

import com.newrelic.agent.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.asm.BenignClassReadException;
import com.newrelic.agent.util.asm.Utils;
import com.newrelic.weave.UtilityClass;
import java.util.Collection;
import java.util.logging.Level;
import org.objectweb.asm.ClassReader;

public class InstrumentationContextClassMatcherHelper {

  public boolean isMatch(Collection<ClassMatchVisitorFactory> matchers, Class<?> clazz) {
    if (clazz.isArray()) {
      // this one is funny. Apparently class arrays get cached. I assume we'll also see the
      // regular class
      // and can ignore the array. The call to ClassLoader.getResource() for the array class will
      // fail.
      return false;
    }
    String className = clazz.getName();
    if (className.startsWith("com.newrelic.api.agent") || className
        .startsWith("com.newrelic.agent.bridge") ||
        className.startsWith("com.newrelic.weave.") || className.startsWith("com.nr.agent") ||
        className.endsWith("_nr_ext") || className.endsWith("_nr_anon")) {
      return false;
    }

    ClassLoader loader = clazz.getClassLoader();
    if (loader == null) {
      loader = AgentBridge.getAgent().getClass().getClassLoader();
    }
    InstrumentationContext context = new InstrumentationContext(null, null, null);

    try {
      ClassReader reader = Utils.readClass(clazz);
      context.match(loader, clazz, reader, matchers);
      return !context.getMatches().isEmpty();
    } catch (BenignClassReadException ex) {
      return false;
    } catch (Exception ex) {
      // we often can't load our classes or lambdas because they're generated. Don't log this stuff.
      if (isMissingResourceExpected(className)) {
        return false;
      }
      if (clazz.isAnnotationPresent(UtilityClass.class)) {
        return false;
      }
      Agent.LOG.log(Level.FINER, "Unable to read {0}", className);
      Agent.LOG.log(Level.FINEST, ex, "Unable to read {0}", className);
      return false;
    }
  }

  // check if the class trying to be matched is a generated class, lambda class or a new relic
  // class.
  // we can't get the resource of a generated class because generated classes don't have resources.
  public boolean isMissingResourceExpected(String className) {
    // new relic classes
    if (className.startsWith("com.newrelic") || className.startsWith("weave.") || className.startsWith("com.nr.instrumentation")) {
      return true;
    }

    // lambda classes
    if (className.contains("$$Lambda$") || className.contains("LambdaForm$")) {
      return true;
    }

    // generated classes
    if (className.contains("GeneratedConstructorAccessor") ||
            className.contains("GeneratedMethodAccessor") ||
            className.contains("GeneratedSerializationConstructor") ||
            className.contains("BoundMethodHandle$") ||
            className.startsWith("jdk.jfr.internal.handlers.EventHandler")) {
      return true;
    }
    return false;
  }
}
