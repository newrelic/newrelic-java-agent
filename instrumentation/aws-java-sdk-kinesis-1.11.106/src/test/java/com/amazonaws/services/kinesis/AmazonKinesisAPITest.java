package com.amazonaws.services.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.model.AddTagsToStreamRequest;
import com.amazonaws.services.kinesis.model.AddTagsToStreamResult;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.agent.introspec.internal.HttpServerRule;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.matchers.GreaterThan;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"com.amazonaws"}, configName = "dt_enabled.yml")
public class AmazonKinesisAPITest {

    @Rule
    public HttpServerRule server = new HttpServerRule();
    private AmazonKinesis kinesisClient;
    private AmazonKinesisAsync kinesisAsyncClient;

    @Before
    public void setup() throws URISyntaxException {
        AwsClientBuilder.EndpointConfiguration endpoint = new AwsClientBuilder.EndpointConfiguration(server.getEndPoint().toString(), "us-east-1");
        kinesisClient = AmazonKinesisClientBuilder.standard()
                .withCredentials(new CredProvider())
                .withEndpointConfiguration(endpoint)
                .build();
        kinesisAsyncClient = AmazonKinesisAsyncClientBuilder.standard()
                .withCredentials(new CredProvider())
                .withEndpointConfiguration(endpoint)
                .build();
    }

    @Test
    public void testaddCreateStream() {
        txn(() -> kinesisClient.addTagsToStream(new AddTagsToStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.addTagsToStreamAsync(new AddTagsToStreamRequest()));
//        txnAsyncWithHandler(handler -> kinesisAsyncClient.addTagsToStreamAsync(new AddTagsToStreamRequest(), handler),
//                new AsyncHandlerNoOp<AddTagsToStreamRequest, AddTagsToStreamResult>());
        assertKinesisTrace("addTagsToStream", true);
    }

    @Trace(dispatcher = true)
    public void txn(Runnable runnable) {
        runnable.run();
    }

    @Trace(dispatcher = true)
    public void txnAsync(Supplier<Future<?>> asyncSupplier) {
        try {
            asyncSupplier.get().get();
        } catch (Exception e) {
        }
    }

    @Trace(dispatcher = true)
    public <R extends AmazonWebServiceRequest, E> void txnAsyncWithHandler(Function<AsyncHandler<R, E>, Future<?>> function, AsyncHandler<R, E> handler) {
        try {
            function.apply(handler).get();
        } catch (Exception e) {
        }
    }

    private void assertKinesisTrace(String kinesisOperation, boolean assertSpan) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        final String traceName = "Kinesis/" + kinesisOperation;
        if (assertSpan) {
            // Span events fail to be generated when enough transactions are done in succession
            List<SpanEvent> kinesisSpans = introspector.getSpanEvents().stream()
                    .filter(span -> traceName.equals(span.getName()))
                    .collect(Collectors.toList());
            assertFalse(kinesisSpans.isEmpty());
            for (SpanEvent kinesisSpan: kinesisSpans) {
                assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
            }
        }
        assertTxn(kinesisOperation, introspector);
//        assertTxnAsync(kinesisOperation, "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txnAsync", introspector);
//        assertTxnAsync(kinesisOperation, "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txnAsyncWithHandler", introspector);
    }

    private void assertTxn(String kinesisOperation, Introspector introspector) {
        String transactionName = "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txn";
        final String traceName = "Kinesis/" + kinesisOperation;
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(transactionName);
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_data_streams", trace.getTracerAttributes().get("cloud.platform"));
    }

    private void assertTxnAsync(String kinesisOperation, String transactionName, Introspector introspector) {
        final String traceName = "Kinesis/" + kinesisOperation;
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(transactionName);
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(2, children.size());
        TraceSegment asyncFunctionTrace = children.get(0);
        String expectedFunctionTraceName = "com.amazonaws.services.kinesis.AmazonKinesisAsyncClient/" + kinesisOperation;
        assertEquals(expectedFunctionTraceName, asyncFunctionTrace.getName());
        TraceSegment externalTrace = children.get(1);
        assertEquals(traceName, externalTrace.getName());
        assertEquals("aws_kinesis_data_streams", externalTrace.getTracerAttributes().get("cloud.platform"));
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

    // To trigger scenarios where the async handler is called.
    private static class AsyncHandlerNoOp<REQ extends AmazonWebServiceRequest, RES> implements AsyncHandler<REQ, RES> {

        @Override
        public void onError(Exception exception) {

        }

        @Override
        public void onSuccess(REQ request, RES res) {

        }
    }
}
