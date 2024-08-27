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
        txnAddTagsToStream();
        String txnName = "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisClientTest/txnAddTagsToStream";
        String traceName = "Kinesis/addTagsToStream";
        assertKinesisTrace(txnName, traceName);
    }

    @Trace(dispatcher = true)
    private void txnAddTagsToStream() {
        kinesisClient.addTagsToStream(AddTagsToStreamRequest.builder().build());
    }

    @Test
    public void testCreateStream() {
        txnCreateStream();
        String txnName = "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisClientTest/txnCreateStream";
        String traceName = "Kinesis/createStream";
        assertKinesisTrace(txnName, traceName);
    }

    @Trace(dispatcher = true)
    private void txnCreateStream() {
        kinesisClient.createStream(CreateStreamRequest.builder().build());
    }

    @Test
    public void testDecreaseStreamRetentionPeriod() {
        txnDecreaseStreamRetentionPeriod();
        String txnName = "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisClientTest/txnDecreaseStreamRetentionPeriod";
        String traceName = "Kinesis/decreaseStreamRetentionPeriod";
        assertKinesisTrace(txnName, traceName);
    }

    @Trace(dispatcher = true)
    private void txnDecreaseStreamRetentionPeriod() {
        kinesisClient.decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest.builder().build());
    }

    @Test
    public void testDeleteStream() {
        txnDeleteStream();
        String txnName = "OtherTransaction/Custom/software.amazon.awssdk.services.kinesis.DefaultKinesisClientTest/txnDeleteStream";
        String traceName = "Kinesis/deleteStream";
        assertKinesisTrace(txnName, traceName);
    }

    @Trace(dispatcher = true)
    private void txnDeleteStream() {
        kinesisClient.deleteStream(DeleteStreamRequest.builder().build());
    }

    private void assertKinesisTrace(String txnName, String traceName) {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        SpanEvent kinesisSpan = introspector.getSpanEvents().stream()
                .filter(span -> traceName.equals(span.getName()))
                .findFirst().orElse(null);
        assertNotNull(kinesisSpan);
        assertEquals("aws_kinesis_data_streams", kinesisSpan.getAgentAttributes().get("cloud.platform"));
        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                txnName);
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
