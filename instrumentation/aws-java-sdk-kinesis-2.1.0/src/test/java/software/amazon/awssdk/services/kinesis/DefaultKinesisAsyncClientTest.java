package software.amazon.awssdk.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.Trace;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.async.EmptyPublisher;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.model.AddTagsToStreamRequest;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeregisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DisableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.EnableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamConsumersRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListTagsForStreamRequest;
import software.amazon.awssdk.services.kinesis.model.MergeShardsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamRequest;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.utils.StringInputStream;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk"}, configName = "dt_enabled.yml")
public class DefaultKinesisAsyncClientTest {

    public static final String ACCOUNT_ID = "111111111111";
    public static final String STREAM_NAME = "stream-name";
    public static final String STREAM_ARN = "arn:aws:kinesis:us-east-1:111111111111:stream/stream-name";
    public static final String STREAM_PARTIAL_ARN = "arn:aws:kinesis:us-east-1:111111111111:stream/stream-name";
    public static final String CONSUMER_PARTIAL_ARN = "arn:aws:kinesis:us-east-1:111111111111:stream/stream-name/consumer/myconsumer:1";
    public static final String CONSUMER_ARN = "arn:aws:kinesis:us-east-1:111111111111:stream/stream-name/consumer/myconsumer:1";
    public KinesisAsyncClient kinesisAsyncClient;
    public HttpExecuteResponse response;

