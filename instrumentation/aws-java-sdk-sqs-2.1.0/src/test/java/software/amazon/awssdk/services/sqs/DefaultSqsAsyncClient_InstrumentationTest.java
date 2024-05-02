package software.amazon.awssdk.services.sqs;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import junit.framework.TestCase;
import org.junit.runner.RunWith;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk.services.sqs" })
public class DefaultSqsAsyncClient_InstrumentationTest extends TestCase {

}