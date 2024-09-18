package com.amazonaws.services.kinesis;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.model.AddTagsToStreamRequest;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import com.amazonaws.services.kinesis.model.DeleteStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeLimitsRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DisableEnhancedMonitoringRequest;
import com.amazonaws.services.kinesis.model.EnableEnhancedMonitoringRequest;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListTagsForStreamRequest;
import com.amazonaws.services.kinesis.model.MergeShardsRequest;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.RemoveTagsFromStreamRequest;
import com.amazonaws.services.kinesis.model.SplitShardRequest;
import com.amazonaws.services.kinesis.model.UpdateShardCountRequest;
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

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
        txn(() -> kinesisClient.addTagsToStream(new AddTagsToStreamRequest()));
        txnAsync(() -> kinesisAsyncClient.addTagsToStreamAsync(new AddTagsToStreamRequest()));
        assertKinesisTrace("addTagsToStream", false);
    }

//    @Test
//    public void testCreateStream() {
//        txn(() -> kinesisClient.createStream(new CreateStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.createStreamAsync(new CreateStreamRequest()));
//        assertKinesisTrace("createStream", false);
//    }
//
//    @Test
//    public void testDecreaseStreamRetentionPeriod() {
//        txn(() -> kinesisClient.decreaseStreamRetentionPeriod(new DecreaseStreamRetentionPeriodRequest()));
//        txnAsync(() -> kinesisAsyncClient.decreaseStreamRetentionPeriodAsync(new DecreaseStreamRetentionPeriodRequest()));
//        assertKinesisTrace("decreaseStreamRetentionPeriod", false);
//    }
//
//    @Test
//    public void testDeleteStream() {
//        txn(() -> kinesisClient.deleteStream(new DeleteStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.deleteStreamAsync(new DeleteStreamRequest()));
//        assertKinesisTrace("deleteStream", false);
//    }
//
//    @Test
//    public void testDescribeLimits() {
//        txn(() -> kinesisClient.describeLimits(new DescribeLimitsRequest()));
//        txnAsync(() -> kinesisAsyncClient.describeLimitsAsync(new DescribeLimitsRequest()));
//        assertKinesisTrace("describeLimits", false);
//    }
//
//    @Test
//    public void testDescribeStream() {
//        txn(() -> kinesisClient.describeStream(new DescribeStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.describeStreamAsync(new DescribeStreamRequest()));
//        assertKinesisTrace("describeStream", false);
//    }
//
//    @Test
//    public void testDisableEnhancedMonitoring() {
//        txn(() -> kinesisClient.disableEnhancedMonitoring(new DisableEnhancedMonitoringRequest()));
//        txnAsync(() -> kinesisAsyncClient.disableEnhancedMonitoringAsync(new DisableEnhancedMonitoringRequest()));
//        assertKinesisTrace("disableEnhancedMonitoring", false);
//    }
//
//    @Test
//    public void testEnableEnhancedMonitoring() {
//        txn(() -> kinesisClient.enableEnhancedMonitoring(new EnableEnhancedMonitoringRequest()));
//        txnAsync(() -> kinesisAsyncClient.enableEnhancedMonitoringAsync(new EnableEnhancedMonitoringRequest()));
//        assertKinesisTrace("enableEnhancedMonitoring", false);
//    }
//
//    @Test
//    public void testGetRecords() {
//        txn(() -> kinesisClient.getRecords(new GetRecordsRequest()));
//        txnAsync(() -> kinesisAsyncClient.getRecordsAsync(new GetRecordsRequest()));
//        assertKinesisTrace("getRecords", false);
//    }
//
//    @Test
//    public void testGetShardIterator() {
//        txn(() -> kinesisClient.getShardIterator(new GetShardIteratorRequest()));
//        txnAsync(() -> kinesisAsyncClient.getShardIteratorAsync(new GetShardIteratorRequest()));
//        assertKinesisTrace("getShardIterator", false);
//    }
//
//    @Test
//    public void testIncreaseStreamRetentionPeriod() {
//        txn(() -> kinesisClient.increaseStreamRetentionPeriod(new IncreaseStreamRetentionPeriodRequest()));
//        txnAsync(() -> kinesisAsyncClient.increaseStreamRetentionPeriodAsync(new IncreaseStreamRetentionPeriodRequest()));
//        assertKinesisTrace("increaseStreamRetentionPeriod", false);
//    }
//
//    @Test
//    public void testListStreams() {
//        txn(() -> kinesisClient.listStreams(new ListStreamsRequest()));
//        txnAsync(() -> kinesisAsyncClient.listStreamsAsync(new ListStreamsRequest()));
//        assertKinesisTrace("listStreams", false);
//    }
//
//    @Test
//    public void testListTagsForStream() {
//        txn(() -> kinesisClient.listTagsForStream(new ListTagsForStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.listTagsForStreamAsync(new ListTagsForStreamRequest()));
//        assertKinesisTrace("listTagsForStream", false);
//    }
//
//    @Test
//    public void testMergeShards() {
//        txn(() -> kinesisClient.mergeShards(new MergeShardsRequest()));
//        txnAsync(() -> kinesisAsyncClient.mergeShardsAsync(new MergeShardsRequest()));
//        assertKinesisTrace("mergeShards", false);
//    }
//
//    @Test
//    public void testPutRecord() {
//        txn(() -> kinesisClient.putRecord(new PutRecordRequest()));
//        txnAsync(() -> kinesisAsyncClient.putRecordAsync(new PutRecordRequest()));
//        assertKinesisTrace("putRecord", false);
//    }
//
//    @Test
//    public void testPutRecords() {
//        txn(() -> kinesisClient.putRecords(new PutRecordsRequest()));
//        txnAsync(() -> kinesisAsyncClient.putRecordsAsync(new PutRecordsRequest()));
//        assertKinesisTrace("putRecords", false);
//    }
//
//    @Test
//    public void testRemoveTagsFromStream() {
//        txn(() -> kinesisClient.removeTagsFromStream(new RemoveTagsFromStreamRequest()));
//        txnAsync(() -> kinesisAsyncClient.removeTagsFromStreamAsync(new RemoveTagsFromStreamRequest()));
//        assertKinesisTrace("removeTagsFromStream", false);
//    }
//
//    @Test
//    public void testSplitShard() {
//        txn(() -> kinesisClient.splitShard(new SplitShardRequest()));
//        txnAsync(() -> kinesisAsyncClient.splitShardAsync(new SplitShardRequest()));
//        assertKinesisTrace("splitShard", false);
//    }
//
//    @Test
//    public void testUpdateShardCount() {
//        txn(() -> kinesisClient.updateShardCount(new UpdateShardCountRequest()));
//        txnAsync(() -> kinesisAsyncClient.updateShardCountAsync(new UpdateShardCountRequest()));
//        assertKinesisTrace("updateShardCount", false);
//    }

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
    public void txnAsync(Supplier<Future<?>> function) {
        try {
            Thread.sleep(200);
            function.get().get();
        } catch (Exception ignored) {
        }
    }

    @Trace(dispatcher = true)
    public <R extends AmazonWebServiceRequest, E> void txnAsyncWithHandler(Function<AsyncHandler<R, E>, Future<?>> function, AsyncHandler<R, E> handler) {
        try {
            function.apply(handler).get();
        } catch (Exception ignored) {
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
            assertEquals(2, kinesisSpans.size());
            for (SpanEvent kinesisSpan: kinesisSpans) {
                assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
            }
        }
        assertTxn(kinesisOperation, introspector);
        assertTxnAsync(kinesisOperation, "OtherTransaction/Custom/com.amazonaws.services.kinesis.AmazonKinesisAPITest/txnAsync", introspector);
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
