/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.weave.weavepackage.language.scala.violations;

import java.util.List

import com.newrelic.weave.WeaveTestUtils
import com.newrelic.weave.utils.WeaveUtils
import com.newrelic.weave.violation.WeaveViolation
import com.newrelic.weave.weavepackage.{WeavePackage, WeavePackageConfig}
import com.newrelic.weave.weavepackage.language.scala.{ScalaWeaveViolation, ScalaWeaveViolationType}
import org.junit.{Assert, BeforeClass, Test}

class ScalaViolationsTest {
  import ScalaApiViolationsTest._

  // traits or objects can't be weaves
  @Test
  def onlyWeaveClasses {
    val classesToLoad: Array[String] = Array[String](
      "com.nr.weave.weavepackage.language.scala.violations.weaveclasses.WeaveTrait"
        ,"com.nr.weave.weavepackage.language.scala.violations.weaveclasses.WeaveObject"
        ,"com.nr.weave.weavepackage.language.scala.violations.weaveclasses.WeaveObject$")
    var weaveBytes: java.util.List[Array[Byte]] = new java.util.ArrayList[Array[Byte]]()
    val config: WeavePackageConfig = WeavePackageConfig.builder().name("weave_unittest").source("com.nr.weave.weavepackage.language.scala.violations").build()
    for (className <- classesToLoad) {
      val bytes: Array[Byte] = WeaveUtils.getClassBytesFromClassLoaderResource(className, classloader)
      Assert.assertNotNull("Class not found: "+className, bytes)
      weaveBytes.add(bytes)
    }
    val testPackage: WeavePackage = new WeavePackage(config, weaveBytes)

    val packageViolations: List[WeaveViolation] = testPackage.getPackageViolations()
    WeaveTestUtils.expectViolations(packageViolations
      , new ScalaWeaveViolation(WeaveUtils.getClassInternalName(classOf[weaveclasses.WeaveTrait].getName()), ScalaWeaveViolationType.CLASS_WEAVE_IS_TRAIT)
      , new ScalaWeaveViolation(WeaveUtils.getClassInternalName(weaveclasses.WeaveObject.getClass().getName()), ScalaWeaveViolationType.CLASS_WEAVE_IS_OBJECT)
    );
  }

  // util classes can be traits or objects
  @Test
  def utilTraitsAndObjectsOkay {
    val classesToLoad: Array[String] = Array[String](
      "com.nr.weave.weavepackage.language.scala.violations.weaveclasses.UtilTrait"
        ,"com.nr.weave.weavepackage.language.scala.violations.weaveclasses.UtilObject"
        ,"com.nr.weave.weavepackage.language.scala.violations.weaveclasses.UtilObject$")
    var weaveBytes: java.util.List[Array[Byte]] = new java.util.ArrayList[Array[Byte]]()
    val config: WeavePackageConfig = WeavePackageConfig.builder().name("weave_unittest").source("com.nr.weave.weavepackage.language.scala.violations").build()
    for (className <- classesToLoad) {
      val bytes: Array[Byte] = WeaveUtils.getClassBytesFromClassLoaderResource(className, classloader)
      Assert.assertNotNull("Class not found: "+className, bytes)
      weaveBytes.add(bytes)
    }
    val testPackage: WeavePackage = new WeavePackage(config, weaveBytes)

    val packageViolations: List[WeaveViolation] = testPackage.getPackageViolations()
    WeaveTestUtils.expectViolations(packageViolations);
  }

}

object ScalaApiViolationsTest {
  val classloader: ClassLoader = Thread.currentThread().getContextClassLoader()

  @BeforeClass
  def beforeClass(): Unit = {} // ensure static init runs
}


package testclasses {

  trait OriginalTrait {
    def amethod(): String = {
      "original"
    }
  }

  object OriginalObject {
    def objectMethod: String = {
      "original"
    }
  }

}

package weaveclasses {
  import com.newrelic.api.agent.weaver.{ Weave, Weaver }
  import com.newrelic.api.agent.weaver.scala.ScalaWeave


  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.violations.testclasses.OriginalTrait")
  trait WeaveTrait {
    def amethod(): String = {
      "weaved"+Weaver.callOriginal()
    }
  }

  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.violations.testclasses.OriginalObject")
  object WeaveObject {
    def objectMethod: String = {
      "weaved"+Weaver.callOriginal()
    }
  }

  object UtilObject {
    def util(): Unit = {
    }
  }

  trait UtilTrait {
    def util(): Unit = {
    }
  }
}
