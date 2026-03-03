package com.amazonaws.services.lambda.runtime;

import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TransactionEvent;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_ARN_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_LENGTH;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_MESSAGE_ID;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_TIMESTAMP;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_TOPIC_ARN;
import static com.nr.instrumentation.lambda.LambdaEventsConstants.EVENT_SOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws.services.lambda"}, configName = "apm_lambda_mode_enabled.yml")
public class ApmLambdaModeTest {
    @Test
    public void testSNSEventMetadataWithAPMMode() {
        Context mockContext = createMockContext();

        SNSEvent.SNS sns = new SNSEvent.SNS();
        sns.setMessageId("12345678-1234-1234-1234-123456789012");
        sns.setTimestamp(DateTime.parse("2021-01-01T12:00:00.000Z"));
        sns.setTopicArn("arn:aws:sns:us-east-1:123456789012:my-topic");
        sns.setType("Notification");

        SNSEvent.SNSRecord record = new SNSEvent.SNSRecord();
        record.setSns(sns);
        record.setEventSubscriptionArn("arn:aws:sns:us-east-1:123456789012:my-topic:subscription-id");

        SNSEvent snsEvent = new SNSEvent();
        snsEvent.setRecords(Collections.singletonList(record));

        TestSNSHandler handler = new TestSNSHandler();
        handler.handleRequest(snsEvent, mockContext);

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        assertEquals(1, introspector.getFinishedTransactionCount());

        Collection<TransactionEvent> events = introspector.getTransactionEvents(
                "OtherTransaction/Function/SNS test-function");
        assertEquals(1, events.size());

        TransactionEvent event = events.iterator().next();
        Map<String, Object> attributes = event.getAttributes();

        // Verify SNS-specific metadata

        assertTrue("Event source ARN should be present", attributes.containsKey(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertEquals("arn:aws:sns:us-east-1:123456789012:my-topic:subscription-id", attributes.get(EVENT_SOURCE_ARN_ATTRIBUTE));
        assertTrue("Event source event type should be present", attributes.containsKey(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));
        assertEquals("sns", attributes.get(EVENT_SOURCE_EVENT_TYPE_ATTRIBUTE));

        assertTrue("Event source length should be present", attributes.containsKey(EVENT_SOURCE_LENGTH));
        assertEquals(1, attributes.get(EVENT_SOURCE_LENGTH));

        assertTrue("Message ID should be present", attributes.containsKey(EVENT_SOURCE_MESSAGE_ID));
        assertEquals("12345678-1234-1234-1234-123456789012", attributes.get(EVENT_SOURCE_MESSAGE_ID));

        assertTrue("Timestamp should be present", attributes.containsKey(EVENT_SOURCE_TIMESTAMP));
        assertEquals("2021-01-01T12:00:00.000Z", attributes.get(EVENT_SOURCE_TIMESTAMP));

        assertTrue("Topic ARN should be present", attributes.containsKey(EVENT_SOURCE_TOPIC_ARN));
        assertEquals("arn:aws:sns:us-east-1:123456789012:my-topic", attributes.get(EVENT_SOURCE_TOPIC_ARN));

        assertTrue("Type should be present", attributes.containsKey(EVENT_SOURCE_TYPE));
        assertEquals("Notification", attributes.get(EVENT_SOURCE_TYPE));
    }

    private Context createMockContext() {
        Context context = mock(Context.class);
        when(context.getInvokedFunctionArn()).thenReturn("arn:aws:lambda:us-east-1:123456789012:function:test-function");
        when(context.getFunctionVersion()).thenReturn("$LATEST");
        when(context.getFunctionName()).thenReturn("test-function");
        when(context.getAwsRequestId()).thenReturn("request-123");
        when(context.getMemoryLimitInMB()).thenReturn(512);
        when(context.getRemainingTimeInMillis()).thenReturn(30000);
        when(context.getLogGroupName()).thenReturn("/aws/lambda/test-function");
        when(context.getLogStreamName()).thenReturn("2026/01/01/[$LATEST]abc123");
        return context;
    }

    public static class TestSNSHandler implements RequestHandler<SNSEvent, Void> {
        @Override
        public Void handleRequest(SNSEvent event, Context context) {
            return null;
        }
    }
}
