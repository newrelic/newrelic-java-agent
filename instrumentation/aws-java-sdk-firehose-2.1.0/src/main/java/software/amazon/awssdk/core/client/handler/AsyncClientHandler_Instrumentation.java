package software.amazon.awssdk.core.client.handler;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.core.client.handler.AsyncClientHandler", type = MatchType.Interface)
public abstract class AsyncClientHandler_Instrumentation {

    @Trace(excludeFromTransactionTrace = true)
    public abstract <InputT extends SdkRequest, OutputT extends SdkResponse> CompletableFuture<OutputT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams);

    @Trace(excludeFromTransactionTrace = true)
    public abstract <InputT extends SdkRequest, OutputT extends SdkResponse, ReturnT> CompletableFuture<ReturnT> execute(
            ClientExecutionParams<InputT, OutputT> executionParams,
            AsyncResponseTransformer<OutputT, ReturnT> asyncResponseTransformer);
}
