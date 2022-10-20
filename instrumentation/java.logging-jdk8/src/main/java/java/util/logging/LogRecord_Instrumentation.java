package java.util.logging;

import com.newrelic.api.agent.weaver.Weave;

import static com.newrelic.agent.bridge.logging.AppLoggingUtils.getLinkingMetadataBlob;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingEnabled;
import static com.newrelic.agent.bridge.logging.AppLoggingUtils.isApplicationLoggingLocalDecoratingEnabled;

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
