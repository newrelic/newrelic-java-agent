package software.amazon.awssdk.services.kinesis;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.SpanEvent;
import com.newrelic.agent.introspec.TraceSegment;
import com.newrelic.agent.introspec.TransactionTrace;
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
    }

    @Test
    public void testAddTagsToStream() {
        txn(() -> kinesisAsyncClient.addTagsToStream(AddTagsToStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/addTagsToStream", true);
    }

    @Test
    public void testCreateStream() {
        txn(() -> kinesisAsyncClient.createStream(CreateStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/createStream", false);
    }

    @Test
    public void testDecreaseStreamRetentionPeriod() {
        txn(() -> kinesisAsyncClient.decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder().build()));
        assertKinesisTrace("Kinesis/decreaseStreamRetentionPeriod", false);
    }

    @Test
    public void testDeleteStream() {
        txn(() -> kinesisAsyncClient.deleteStream(DeleteStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/deleteStream", false);
    }

    @Test
    public void testDeregisterStreamConsumer() {
        txn(() -> kinesisAsyncClient.deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/deregisterStreamConsumer", false);
    }

    @Test
    public void testDescribeLimits() {
        txn(() -> kinesisAsyncClient.describeLimits(DescribeLimitsRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeLimits", false);
    }

    @Test
    public void testDescribeStream() {
        txn(() -> kinesisAsyncClient.describeStream(DescribeStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStream", false);
    }

    @Test
    public void testDescribeStreamConsumer() {
        txn(() -> kinesisAsyncClient.describeStreamConsumer(DescribeStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStreamConsumer", false);
    }

    @Test
    public void testDescribeStreamSummary() {
        txn(() -> kinesisAsyncClient.describeStreamSummary(DescribeStreamSummaryRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStreamSummary", false);
    }

    @Test
    public void DisableEnhancedMonitoring() {
        txn(() -> kinesisAsyncClient.disableEnhancedMonitoring(DisableEnhancedMonitoringRequest.builder().build()));
        assertKinesisTrace("Kinesis/disableEnhancedMonitoring", false);
    }

    @Test
    public void testEnableEnhancedMonitoring() {
        txn(() -> kinesisAsyncClient.enableEnhancedMonitoring(EnableEnhancedMonitoringRequest.builder().build()));
        assertKinesisTrace("Kinesis/enableEnhancedMonitoring", false);
    }

    @Test
    public void testGetRecords() {
        txn(() -> kinesisAsyncClient.getRecords(GetRecordsRequest.builder().build()));
        assertKinesisTrace("Kinesis/getRecords", false);
    }

    @Test
    public void testGetShardIterator() {
        txn(() -> kinesisAsyncClient.getShardIterator(GetShardIteratorRequest.builder().build()));
        assertKinesisTrace("Kinesis/getShardIterator", false);
    }

    @Test
    public void testIncreaseStreamRetentionPeriod() {
        txn(() -> kinesisAsyncClient.increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest.builder().build()));
        assertKinesisTrace("Kinesis/increaseStreamRetentionPeriod", false);
    }

    @Test
    public void testListShards() {
        txn(() -> kinesisAsyncClient.listShards(ListShardsRequest.builder().build()));
        assertKinesisTrace("Kinesis/listShards", false);
    }

    @Test
    public void testListStreams() {
        txn(() -> kinesisAsyncClient.listStreams(ListStreamsRequest.builder().build()));
        assertKinesisTrace("Kinesis/listStreams", false);
    }

    @Test
    public void testListTagsForStream() {
        txn(() -> kinesisAsyncClient.listTagsForStream(ListTagsForStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/listTagsForStream", false);
    }

    @Test
    public void testMergeShards() {
        txn(() -> kinesisAsyncClient.mergeShards(MergeShardsRequest.builder().build()));
        assertKinesisTrace("Kinesis/mergeShards", false);
    }

    @Test
    public void testPutRecord() {
        txn(() -> kinesisAsyncClient.putRecord(PutRecordRequest.builder().build()));
        assertKinesisTrace("Kinesis/putRecord", false);
    }

    @Test
    public void testPutRecords() {
        txn(() -> kinesisAsyncClient.putRecords(PutRecordsRequest.builder().build()));
        assertKinesisTrace("Kinesis/putRecords", false);
    }

    @Test
    public void testRegisterStreamConsumer() {
        txn(() -> kinesisAsyncClient.registerStreamConsumer(RegisterStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/registerStreamConsumer", false);
    }

    @Test
    public void testRemoveTagsFromStream() {
        txn(() -> kinesisAsyncClient.removeTagsFromStream(RemoveTagsFromStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/removeTagsFromStream", false);
    }

    @Test
    public void testSplitShard() {
        txn(() -> kinesisAsyncClient.splitShard(SplitShardRequest.builder().build()));
        assertKinesisTrace("Kinesis/splitShard", false);
    }

    @Test
    public void testStartStreamEncryption() {
        txn(() -> kinesisAsyncClient.startStreamEncryption(StartStreamEncryptionRequest.builder().build()));
        assertKinesisTrace("Kinesis/startStreamEncryption", false);
    }

    @Test
    public void testStopStreamEncryption() {
        txn(() -> kinesisAsyncClient.stopStreamEncryption(StopStreamEncryptionRequest.builder().build()));
        assertKinesisTrace("Kinesis/stopStreamEncryption", false);
    }

    @Test
    public void testUpdateShardCount() {
        txn(() -> kinesisAsyncClient.updateShardCount(UpdateShardCountRequest.builder().build()));
        assertKinesisTrace("Kinesis/updateShardCount", false);
    }

    @Trace(dispatcher = true)
    private void txn(Supplier<CompletableFuture<?>> supplier) {
        supplier.get();
    }

    private void assertKinesisTrace(String traceName, boolean assertSpan) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        if (assertSpan) {
            // Span events fail to be generated when enough transactions are done in succession
            SpanEvent kinesisSpan = introspector.getSpanEvents().stream()
                    .filter(span -> traceName.equals(span.getName()))
                    .findFirst().orElse(null);
            assertNotNull(kinesisSpan);
            assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
        }
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisAsyncClientTest/txn");
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_data_streams", trace.getTracerAttributes().get("cloud.platform"));
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

        @Override
        public String clientName() {
            return "MockHttpClient";
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
