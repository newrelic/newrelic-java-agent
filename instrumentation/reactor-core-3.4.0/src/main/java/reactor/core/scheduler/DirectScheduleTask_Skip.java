package reactor.core.scheduler;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "reactor.core.scheduler.ElasticScheduler$DirectScheduleTask")
public class DirectScheduleTask_Skip {
}
