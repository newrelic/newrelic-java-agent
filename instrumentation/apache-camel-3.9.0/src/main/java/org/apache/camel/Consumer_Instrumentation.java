package org.apache.camel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.apache.camel.CamelUtil;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;

@Weave(originalName = "org.apache.camel.Consumer", type = MatchType.Interface)
public class Consumer_Instrumentation {

    public Exchange createExchange(boolean autoRelease) {
        Exchange exchange = Weaver.callOriginal();
        ExchangeProcessor exchangeProcessor = CamelUtil.getExchangeProcessor(exchange.getFromEndpoint());
        if (exchangeProcessor != null && exchangeProcessor.shouldStartTransaction()) {
            CamelUtil.startTxn(exchange, exchangeProcessor);
        }
        return exchange;
    }
}
