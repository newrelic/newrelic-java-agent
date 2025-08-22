package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Included to avoid loading in version 1.4.x 
 * Only difference between 1.5.x and 1.4.x is one method in AbstractCoroutine that is in 1.4.x but not 1.5.x
 * TimeSource is only 1.4.x versions
 * 
 * @author dhilpipre
 *
 */
@SkipIfPresent(originalName = "kotlinx.coroutines.TimeSource")
public class TimeSource_Instrumentation {

}