    @Before
    public void setup() {
        AsyncHttpClient mockHttpClient = new AsyncHttpClient();
        response = mockHttpClient.getResponse();
        kinesisAsyncClient = KinesisAsyncClient.builder()
                .httpClient(mockHttpClient)
                .credentialsProvider(new CredProvider())
                .region(Region.US_EAST_1)
                .build();
        AgentBridge.cloud.setAccountInfo(kinesisAsyncClient, CloudAccountInfo.AWS_ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    public void testAddTagsToStream() {
        txn(() -> kinesisAsyncClient.addTagsToStream(AddTagsToStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/addTagsToStream/stream-name", STREAM_ARN, true);
    }

    @Test
    public void testCreateStream() {
        txn(() -> kinesisAsyncClient.createStream(CreateStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/createStream/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDecreaseStreamRetentionPeriod() {
        txn(() -> kinesisAsyncClient.decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/decreaseStreamRetentionPeriod/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDeleteStream() {
        txn(() -> kinesisAsyncClient.deleteStream(DeleteStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/deleteStream/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDeregisterStreamConsumerWithStreamArn() {
        txn(() -> kinesisAsyncClient.deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().streamARN(STREAM_ARN).build()));
        assertKinesisTrace("Kinesis/deregisterStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDeregisterStreamConsumerWithConsumerArn() {
        txn(() -> kinesisAsyncClient.deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().consumerARN(CONSUMER_ARN).build()));
        assertKinesisTrace("Kinesis/deregisterStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDeregisterStreamConsumerWithPartialConsumerArn() {
        txn(() -> kinesisAsyncClient.deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().consumerARN(CONSUMER_PARTIAL_ARN).build()));
        assertKinesisTrace("Kinesis/deregisterStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDescribeLimits() {
        txn(() -> kinesisAsyncClient.describeLimits(DescribeLimitsRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeLimits", null, false);
    }

    @Test
    public void testDescribeStream() {
        txn(() -> kinesisAsyncClient.describeStream(DescribeStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/describeStream/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDescribeStreamConsumerWithStreamArn() {
        txn(() -> kinesisAsyncClient.describeStreamConsumer(DescribeStreamConsumerRequest.builder().streamARN(STREAM_ARN).build()));
        assertKinesisTrace("Kinesis/describeStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDescribeStreamConsumerWithConsumerArn() {
        txn(() -> kinesisAsyncClient.describeStreamConsumer(DescribeStreamConsumerRequest.builder().consumerARN(CONSUMER_ARN).build()));
        assertKinesisTrace("Kinesis/describeStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testDescribeStreamSummary() {
        txn(() -> kinesisAsyncClient.describeStreamSummary(DescribeStreamSummaryRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/describeStreamSummary/stream-name", STREAM_ARN, false);
    }

    @Test
    public void DisableEnhancedMonitoring() {
        txn(() -> kinesisAsyncClient.disableEnhancedMonitoring(DisableEnhancedMonitoringRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/disableEnhancedMonitoring/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testEnableEnhancedMonitoring() {
        txn(() -> kinesisAsyncClient.enableEnhancedMonitoring(EnableEnhancedMonitoringRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/enableEnhancedMonitoring/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testGetRecords() {
        txn(() -> kinesisAsyncClient.getRecords(GetRecordsRequest.builder().build()));
        assertKinesisTrace("Kinesis/getRecords", null, false);
    }

    @Test
    public void testGetShardIterator() {
        txn(() -> kinesisAsyncClient.getShardIterator(GetShardIteratorRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/getShardIterator/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testIncreaseStreamRetentionPeriod() {
        txn(() -> kinesisAsyncClient.increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/increaseStreamRetentionPeriod/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testListShards() {
        txn(() -> kinesisAsyncClient.listShards(ListShardsRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/listShards/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testListStreamConsumers() {
        ListStreamConsumersRequest request = ListStreamConsumersRequest.builder().streamARN(STREAM_ARN).build();
        txn(() -> kinesisAsyncClient.listStreamConsumers(request));
        assertKinesisTrace("Kinesis/listStreamConsumers/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testListStreams() {
        ListStreamsRequest listStreamsRequest = ListStreamsRequest.builder().build();
        txn(() -> kinesisAsyncClient.listStreams(listStreamsRequest));
        assertKinesisTrace("Kinesis/listStreams", null, false);
    }

    @Test
    public void testListTagsForStream() {
        txn(() -> kinesisAsyncClient.listTagsForStream(ListTagsForStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/listTagsForStream/stream-name", STREAM_ARN,false);
    }

    @Test
    public void testMergeShards() {
        txn(() -> kinesisAsyncClient.mergeShards(MergeShardsRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/mergeShards/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testPutRecord() {
        txn(() -> kinesisAsyncClient.putRecord(PutRecordRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/putRecord/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testPutRecords() {
        txn(() -> kinesisAsyncClient.putRecords(PutRecordsRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/putRecords/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testRegisterStreamConsumer() {
        txn(() -> kinesisAsyncClient.registerStreamConsumer(RegisterStreamConsumerRequest.builder().streamARN(STREAM_ARN).build()));
        assertKinesisTrace("Kinesis/registerStreamConsumer/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testRemoveTagsFromStream() {
        txn(() -> kinesisAsyncClient.removeTagsFromStream(RemoveTagsFromStreamRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/removeTagsFromStream/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testSplitShard() {
        txn(() -> kinesisAsyncClient.splitShard(SplitShardRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/splitShard/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testStartStreamEncryption() {
        txn(() -> kinesisAsyncClient.startStreamEncryption(StartStreamEncryptionRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/startStreamEncryption/stream-name", STREAM_ARN, false);
    }

    @Test
    public void testStopStreamEncryption() {
        txn(() -> kinesisAsyncClient.stopStreamEncryption(StopStreamEncryptionRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/stopStreamEncryption/stream-name", STREAM_ARN,false);
    }

    @Test
    public void testUpdateShardCount() {
        txn(() -> kinesisAsyncClient.updateShardCount(UpdateShardCountRequest.builder().streamName(STREAM_NAME).build()));
        assertKinesisTrace("Kinesis/updateShardCount/stream-name", STREAM_ARN, false);
    }

    @Trace(dispatcher = true)
    private void txn(Supplier<CompletableFuture<?>> supplier) {
        supplier.get();
    }

    private void assertKinesisTrace(String traceName, String expectedArn, boolean assertSpan) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        if (assertSpan) {
            // Span events fail to be generated when enough transactions are done in succession
            SpanEvent kinesisSpan = introspector.getSpanEvents().stream()
                    .filter(span -> traceName.equals(span.getName()))
                    .findFirst().orElse(null);
            assertNotNull(kinesisSpan);
            assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
            assertEquals(expectedArn, kinesisSpan.getAgentAttributes().get("cloud.resource_id"));
        }
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisAsyncClientTest/txn");
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.stream()
                .filter(t -> traceName.equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(trace);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_data_streams", trace.getTracerAttributes().get("cloud.platform"));
        assertEquals(expectedArn, trace.getTracerAttributes().get("cloud.resource_id"));
    }

    // This mock SdkAsyncHttpClient allows testing the AWS SDK without making actual HTTP requests.
    private static class AsyncHttpClient implements SdkAsyncHttpClient {
        private ExecutableHttpRequest executableMock;
        private HttpExecuteResponse response;
        private SdkHttpFullResponse httpResponse;

        public AsyncHttpClient() {
            executableMock = mock(ExecutableHttpRequest.class);
            response = mock(HttpExecuteResponse.class, Mockito.RETURNS_DEEP_STUBS);
            httpResponse = mock(SdkHttpFullResponse.class, Mockito.RETURNS_DEEP_STUBS);
            when(response.httpResponse()).thenReturn(httpResponse);
            when(httpResponse.toBuilder().content(any()).build()).thenReturn(httpResponse);
            when(httpResponse.isSuccessful()).thenReturn(true);
            AbortableInputStream inputStream = AbortableInputStream.create(new StringInputStream("Dont panic"));
            when(httpResponse.content()).thenReturn(Optional.of(inputStream));
            try {
                when(executableMock.call()).thenReturn(response);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
        }

        @Override
        public CompletableFuture<Void> execute(AsyncExecuteRequest asyncExecuteRequest) {
            asyncExecuteRequest.responseHandler().onStream(new EmptyPublisher<>());
            return new CompletableFuture<>();
        }

        public HttpExecuteResponse getResponse() {
            return response;
        }
    }

    private static class CredProvider implements AwsCredentialsProvider {
        @Override
        public AwsCredentials resolveCredentials() {
            AwsCredentials credentials = mock(AwsCredentials.class);
            when(credentials.accessKeyId()).thenReturn("accessKeyId");
            when(credentials.secretAccessKey()).thenReturn("secretAccessKey");
            return credentials;
        }
    }
}
