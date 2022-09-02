package com.nr.ratpack.instrumentation;

import ratpack.handling.Context;
import ratpack.handling.Handler;

public class ThrowExceptionHandler implements Handler {
    @Override
    public void handle(Context ctx) throws Exception {
        throw new RuntimeException("Exception");
    }
}
