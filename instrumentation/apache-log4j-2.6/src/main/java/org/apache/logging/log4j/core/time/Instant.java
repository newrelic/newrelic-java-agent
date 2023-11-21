package org.apache.logging.log4j.core.time;

import com.newrelic.api.agent.weaver.SkipIfPresent;

@SkipIfPresent(originalName = "org.apache.logging.log4j.core.time.Instant")
public abstract class Instant {
}
