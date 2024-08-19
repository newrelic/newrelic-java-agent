package slick.util

import com.newrelic.api.agent.weaver.SkipIfPresent;


@SkipIfPresent(originalName = "slick.util.AsyncExecutor$PrioritizedRunnable")
class SkipPrioritizedRunnable {

}
