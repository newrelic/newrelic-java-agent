package ch.qos.logback.classic;

import ch.qos.logback.classic.Level;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.slf4j.Marker;

@Weave(originalName = "ch.qos.logback.classic.Logger", type = MatchType.ExactClass)
public abstract class Logger_Instrumentation {

    private void buildLoggingEventAndAppend(final String localFQCN, final Marker marker, final Level level, final String msg, final Object[] params,
            final Throwable t) {
        NewRelic.incrementCounter("Logging/lines");
        NewRelic.incrementCounter("Logging/lines/" + level);

//        msg = msg + getLinkingMetadataAsString();

//        LoggingEvent loggingEvent = new LoggingEvent();
//        loggingEvent.

//        Map<String, String> linkingMetadata = NewRelic.getAgent().getLinkingMetadata();

        // TODO conditional check if logs should be decorated and sent to NR, send relevant event if so
        //  check if only in transaction???
        //  check log level, only send up certain log levels by default?
        Weaver.callOriginal();
    }

    // TODO look into weaving ch.qos.logback.classic.AsyncAppender, ch.qos.logback.core.encoder.Encoder, and ch.qos.logback.core.LayoutBase.
    //  AsyncAppender could be a spot to capture New Relic trace data
    //  Encoder could be a spot to convert the logs into NR JSON
    //  LayoutBase could be a spot to format the logs
    //  Look into org.slf4j.Logger as a possible common interface weave point? probably doesn't make sense
}
