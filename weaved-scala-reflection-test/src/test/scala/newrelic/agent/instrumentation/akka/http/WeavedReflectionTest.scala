package newrelic.agent.instrumentation.akka.http

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.server.{Directive, ImplicitPathMatcherConstruction, PathMatcher}
import newrelic.agent.instrumentation.CustomClass
import org.junit.{Assert, Test}

import scala.reflect.runtime.{universe => ru}
import ru._

class WeavedReflectionTest {

  @Test
  def testWeavedParentClass = {
    assertMethodNamesAndParams[CustomClass]("Akka PathMatcher info correct", Map(
      "<init>" -> List.empty,
      "method1" -> List("param1"),
      "method2" -> List("param1", "param2")
    ))
  }

  @Test
  def testWeavedAkkaHttpMarshal = {
    assertMethodNamesAndParams[Marshal[_]]("Marshal info correct", Map(
      "<init>" -> List("value"),
      "value" -> List(),
      "to" -> List("m", "ec"),
      "toResponseFor" -> List("request", "m", "ec")
    ))
  }

  @Test
  def testWeavedImplicitPathMatcherConstruction = {
    assertMethodNamesAndParams[ImplicitPathMatcherConstruction]("ImplicitPathMatcherConstruction info correct", Map(
      "_valueMap2PathMatcher" -> List("valueMap"),
      "_stringNameOptionReceptacle2PathMatcher" -> List("nr"),
      "_stringExtractionPair2PathMatcher" -> List("tuple"),
      "_segmentStringToPathMatcher" -> List("segment"),
      "$init$" -> List(),
      "_regex2PathMatcher" -> List("regex")
    ))
  }


  private def assertMethodNamesAndParams[T](msg: String,
                                            expectedMethodNamesAndParams: Map[String, List[String]])(
                                             implicit tag: TypeTag[T]) = {
    val methodInfo: Map[String, List[String]] =
      tag.tpe.decls
         .filter(_.isMethod)
         .map(symbol => (
           symbol.name.toString,
           symbol.asMethod.paramLists.flatten.map(_.name.toString)
         )).toMap

    Assert.assertEquals(msg, expectedMethodNamesAndParams, methodInfo)
  }
}
