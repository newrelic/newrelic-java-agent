package org.redisson.command;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.NRBiConsumer;
import com.newrelic.instrumentation.labs.redisson.RedissonUtil;
import org.redisson.api.BatchResult;
import org.redisson.api.RFuture;
import org.redisson.client.protocol.BatchCommandData;
import org.redisson.connection.NodeSource;
import org.redisson.misc.CompletableFutureWrapper;

import java.util.concurrent.ConcurrentMap;

@Weave(type= MatchType.ExactClass, originalName = "org.redisson.command.CommandBatchService")
public abstract class CommandBatchService_Instrumentation extends CommandAsyncService {
    private final ConcurrentMap<NodeSource, CommandBatchService.Entry> commands = Weaver.callOriginal();

    public RFuture<BatchResult<?>> executeAsync() {
        RFuture<BatchResult<?>> mainPromise =  Weaver.callOriginal();

        StringBuilder operations = new StringBuilder("BATCH EXECUTE : ");

        if(mainPromise instanceof CompletableFutureWrapper) {
            for (CommandBatchService.Entry entry : commands.values()) {
                for(BatchCommandData<?, ?> data: entry.getCommands()) {
                    String operation = data.getCommand().getName();
                    operations.append(operation).append("; ");
                }
            }

            String operation = operations.toString().trim();
            CompletableFutureWrapper<?> promise = (CompletableFutureWrapper<?>)mainPromise;

            Segment segment = RedissonUtil.createSegment("Redisson", "batch-operation");

            RedissonUtil.RedisAddr redisAddr = RedissonUtil.getHost(connectionManager);

            DatastoreParameters params = RedissonUtil.createDatastoreParameters(operation, redisAddr);

            NRBiConsumer<Object> listener = new NRBiConsumer<>(segment, params);
            promise.whenComplete(listener);
        }


        return mainPromise;
    }
}
