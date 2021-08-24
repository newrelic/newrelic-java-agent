package newrelic.agent.instrumentation

class CustomClass extends akka.http.scaladsl.server.Directives {
  def method1(param1: String) = ???

  def method2(param1: String, param2: Int) = ???

}