package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.nr.instrumentation.apache.camel.CamelUtil;
import com.nr.instrumentation.apache.camel.ExchangeHeadersWrapper;
import org.apache.camel.Exchange;

public class DefaultExchangeProcessor implements ExchangeProcessor {

    @Override
    public void nameTransaction(Transaction transaction, Exchange exchange) {
        String operation = CamelUtil.endpointOperation(CamelUtil.getEndpoint(exchange));
        if (operation == null || operation.isEmpty()) {
            operation = "Operation";
        }
        transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW,
                false, "ApacheCamel", operation);
    }

    @Override
    public boolean shouldStartTransaction() {
        return true;
    }

    @Override
    public void processInbound(Transaction txn, Exchange exchange) {
        if (txn != null) {
            txn.acceptDistributedTraceHeaders(TransportType.Unknown, new ExchangeHeadersWrapper(exchange));
        }
    }

}
