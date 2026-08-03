package org.redisson.command;

import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.redisson.NRBiBatchConsumer;
import com.newrelic.instrumentation.labs.redisson.NRBiConsumer;
import com.newrelic.instrumentation.labs.redisson.RedissonUtil;
import com.newrelic.instrumentation.labs.redisson.SegmentEntry;
import org.redisson.api.BatchResult;
import org.redisson.api.RFuture;
import org.redisson.client.protocol.BatchCommandData;
import org.redisson.connection.NodeSource;
import org.redisson.misc.CompletableFutureWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

@Weave(type= MatchType.ExactClass, originalName = "org.redisson.command.CommandBatchService")
public class CommandBatchService_Instrumentation {
    private final ConcurrentMap<NodeSource, CommandBatchService.Entry> commands = Weaver.callOriginal();

    public RFuture<BatchResult<?>> executeAsync() {
        RFuture<BatchResult<?>> mainPromise =  Weaver.callOriginal();
        if(mainPromise instanceof CompletableFutureWrapper) {
            List<SegmentEntry> entries = new ArrayList<>();
            for (CommandBatchService.Entry entry : commands.values()) {
                for(BatchCommandData<?, ?> data: entry.getCommands()) {
                    String operation = data.getCommand().getName();
                    String collection = data.getParams().length > 0 ? String.valueOf(data.getParams()[0]) : "other";
                    entries.add(RedissonUtil.createTracedSegmentEntry(collection, operation));
                }
            }
            CompletableFutureWrapper<?> promise = (CompletableFutureWrapper<?>)mainPromise;


            NRBiBatchConsumer<Object> listener = new NRBiBatchConsumer<>(entries);
            promise.whenComplete(listener);
        }


        return mainPromise;
    }
}
