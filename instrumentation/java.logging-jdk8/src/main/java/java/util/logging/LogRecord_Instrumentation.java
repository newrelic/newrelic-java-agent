package java.util.logging;

import com.newrelic.api.agent.weaver.Weave;

import static com.nr.instrumentation.jul.AgentUtil.getLinkingMetadataBlob;
import static com.nr.instrumentation.jul.AgentUtil.isApplicationLoggingEnabled;
import static com.nr.instrumentation.jul.AgentUtil.isApplicationLoggingLocalDecoratingEnabled;

@Weave(originalName = "java.util.logging.LogRecord")
public class LogRecord_Instrumentation {
    private String message;

    public String getMessage() {
        if (isApplicationLoggingEnabled() && isApplicationLoggingLocalDecoratingEnabled()) {
            // Append New Relic linking metadata from agent to log message
            return message + getLinkingMetadataBlob();
        } else {
            return message;
        }
    }

}
