package org.apache.log4j;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.log4j1.AgentUtil;
import org.apache.log4j.Priority;

import static com.nr.agent.instrumentation.log4j1.AgentUtil.*;

@Weave(originalName = "org.apache.log4j.Category")
public class Category_Instrumentation {

    protected void forcedLog(String fqcn, Priority level, Object message, Throwable t) {
        // Do nothing if application_logging.enabled: false
        if (isApplicationLoggingEnabled()) {
            if (isApplicationLoggingMetricsEnabled()) {
                // Generate log level metrics
                NewRelic.incrementCounter("Logging/lines");
                NewRelic.incrementCounter("Logging/lines/" + level.toString());
            }
        }
        Weaver.callOriginal();
    }

}
