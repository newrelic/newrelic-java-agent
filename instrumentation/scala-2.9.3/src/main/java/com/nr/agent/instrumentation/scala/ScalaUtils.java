/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.agent.bridge.AgentBridge;

public class ScalaUtils {

    public static final boolean scalaFuturesAsSegments = AgentBridge.getAgent().getConfig().getValue("scala_futures_as_segments.enabled", false);

    /**
     * Strip out compiler generated names:
     * <ul>
     * <li>functions named $anonfun</li>
     * <li>compiler generated classes like $1</li>
     * <li>Trailing $ chars</li>
     * </ul>
     */
    private static final Pattern compilerNames = Pattern.compile("(\\$anonfun|\\$[0-9]+)");

    /**
     * Replace consecutive $ chars with a single $
     */
    private static final Pattern singleDollar = Pattern.compile("\\$\\$+");

    /**
     * Remove trailing $ char
     */
    private static final Pattern trailingDollar = Pattern.compile("(\\$+$)");

    /**
     * Provides a metric name for compiled scala class name.
     */
    public static String nameScalaFunction(String scalaFunctionName) {
        String metricName = scalaFunctionName;
        metricName = compilerNames.matcher(metricName).replaceAll("");
        metricName = singleDollar.matcher(metricName).replaceAll("\\$");
        metricName = trailingDollar.matcher(metricName).replaceAll("");
        return metricName;
    }

    private ScalaUtils() {
    }

    public static AgentBridge.TokenAndRefCount getThreadTokenAndRefCount() {
      AgentBridge.TokenAndRefCount tokenAndRefCount = AgentBridge.activeToken.get();
      if (tokenAndRefCount == null) {
        Transaction tx = AgentBridge.getAgent().getTransaction(false);
        if (tx != null) {
          tokenAndRefCount = new AgentBridge.TokenAndRefCount(tx.getToken(), AgentBridge.getAgent().getTracedMethod(), new AtomicInteger(1));
        }
      } else {
        tokenAndRefCount.refCount.incrementAndGet();
      }
      return tokenAndRefCount;
    }

    public static void setThreadTokenAndRefCount(AgentBridge.TokenAndRefCount tokenAndRefCount) {
      if (tokenAndRefCount != null) {
        AgentBridge.activeToken.set(tokenAndRefCount);
        tokenAndRefCount.token.link();
      }
    }

    public static void clearThreadTokenAndRefCountAndTxn(AgentBridge.TokenAndRefCount tokenAndRefCount) {
      if (tokenAndRefCount != null && tokenAndRefCount.refCount.decrementAndGet() <= 0) {
        tokenAndRefCount.token.expire();
        tokenAndRefCount.token = null;
      }
    }
}
