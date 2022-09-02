package com.nr.ratpack.instrumentation.handlers;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import ratpack.exec.Promise;
import ratpack.exec.util.SerialBatch;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.ArrayList;
import java.util.List;

public class SerialBatchHandlerWithError implements Handler {
    @Override
    public void handle(Context ctx) {
        NewRelic.getAgent().getTransaction().setTransactionName(TransactionNamePriority.CUSTOM_HIGH, true, "Ratpack",
                "serialBatchWithError");

        List<Promise<String>> promisesBatch = new ArrayList<>();

        promisesBatch.add(Promise.async(downstream -> {
            NewRelic.addCustomParameter("SerialBatchHandlerWithError1", "true");
            downstream.complete();
        }));

        promisesBatch.add(Promise.sync(() -> {
            NewRelic.addCustomParameter("SerialBatchHandlerWithErrorBang", "true");
            throw new RuntimeException("ERROR");
        }));
        promisesBatch.add(Promise.sync(() -> {
            // Should never execute. Error stops execution
            NewRelic.addCustomParameter("SerialBatchHandlerWithError2", "true");
            return "testing";
        }));

        SerialBatch.of(promisesBatch).yield().onError(e -> {
            // Error above should execute this branch
            ctx.render("success");
        }).then(results -> {
            ctx.error(new RuntimeException("Should not execute"));
        });
    }
}
