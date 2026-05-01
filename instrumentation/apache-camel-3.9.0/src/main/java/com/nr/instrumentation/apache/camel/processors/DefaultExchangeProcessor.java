package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.ExtendedInboundHeaders;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.TransactionNamePriority;
import com.nr.instrumentation.apache.camel.CamelUtil;
import org.apache.camel.Exchange;

import java.util.Arrays;
import java.util.List;

public class DefaultExchangeProcessor implements ExchangeProcessor {

    @Override
    public void processInbound(Transaction transaction, Exchange exchange) {
        transaction.provideHeaders(new ExchangeInboundWrapper(exchange));
    }

    @Override
    public void nameTransaction(Transaction transaction, Exchange exchange) {
        String operation = CamelUtil.endpointOperation(exchange.getFromEndpoint());
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

    private static class ExchangeInboundWrapper extends ExtendedInboundHeaders {
        private final Exchange exchange;
        public ExchangeInboundWrapper(Exchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public HeaderType getHeaderType() {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return exchange.getIn().getHeader(name, String.class);
        }

        @Override
        public List<String> getHeaders(String name) {
            String header = exchange.getIn().getHeader(name, String.class);
            return Arrays.asList(header.split("[,;]"));
        }
    }

}
