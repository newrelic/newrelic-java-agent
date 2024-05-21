package skip;

import com.newrelic.api.agent.weaver.SkipIfPresent;

/**
 * This Skip class instructs JREs 11 and up to skip this instrumentation module,
 * and use java.completable-future-jdk11 instead.
 * HttpClient was chosen for the Skip because it was introduced with Java 11.
 */
@SkipIfPresent(originalName = "java.net.http.HttpClient")
public class Skip_HttpClient {
}
