package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.dynamodb_v2.DynamoDBMetricUtil;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DefaultDynamoDbAsyncClient", type = MatchType.ExactClass)
final class DefaultDynamoDbAsyncClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();
    private final URI endpoint = clientConfiguration != null ? clientConfiguration.option(SdkClientOption.ENDPOINT) : null;

    @Trace
    public CompletableFuture<ScanResponse> scan(ScanRequest scanRequest) {
        System.out.println("-> scan async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "scan", scanRequest.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<PutItemResponse> putItem(PutItemRequest request) {
        System.out.println("-> putItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "putItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<GetItemResponse> getItem(GetItemRequest request) {
        System.out.println("-> getItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "getItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }

    @Trace
    public CompletableFuture<DeleteItemResponse> deleteItem(DeleteItemRequest request) {
        System.out.println("-> deleteItem async on endpoint: " + endpoint.toString());
        DynamoDBMetricUtil.metrics(NewRelic.getAgent().getTracedMethod(), "deleteItem", request.tableName(), endpoint);
        return Weaver.callOriginal();
    }
}