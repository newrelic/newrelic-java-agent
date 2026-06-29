package com.nr.instrumentation.reactor.test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class AwaitSingle {

    private String result = null;
    private CompletableFuture<String> f;

    public AwaitSingle() {
        f = new CompletableFuture<String>();
    }

    public String getResult() {
        return result;
    }

    public String  await() {
        String s = null;
        try {
            s = f.get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return s;
    }

    public void setResult(String s) {
        result = s;
        f.complete(result);
    }
}
