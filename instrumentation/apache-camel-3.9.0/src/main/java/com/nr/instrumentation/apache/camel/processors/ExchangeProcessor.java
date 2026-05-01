package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import org.apache.camel.Exchange;

public interface ExchangeProcessor {
    void processInbound(Transaction transaction, Exchange exchange);
    void nameTransaction(Transaction transaction, Exchange exchange);
    boolean shouldStartTransaction();
}
