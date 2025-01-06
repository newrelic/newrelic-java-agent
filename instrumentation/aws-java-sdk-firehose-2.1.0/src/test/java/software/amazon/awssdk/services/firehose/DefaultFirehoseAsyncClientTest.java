package software.amazon.awssdk.services.firehose;

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
import software.amazon.awssdk.services.firehose.model.CreateDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DeleteDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DescribeDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.ListDeliveryStreamsRequest;
import software.amazon.awssdk.services.firehose.model.ListTagsForDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.StartDeliveryStreamEncryptionRequest;
import software.amazon.awssdk.services.firehose.model.StopDeliveryStreamEncryptionRequest;
import software.amazon.awssdk.services.firehose.model.TagDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.UntagDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.UpdateDestinationRequest;
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
public class DefaultFirehoseAsyncClientTest {

    public static final String ACCOUNT_ID = "111111111111";
    public static final String DELIVERY_STREAM_NAME = "stream-name";
    public static final String DELIVERY_STREAM_ARN = "arn:aws:firehose:us-east-1:111111111111:deliverystream/stream-name";
    public FirehoseAsyncClient firehoseAsyncClient;
    public HttpExecuteResponse response;

    @Before
    public void setup() {
        AsyncHttpClient mockHttpClient = new AsyncHttpClient();
        response = mockHttpClient.getResponse();
        firehoseAsyncClient = FirehoseAsyncClient.builder()
                .httpClient(mockHttpClient)
                .credentialsProvider(new CredProvider())
                .region(Region.US_EAST_1)
                .build();
        AgentBridge.cloud.setAccountInfo(firehoseAsyncClient, CloudAccountInfo.AWS_ACCOUNT_ID, ACCOUNT_ID);
    }

    @Test
    public void testCreateDeliveryStream() {
        txn(() -> firehoseAsyncClient.createDeliveryStream(CreateDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/createDeliveryStream/stream-name", DELIVERY_STREAM_ARN, true);
    }

    @Test
    public void testDeleteDeliveryStream() {
        txn(() -> firehoseAsyncClient.deleteDeliveryStream(DeleteDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/deleteDeliveryStream/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testDescribeDeliveryStream() {
        txn(() -> firehoseAsyncClient.describeDeliveryStream(DescribeDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/describeDeliveryStream/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testListDeliveryStreams() {
        txn(() -> firehoseAsyncClient.listDeliveryStreams(ListDeliveryStreamsRequest.builder().build()));
        assertFirehoseTrace("Firehose/listDeliveryStreams", null, false);
    }

    @Test
    public void testListTagsForDeliveryStream() {
        txn(() -> firehoseAsyncClient.listTagsForDeliveryStream(ListTagsForDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/listTagsForDeliveryStream/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testPutRecord() {
        txn(() -> firehoseAsyncClient.putRecord(PutRecordRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/putRecord/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testPutRecordBatch() {
        txn(() -> firehoseAsyncClient.putRecordBatch(PutRecordBatchRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/putRecordBatch/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testStartDeliveryStreamEncryption() {
        txn(() -> firehoseAsyncClient.startDeliveryStreamEncryption(StartDeliveryStreamEncryptionRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/startDeliveryStreamEncryption/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testStopDeliveryStreamEncryption() {
        txn(() -> firehoseAsyncClient.stopDeliveryStreamEncryption(StopDeliveryStreamEncryptionRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/stopDeliveryStreamEncryption/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testTagDeliveryStream() {
        txn(() -> firehoseAsyncClient.tagDeliveryStream(TagDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/tagDeliveryStream/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testUnTagDeliveryStream() {
        txn(() -> firehoseAsyncClient.untagDeliveryStream(UntagDeliveryStreamRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/untagDeliveryStream/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Test
    public void testUpdateDestination() {
        txn(() -> firehoseAsyncClient.updateDestination(UpdateDestinationRequest.builder().deliveryStreamName(DELIVERY_STREAM_NAME).build()));
        assertFirehoseTrace("Firehose/updateDestination/stream-name", DELIVERY_STREAM_ARN, false);
    }

    @Trace(dispatcher = true)
    private void txn(Supplier<CompletableFuture<?>> supplier) {
        supplier.get();
    }

    private void assertFirehoseTrace(String traceName, String expectedArn, boolean assertSpan) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        if (assertSpan) {
            // Span events fail to be generated when enough transactions are done in succession
            SpanEvent kinesisSpan = introspector.getSpanEvents().stream()
                    .filter(span -> traceName.equals(span.getName()))
                    .findFirst().orElse(null);
            assertNotNull(kinesisSpan);
            assertEquals("aws_kinesis_delivery_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
            assertEquals(expectedArn, kinesisSpan.getAgentAttributes().get("cloud.resource_id"));
        }
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                "OtherTransaction/Custom/software.amazon.awssdk.services.firehose.DefaultFirehoseAsyncClientTest/txn");
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.stream()
                .filter(t -> traceName.equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(trace);
        assertEquals(traceName, trace.getName());
        assertEquals("aws_kinesis_delivery_streams", trace.getTracerAttributes().get("cloud.platform"));
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
