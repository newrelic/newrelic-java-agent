package com.amazonaws.services.kinesis;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kinesis.model.AddTagsToStreamRequest;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws"}, configName = "dt_enabled.yml")
public class AmazonKinesisAPITest {

    public static final String STREAM_NAME = "stream-name";
    @Rule
    public HttpServerRule server = new HttpServerRule();
    private AmazonKinesis kinesisClient;
    private AmazonKinesisAsync kinesisAsyncClient;

    @Before
    public void setup() throws URISyntaxException {
        String serverUriStr = server.getEndPoint().toString();
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(serverUriStr, "us-east-1");
        kinesisClient = AmazonKinesisClientBuilder.standard()
                .withCredentials(new CredProvider())
                .withEndpointConfiguration(endpoint)
                .build();
        kinesisAsyncClient = AmazonKinesisAsyncClientBuilder.standard()
                .withCredentials(new CredProvider())
                .withEndpointConfiguration(endpoint)
                .build();
    }

    // HttpServerRule is flaky so only 1 test is run

    @Test
    public void testAddTagsToStream() {
        AddTagsToStreamRequest syncRequest = new AddTagsToStreamRequest();
        syncRequest.setStreamName(STREAM_NAME);
        txn(() -> kinesisClient.addTagsToStream(syncRequest));
        txnAsyncNoStream(() -> kinesisAsyncClient.addTagsToStreamAsync(new AddTagsToStreamRequest()));
        assertKinesisTrace("addTagsToStream", STREAM_NAME, false);
    }

    @Trace(dispatcher = true)
    public void txn(Runnable runnable) {
        try {
            Thread.sleep(200);
            runnable.run();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Trace(dispatcher = true)
    public void txnAsyncNoStream(Supplier<Future<?>> function) {
        try {
            Thread.sleep(200);
            function.get().get();
        } catch (Exception ignored) {
        }
    }

    private void assertKinesisTrace(String kinesisOperation, String streamName, boolean assertSpan) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        final String traceName = "Kinesis/" + kinesisOperation;
        if (assertSpan) {
            // Span events fail to be generated when enough transactions are done in succession
            List<SpanEvent> kinesisSpans = introspector.getSpanEvents().stream()
                    .filter(span -> traceName.equals(span.getName()))
                    .collect(Collectors.toList());
            assertEquals(2, kinesisSpans.size());
            for (SpanEvent kinesisSpan: kinesisSpans) {
                assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
            }
        }
        assertTxn(kinesisOperation, streamName, "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txn",
                introspector);
        assertTxnAsyncNoStream(kinesisOperation, "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txnAsyncNoStream",
                introspector);

    }

    private void assertTxn(String kinesisOperation,String streamName, String transactionName, Introspector introspector) {
        final String traceName = "Kinesis/" + kinesisOperation + "/" + streamName;
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(transactionName);
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_data_streams", trace.getTracerAttributes().get("cloud.platform"));
    }

    private void assertTxnAsyncNoStream(String kinesisOperation, String transactionName, Introspector introspector) {
        final String asyncClientTraceName = "Java/com.amazonaws.services.kinesis.AmazonKinesisAsyncClient/" + kinesisOperation + "Async";
        final String extTraceName = "Kinesis/" + kinesisOperation;
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(transactionName);
        TransactionTrace transactionTrace = transactionTraces.iterator().next();

        List<TraceSegment> rootChildren = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, rootChildren.size());
        TraceSegment asyncClientTrace = rootChildren.get(0);
        assertEquals(asyncClientTraceName, asyncClientTrace.getName());

        List<TraceSegment> asyncFunctionTraceChildren = asyncClientTrace.getChildren();
        assertEquals(1, asyncFunctionTraceChildren.size());
        TraceSegment extTrace = asyncFunctionTraceChildren.get(0);
        assertEquals(extTraceName, extTrace.getName());
        assertEquals("aws_kinesis_data_streams", extTrace.getTracerAttributes().get("cloud.platform"));
    }

    private static class CredProvider implements AWSCredentialsProvider {
        @Override
        public AWSCredentials getCredentials() {
            AWSCredentials credentials = mock(AWSCredentials.class);
            when(credentials.getAWSAccessKeyId()).thenReturn("accessKeyId");
            when(credentials.getAWSSecretKey()).thenReturn("secretAccessKey");
            return credentials;
        }

        @Override
        public void refresh() {

        }
    }

}
