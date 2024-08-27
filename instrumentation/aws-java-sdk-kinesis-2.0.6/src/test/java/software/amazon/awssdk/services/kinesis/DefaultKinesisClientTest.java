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
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk"}, configName = "dt_enabled.yml")
public class DefaultKinesisClientTest {

    public KinesisClient kinesisClient;
    public HttpExecuteResponse response;

    @Before
    public void setup() {
        MockHttpClient mockHttpClient = new MockHttpClient();
        response = mockHttpClient.getResponse();
        kinesisClient = KinesisClient.builder()
                .httpClient(mockHttpClient)
                .credentialsProvider(new CredProvider())
                .region(Region.US_EAST_1)
                .build();
    }

    @Test
    public void testAddTagsToStream() {
        txn(() -> kinesisClient.addTagsToStream(AddTagsToStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/addTagsToStream", true);
    }

    @Test
    public void testCreateStream() {
        txn(() -> kinesisClient.createStream(CreateStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/createStream", false);
    }

    @Test
    public void testDecreaseStreamRetentionPeriod() {
        txn(() -> kinesisClient.decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder().build()));
        assertKinesisTrace("Kinesis/decreaseStreamRetentionPeriod", false);
    }

    @Test
    public void testDeleteStream() {
        txn(() -> kinesisClient.deleteStream(DeleteStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/deleteStream", false);
    }

    @Test
    public void testDeregisterStreamConsumer() {
        txn(() -> kinesisClient.deregisterStreamConsumer(DeregisterStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/deregisterStreamConsumer", false);
    }

    @Test
    public void testDescribeLimits() {
        txn(() -> kinesisClient.describeLimits(DescribeLimitsRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeLimits", false);
    }

    @Test
    public void testDescribeStream() {
        txn(() -> kinesisClient.describeStream(DescribeStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStream", false);
    }

    @Test
    public void testDescribeStreamConsumer() {
        txn(() -> kinesisClient.describeStreamConsumer(DescribeStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStreamConsumer", false);
    }

    @Test
    public void testDescribeStreamSummary() {
        txn(() -> kinesisClient.describeStreamSummary(DescribeStreamSummaryRequest.builder().build()));
        assertKinesisTrace("Kinesis/describeStreamSummary", false);
    }

    @Test
    public void DisableEnhancedMonitoring() {
        txn(() -> kinesisClient.disableEnhancedMonitoring(DisableEnhancedMonitoringRequest.builder().build()));
        assertKinesisTrace("Kinesis/disableEnhancedMonitoring", false);
    }

    @Test
    public void testEnableEnhancedMonitoring() {
        txn(() -> kinesisClient.enableEnhancedMonitoring(EnableEnhancedMonitoringRequest.builder().build()));
        assertKinesisTrace("Kinesis/enableEnhancedMonitoring", false);
    }

    @Test
    public void testGetRecords() {
        txn(() -> kinesisClient.getRecords(GetRecordsRequest.builder().build()));
        assertKinesisTrace("Kinesis/getRecords", false);
    }

    @Test
    public void testGetShardIterator() {
        txn(() -> kinesisClient.getShardIterator(GetShardIteratorRequest.builder().build()));
        assertKinesisTrace("Kinesis/getShardIterator", false);
    }

    @Test
    public void testIncreaseStreamRetentionPeriod() {
        txn(() -> kinesisClient.increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest.builder().build()));
        assertKinesisTrace("Kinesis/increaseStreamRetentionPeriod", false);
    }

    @Test
    public void testListShards() {
        txn(() -> kinesisClient.listShards(ListShardsRequest.builder().build()));
        assertKinesisTrace("Kinesis/listShards", false);
    }

    @Test
    public void testListStreams() {
        txn(() -> kinesisClient.listStreams(ListStreamsRequest.builder().build()));
        assertKinesisTrace("Kinesis/listStreams", false);
    }

    @Test
    public void testListTagsForStream() {
        txn(() -> kinesisClient.listTagsForStream(ListTagsForStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/listTagsForStream", false);
    }

    @Test
    public void testMergeShards() {
        txn(() -> kinesisClient.mergeShards(MergeShardsRequest.builder().build()));
        assertKinesisTrace("Kinesis/mergeShards", false);
    }

    @Test
    public void testPutRecord() {
        txn(() -> kinesisClient.putRecord(PutRecordRequest.builder().build()));
        assertKinesisTrace("Kinesis/putRecord", false);
    }

    @Test
    public void testPutRecords() {
        txn(() -> kinesisClient.putRecords(PutRecordsRequest.builder().build()));
        assertKinesisTrace("Kinesis/putRecords", false);
    }

    @Test
    public void testRegisterStreamConsumer() {
        txn(() -> kinesisClient.registerStreamConsumer(RegisterStreamConsumerRequest.builder().build()));
        assertKinesisTrace("Kinesis/registerStreamConsumer", false);
    }

    @Test
    public void testRemoveTagsFromStream() {
        txn(() -> kinesisClient.removeTagsFromStream(RemoveTagsFromStreamRequest.builder().build()));
        assertKinesisTrace("Kinesis/removeTagsFromStream", false);
    }

    @Test
    public void testSplitShard() {
        txn(() -> kinesisClient.splitShard(SplitShardRequest.builder().build()));
        assertKinesisTrace("Kinesis/splitShard", false);
    }

    @Test
    public void testStartStreamEncryption() {
        txn(() -> kinesisClient.startStreamEncryption(StartStreamEncryptionRequest.builder().build()));
        assertKinesisTrace("Kinesis/startStreamEncryption", false);
    }

    @Test
    public void testStopStreamEncryption() {
        txn(() -> kinesisClient.stopStreamEncryption(StopStreamEncryptionRequest.builder().build()));
        assertKinesisTrace("Kinesis/stopStreamEncryption", false);
    }

    @Test
    public void testUpdateShardCount() {
        txn(() -> kinesisClient.updateShardCount(UpdateShardCountRequest.builder().build()));
        assertKinesisTrace("Kinesis/updateShardCount", false);
    }

    @Trace(dispatcher = true)
    private void txn(Runnable runnable) {
        runnable.run();
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
                "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisClientTest/txn");
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_data_streams", trace.getTracerAttributes().get("cloud.platform"));
    }

    private static class MockHttpClient implements SdkHttpClient {
        private final ExecutableHttpRequest executableMock;
        private final HttpExecuteResponse response;
        private final SdkHttpFullResponse httpResponse;

        public MockHttpClient() {
            executableMock = mock(ExecutableHttpRequest.class);
            response = mock(HttpExecuteResponse.class, Mockito.RETURNS_DEEP_STUBS);
            httpResponse = mock(SdkHttpFullResponse.class, Mockito.RETURNS_DEEP_STUBS);
            when(response.httpResponse()).thenReturn(httpResponse);
            when(httpResponse.toBuilder().content(any()).build()).thenReturn(httpResponse);
            when(httpResponse.isSuccessful()).thenReturn(true);
            AbortableInputStream inputStream = AbortableInputStream.create(new StringInputStream("42"));
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
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest httpExecuteRequest) {
            return executableMock;
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
