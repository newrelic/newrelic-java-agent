package com.nr.instrumentation.apache.camel;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Exchange_Instrumentation;
import org.apache.camel.impl.event.ExchangeCompletedEvent;
import org.apache.camel.impl.event.ExchangeCreatedEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;

final class NrCamelEventNotifier extends EventNotifierSupport {

    @Override
    public void notify(CamelEvent event) throws Exception {
        if (event instanceof ExchangeCreatedEvent) {
            onExchangeCreated((ExchangeCreatedEvent) event);
        } else if (event instanceof ExchangeCompletedEvent) {
            onExchangeCompleted((ExchangeCompletedEvent) event);
        }
    }

    @Trace(async = true)
    private static void onExchangeCompleted(CamelEvent.ExchangeCompletedEvent event) {
        Exchange exchange = event.getExchange();
        if (exchange instanceof Exchange_Instrumentation) {
            Exchange_Instrumentation exchangeInstrumentation = (Exchange_Instrumentation)exchange;
            if (exchangeInstrumentation.token != null) {
                exchangeInstrumentation.token.linkAndExpire();
                exchangeInstrumentation.token = null;
                Transaction txn = AgentBridge.getAgent().getTransaction(false);
                // For exchanges that began when camel consumed messages, inbound headers need to be processed and the transaction is named.
                // We do these actions here because the exchange contents (such as headers) are guaranteed to be available by the time it is completed.
                if (txn != null && ((Exchange_Instrumentation) exchange).consumerTxnStarted) {
                    ExchangeProcessor exchangeProcessor = CamelUtil.getExchangeProcessor(exchange.getFromEndpoint());
                    exchangeProcessor.nameTransaction(txn, exchange);
                    exchangeProcessor.processInbound(txn, exchange);
                }
            }
        }
    }

    @Trace(async = true, excludeFromTransactionTrace = true)
    private static void onExchangeCreated(ExchangeCreatedEvent event) {
        Exchange exchange = event.getExchange();
        if (exchange instanceof Exchange_Instrumentation) {
            Exchange_Instrumentation exchangeInstrumentation = (Exchange_Instrumentation)exchange;
            if (exchangeInstrumentation.token != null) {
                exchangeInstrumentation.token.link();
            } else {
                exchangeInstrumentation.token = NewRelic.getAgent().getTransaction().getToken();
            }
        }
    }

    @Override
    public boolean isEnabled(CamelEvent event) {
        return event instanceof ExchangeCreatedEvent || event instanceof ExchangeCompletedEvent;
    }

    @Override
    public String toString() {
        return "NewRelicCamelEventNotifier";
    }
}
