package kotlinx.coroutines;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * Included to avoid loading in lower versions
 * 
 * @author dhilpipre
 *
 */

@SkipIfPresent(originalName = "kotlinx.coroutines.CoroutineExceptionHandlerImplKt")
public class CoroutineExceptionHandlerImplKt_Instrumentation {

}
