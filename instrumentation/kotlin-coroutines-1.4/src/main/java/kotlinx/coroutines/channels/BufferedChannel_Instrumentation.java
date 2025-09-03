package kotlinx.coroutines.channels;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Included to avoid loading in version 1.7.x
 * One difference between 1.5.x and 1.7.x is the class BufferedChannel which was
 * introduced in 1.7
 *
 * @author dhilpipre
 *
 */
@SkipIfPresent(originalName = "kotlinx.coroutines.channels.BufferedChannel")
public class BufferedChannel_Instrumentation {
}
