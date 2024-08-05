/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.lambda;

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
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.async.AsyncExecuteRequest;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.utils.StringInputStream;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"software.amazon.awssdk.services.lambda"}, configName = "dt_enabled.yml")
public class DefaultLambdaAsyncClient_InstrumentationTest {

    public LambdaAsyncClient lambdaClient;
    public HttpExecuteResponse response;

    @Before
    public void setup() {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        response = asyncHttpClient.getResponse();
        lambdaClient = LambdaAsyncClient.builder()
                .httpClient(asyncHttpClient)
                .credentialsProvider(new CredProvider())
                .region(Region.US_EAST_1)
                .build();
    }

    @Test
    public void testInvokeArn() {
        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        txn();
        SpanEvent lambdaSpan = introspector.getSpanEvents().stream()
                .filter(span -> span.getName().equals("Lambda/invoke/my-function"))
                .findFirst().orElse(null);
        assertNotNull(lambdaSpan);
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", lambdaSpan.getAgentAttributes().get("cloud.resource_id"));

        Collection<TransactionTrace> transactionTraces = introspector.getTransactionTracesForTransaction(
                "OtherTransaction/Custom/software.amazon.awssdk.services.lambda.DefaultLambdaAsyncClient_InstrumentationTest/txn");
        assertEquals(1,transactionTraces.size());
        TransactionTrace transactionTrace = transactionTraces.iterator().next();
        List<TraceSegment> children = transactionTrace.getInitialTraceSegment().getChildren();
        assertEquals(1, children.size());
        TraceSegment trace = children.get(0);
        assertEquals("Lambda/invoke/my-function", trace.getName());
        assertEquals("arn:aws:lambda:us-east-1:123456789012:function:my-function", trace.getTracerAttributes().get("cloud.resource_id"));
    }

    @Trace(dispatcher = true)
    public void txn() {
        InvokeRequest request = InvokeRequest.builder()
                .functionName("arn:aws:lambda:us-east-1:123456789012:function:my-function")
                .build();
        lambdaClient.invoke(request);
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