package ch.qos.logback.classic.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.logbackclassic12.AgentUtil;

import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.recordNewRelicLogEvent;

@Weave(originalName = "ch.qos.logback.classic.spi.LoggingEvent", type = MatchType.ExactClass)
public class LoggingEvent_Instrumentation {

    transient String fqnOfLoggerClass;
    private String loggerName;
    private LoggerContext loggerContext;
    private LoggerContextVO loggerContextVO;
    private transient Level level;
    private String message;
    private transient Object[] argumentArray;
    private ThrowableProxy throwableProxy;

    /**
     * The number of milliseconds elapsed from 1/1/1970 until logging event was
     * created.
     */
    private long timeStamp;

    public LoggingEvent_Instrumentation(String fqcn, Logger logger, Level level, String message, Throwable throwable, Object[] argArray) {
        this.fqnOfLoggerClass = fqcn;
        this.loggerName = logger.getName();
        this.loggerContext = logger.getLoggerContext();
        this.loggerContextVO = loggerContext.getLoggerContextRemoteView();
        this.level = level;

        // Append New Relic linking metadata from agent to log message
        // TODO conditional checks based on config
        //  Should log be decorated? Should the decoration persist in the log file/console? Only log at certain log level?
        this.message = message + " NR-LINKING-METADATA: " + AgentUtil.getLinkingMetadataAsString();
        this.argumentArray = argArray;

        if (throwable == null) {
            throwable = extractThrowableAnRearrangeArguments(argArray);
        }

        if (throwable != null) {
            this.throwableProxy = new ThrowableProxy(throwable);
            LoggerContext lc = logger.getLoggerContext();
            if (lc.isPackagingDataEnabled()) {
                this.throwableProxy.calculatePackagingData();
            }
        }

        timeStamp = System.currentTimeMillis();

        recordNewRelicLogEvent(message, timeStamp, level, logger, fqcn, throwable);
    }

    private Throwable extractThrowableAnRearrangeArguments(Object[] argArray) {
        return Weaver.callOriginal();
    }

}
