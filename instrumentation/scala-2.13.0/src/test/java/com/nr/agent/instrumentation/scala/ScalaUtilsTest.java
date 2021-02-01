/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.scala;

import org.junit.Assert;
import org.junit.Test;

public class ScalaUtilsTest {
    @Test
    public void testNaming() {
        // remove scalac generated anonfun and numbered anonymous classes.
        Assert.assertEquals("scala.concurrent.Future$flatMap", ScalaUtils.nameScalaFunction(
                "scala.concurrent.Future$$anonfun$flatMap$1"));
        Assert.assertEquals("scala.concurrent.Future$flatMap", ScalaUtils.nameScalaFunction(
                "scala.concurrent.Future$$anonfun$flatMap$98"));
        Assert.assertEquals("scala.concurrent.Future$flatMap", ScalaUtils.nameScalaFunction(
                "scala.concurrent.Future$$anonfun$flatMap$98"));
        // remove Lambda generated names
        Assert.assertEquals("scala.concurrent.Future$flatMap", ScalaUtils.nameScalaFunction(
                "scala.concurrent.Future$flatMap$Lambda$success"));
        Assert.assertEquals("scala.concurrent.Future$flatMap", ScalaUtils.nameScalaFunction(
                "scala.concurrent.Future$flatMap$Lambda12345$/108380"));

        // remove trailing $
        Assert.assertEquals("dollar$dollar", ScalaUtils.nameScalaFunction("dollar$$$$$$$$$dollar$"));
        Assert.assertEquals("dollar$dollar", ScalaUtils.nameScalaFunction("dollar$$$$$$$$$dollar$$$$"));
        Assert.assertEquals("dollar$dollar", ScalaUtils.nameScalaFunction("dollar$$$$$$$$$dollar$$1"));
        // remove multiple $
        Assert.assertEquals("dollar$dollar", ScalaUtils.nameScalaFunction("dollar$$$$$$$$$dollar"));
    }
}