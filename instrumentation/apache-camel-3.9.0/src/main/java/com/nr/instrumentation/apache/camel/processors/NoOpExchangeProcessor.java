package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import org.apache.camel.Exchange;

public class NoOpExchangeProcessor implements ExchangeProcessor {

    @Override
    public void nameTransaction(Transaction transaction, Exchange exchange) {
        // No-Op
    }

    @Override
    public boolean shouldStartTransaction() {
        return false;
    }

    @Override
    public void processInbound(Transaction txn, Exchange exchange) {

    }
}
