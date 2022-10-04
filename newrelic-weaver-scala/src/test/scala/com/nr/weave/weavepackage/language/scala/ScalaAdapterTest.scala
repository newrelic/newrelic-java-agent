/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.weave.weavepackage.language.scala;

import com.newrelic.agent.introspec.{InstrumentationTestConfig, InstrumentationTestRunner}
import com.newrelic.test.marker.Java17IncompatibleTest
import com.nr.weave.weavepackage.language.scala.testclasses._
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.{Assert, Test}

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("com.nr.weave.weavepackage.language.scala.weaveclasses."))
@Category(Array(classOf[Java17IncompatibleTest]))
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
  def weaveClassExtendingTraitTest() {
    val overriding: SomeTrait = new OverridingTrait()
    Assert.assertEquals("weavedoverride", overriding.traitmethod())
  }

  @Test
  def weaveChildClassWithMultipleTraitsTest() {
    val multipleOverridingWeaved: BaseTrait = new MultipleOverridingWeaved
    Assert.assertEquals("weaved-ChildWeaveTrait", multipleOverridingWeaved.traitmethod())
  }

  @Test
  def nonWeaveChildClassWithMultipleTraitsTest() {
    val multipleOverridingNonWeaved: BaseTrait = new MultipleOverridingNonWeaved
    Assert.assertEquals("weaved-ChildNonWeaveTrait", multipleOverridingNonWeaved.traitmethod())
  }

  @Test
  def weaveChildObjectWithTraitField() {
    Assert.assertEquals("weaved-other-method", new ChildFieldClass().otherMethod())
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
  trait BaseTrait {
    def traitmethod(): String
  }

  class OverridingTrait extends SomeTrait {
    override def traitmethod(): String = {
      "override"
    }
  }

  trait ChildWeaveTrait extends BaseTrait {
    override def traitmethod(): String = "ChildWeaveTrait"
  }
  trait ChildNonWeaveTrait extends BaseTrait {
    override def traitmethod(): String = "ChildNonWeaveTrait"
  }

  class MultipleOverridingNonWeaved extends ChildWeaveTrait with ChildNonWeaveTrait
  class MultipleOverridingWeaved extends ChildNonWeaveTrait with ChildWeaveTrait

  trait FieldTrait {
    val finalField: String = "field-name"
    var nonFinalField: String = "non-final-field"
    def otherMethod(): String = "other-method"
  }

  trait ChildFieldTrait  extends FieldTrait
  class ChildFieldClass extends ChildFieldTrait

}

package weaveclasses {

  import com.newrelic.api.agent.weaver.Weaver
  import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}


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

  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.testclasses.FieldTrait", `type`=ScalaMatchType.Trait)
  class FieldTraitWeave {
    def otherMethod(): String = "weaved-"+Weaver.callOriginal()
  }

  @ScalaWeave(originalName="com.nr.weave.weavepackage.language.scala.testclasses.ChildWeaveTrait", `type`=ScalaMatchType.Trait)
  class ChildTraitWeave {
    def traitmethod(): String = {
      "weaved-"+Weaver.callOriginal()
    }
  }

}
