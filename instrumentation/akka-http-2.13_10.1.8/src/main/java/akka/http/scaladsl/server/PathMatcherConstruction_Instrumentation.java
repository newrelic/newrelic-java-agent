package akka.http.scaladsl.server;

import akka.http.scaladsl.server.util.Tuple$;
import com.agent.instrumentation.akka.http.PathMatcherScalaUtils;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import scala.Tuple1;
import scala.util.matching.Regex;

@Weave(type = MatchType.BaseClass, originalName = "akka.http.scaladsl.server.ImplicitPathMatcherConstruction")
public class PathMatcherConstruction_Instrumentation {
  public PathMatcher<Tuple1<String>> _regex2PathMatcher(Regex regex) {
    PathMatcher<Tuple1<String>> orig = Weaver.callOriginal();
    return  PathMatcherScalaUtils.pathMatcherWrapper(PathMatcherScalaUtils.emptyFunction1(),
                                                     PathMatcherScalaUtils.appendRegex(regex), orig,
                                                     Tuple$.MODULE$.<String>forTuple1());
  }
}