package ch.qos.logback.classic.spi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.logbackclassic12.AgentUtil;
import org.slf4j.Marker;

import java.util.HashMap;
import java.util.Map;

import static com.nr.agent.instrumentation.logbackclassic12.AgentUtil.getLinkingMetadataAsMap;

@Weave(originalName = "ch.qos.logback.classic.spi.LoggingEvent", type = MatchType.ExactClass)
public class LoggingEvent_Instrumentation implements ILoggingEvent {
    /**
     * Fully qualified name of the calling Logger class. This field does not
     * survive serialization.
     * <p/>
     * <p/>
     * Note that the getCallerInformation() method relies on this fact.
     */
    transient String fqnOfLoggerClass;

    /**
     * The name of thread in which this logging event was generated.
     */
    private String threadName;

    private String loggerName;
    private LoggerContext loggerContext;
    private LoggerContextVO loggerContextVO;

    /**
     * Level of logging event.
     * <p/>
     * <p>
     * This field should not be accessed directly. You should use the
     * {@link #getLevel} method instead.
     * </p>
     */
    private transient Level level;

    private String message;

    // we gain significant space at serialization time by marking
    // formattedMessage as transient and constructing it lazily in
    // getFormattedMessage()
    transient String formattedMessage;

    private transient Object[] argumentArray;

    private ThrowableProxy throwableProxy;

    private StackTraceElement[] callerDataArray;

    private Marker marker;

    private Map<String, String> mdcPropertyMap;

    /**
     * The number of milliseconds elapsed from 1/1/1970 until logging event was
     * created.
     */
    private long timeStamp;

    public LoggingEvent_Instrumentation() {
    }

    public LoggingEvent_Instrumentation(String fqcn, Logger logger, Level level, String message, Throwable throwable, Object[] argArray) {
//                "message": "...",
//                "timestamp": 1641579045527,
//                "thread.name": "http-nio-8080-exec-7",
//                "log.level": "ERROR",
//                "logger.name": "org.springframework.samples.petclinic.system.CrashController",
//                "class.name": "org.springframework.samples.petclinic.system.CrashController",
//                "method.name": "triggerException",
//                "line.number": 41,

        HashMap<String, Object> logEventMap = new HashMap<>(getLinkingMetadataAsMap());
        logEventMap.put("message", message);
        logEventMap.put("timeStamp", timeStamp);
        logEventMap.put("log.level", level);
        logEventMap.put("logger.name", logger.getName());
        logEventMap.put("class.name", fqcn);

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
            logEventMap.put("throwable", throwable);
            this.throwableProxy = new ThrowableProxy(throwable);
            LoggerContext lc = logger.getLoggerContext();
            if (lc.isPackagingDataEnabled()) {
                this.throwableProxy.calculatePackagingData();
            }
        }

        timeStamp = System.currentTimeMillis();

        NewRelic.getAgent().getLogSender().recordCustomEvent("LogEvent", logEventMap);
    }

    private Throwable extractThrowableAnRearrangeArguments(Object[] argArray) {
        return Weaver.callOriginal();
    }

    @Override
    public String getThreadName() {
        return Weaver.callOriginal();
    }

    @Override
    public Level getLevel() {
        return Weaver.callOriginal();
    }

    @Override
    public String getMessage() {
        return Weaver.callOriginal();
    }

    @Override
    public Object[] getArgumentArray() {
        return Weaver.callOriginal();
    }

    @Override
    public String getFormattedMessage() {
        return Weaver.callOriginal();
    }

    @Override
    public String getLoggerName() {
        return Weaver.callOriginal();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return Weaver.callOriginal();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        return Weaver.callOriginal();
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return Weaver.callOriginal();
    }

    @Override
    public boolean hasCallerData() {
        return Weaver.callOriginal();
    }

    @Override
    public Marker getMarker() {
        return Weaver.callOriginal();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return Weaver.callOriginal();
    }

    /**
     * @deprecated
     */
    @Override
    public Map<String, String> getMdc() {
        return Weaver.callOriginal();
    }

    @Override
    public long getTimeStamp() {
        return Weaver.callOriginal();
    }

    @Override
    public void prepareForDeferredProcessing() {
        Weaver.callOriginal();
    }
}
