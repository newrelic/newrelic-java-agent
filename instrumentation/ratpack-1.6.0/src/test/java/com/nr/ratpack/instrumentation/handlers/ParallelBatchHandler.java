package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import ratpack.exec.Promise;
import ratpack.exec.util.ParallelBatch;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.ArrayList;
import java.util.List;

public class ParallelBatchHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        List<Promise<String>> promisesBatch = new ArrayList<>();
        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("ParallelBatch1", "yes");
            downstream.success("yes");
        }));
        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("ParallelBatch2", "yes");
            downstream.success("yes");
        }));
        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("ParallelBatch3", "yes");
            downstream.success("yes");
        }));

        ParallelBatch.of(promisesBatch).yield().then(action -> {
            ctx.next();
        });
    }
}
