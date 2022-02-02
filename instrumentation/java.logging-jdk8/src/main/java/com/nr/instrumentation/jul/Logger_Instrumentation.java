package com.nr.instrumentation.jul;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

@Weave(originalName = "java.util.logging.Logger")
public class Logger_Instrumentation {

    public Filter getFilter() {
        return Weaver.callOriginal();
    }

    public boolean isLoggable(Level level) {
        return Boolean.TRUE.equals(Weaver.callOriginal());
    }

    public void log(LogRecord record) {
        if (isLoggable(record.getLevel()) && getFilter() == null || getFilter().isLoggable(record)) {
            NewRelic.incrementCounter("Logging/lines");
            NewRelic.incrementCounter("Logging/lines/" + record.getLevel().toString());
        }
        Weaver.callOriginal();
    }

}
