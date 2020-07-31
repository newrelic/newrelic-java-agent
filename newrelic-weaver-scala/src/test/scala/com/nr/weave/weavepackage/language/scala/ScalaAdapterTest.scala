/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.weave.weavepackage.language.scala;

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner}
import com.nr.weave.weavepackage.language.scala.testclasses._
import org.junit.{Assert, Test}
import org.junit.runner.RunWith

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("com.nr.weave.weavepackage.language.scala.weaveclasses."))
class ScalaAdapterTest {

  @Test
  def basicWeavingTest() {
    val composite: OriginalClass = new OriginalClass()
    Assert.assertEquals("weavedoriginal", composite.amethod())
  }

  @Test
  def weaveObjectTest() {
    Assert.assertEquals("weavedoriginal", SomeObject.objectmethod)
  }

  @Test
  def weaveTraitTest() {
    val nonoverriding: SomeTrait = new NonOverridingTrait()
    val overriding: SomeTrait = new OverridingTrait()
    Assert.assertEquals("weavedoriginal", nonoverriding.traitmethod())
    Assert.assertEquals("weavedoverride", overriding.traitmethod())
  }

}

object ScalaAdapterTest {
}

package testclasses {

  class OriginalClass {
    def amethod(): String = {
      "original"
    }
  }

  object SomeObject {
    def objectmethod(): String = {
      "original"
    }
  }

  trait SomeTrait {
    def traitmethod(): String = {
      "original"
    }
  }

  class OverridingTrait extends SomeTrait {
    override def traitmethod(): String = {
      "override"
    }
  }
  class NonOverridingTrait extends SomeTrait {
  }

}

package weaveclasses {

  import com.newrelic.api.agent.weaver.scala.{ ScalaMatchType, ScalaWeave }
  import com.newrelic.api.agent.weaver.{ Weaver }


  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.testclasses.OriginalClass")
  class WeaveClass {
    def amethod(): String = {
      "weaved"+Weaver.callOriginal()
    }
  }

  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.testclasses.SomeObject", `type`=ScalaMatchType.Object)
  class ObjectWeave {
    def objectmethod(): String = {
      "weaved"+Weaver.callOriginal()
    }
  }

  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.testclasses.SomeTrait", `type`=ScalaMatchType.Trait)
  class TraitWeave {
    def traitmethod(): String = {
      "weaved"+Weaver.callOriginal()
    }
  }

}
