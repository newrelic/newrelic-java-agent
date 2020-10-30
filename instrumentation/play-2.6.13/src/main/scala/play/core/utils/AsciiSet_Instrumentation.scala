package play.core.utils

import com.newrelic.api.agent.weaver.scala.{ScalaMatchType, ScalaWeave}

// This is weaved to prevent from matching on version 2.6.12
@ScalaWeave(`type` = ScalaMatchType.Object)
class AsciiSet {
}
