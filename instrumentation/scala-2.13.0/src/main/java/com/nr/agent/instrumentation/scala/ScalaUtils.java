/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import com.newrelic.agent.bridge.AgentBridge;

import java.util.regex.Pattern;

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
     * Remove any Lambda generated names
     */
    private static final Pattern lambdaNames = Pattern.compile("(\\$Lambda.*$)");

    /**
     * Provides a metric name for compiled scala class name.
     */
    public static String nameScalaFunction(String scalaFunctionName) {
        String metricName = scalaFunctionName;
        metricName = compilerNames.matcher(metricName).replaceAll("");
        metricName = singleDollar.matcher(metricName).replaceAll("\\$");
        metricName = trailingDollar.matcher(metricName).replaceAll("");
        metricName = lambdaNames.matcher(metricName).replaceAll("");
        return metricName;
    }

    private ScalaUtils() {
    }
}
