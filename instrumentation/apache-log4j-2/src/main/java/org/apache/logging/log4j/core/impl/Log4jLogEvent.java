//package org.apache.logging.log4j.core.impl;
//
//import com.newrelic.api.agent.weaver.MatchType;
//import com.newrelic.api.agent.weaver.Weave;
//import com.newrelic.api.agent.weaver.WeaveAllConstructors;
//import org.apache.logging.log4j.core.LogEvent;
//import org.apache.logging.log4j.message.Message;
//
//@Weave(originalName = "org.apache.logging.log4j.core.impl.Log4jLogEvent", type = MatchType.ExactClass)
//public class Log4jLogEvent  { //implements LogEvent
//
//    private Message message;
//
//    @WeaveAllConstructors
//    Log4jLogEvent() {
////        message = message.;
//    }
//
//
//    // might need to copy NewRelicContextDataProvider to instrumentation and find a way to activate it
//    // https://github.com/newrelic/java-log-extensions/blob/main/log4j2/src/main/java/com/newrelic/logging/log4j2/NewRelicContextDataProvider.java
//
//
//    final LogEvent event = Log4jLogEvent.newBuilder()
//
//            // checkout org.apache.logging.log4j.core.layout.PatternLayout.encode
//            // org.apache.logging.log4j.core.layout.TextEncoderHelper.encodeText
//
//}
