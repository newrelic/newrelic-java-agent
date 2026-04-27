package com.nr.instrumentation.reactor.test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AwaitMany {

    private List<String> result = null;
    private CompletableFuture<List<String>> f;

    public AwaitMany() {
        f = new CompletableFuture<>();
    }

    public List<String>  await() {
        List<String> s = Collections.emptyList();
        try {
            s = f.get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return s;
    }

    public void onError(Throwable t) {
        result.add(t.getMessage());
        f.completeExceptionally(t);
    }

    public void done(List<String> result) {
        System.out.println("AwaitMany done");
        this.result = result;
        f.complete(result);
    }

}
