package org.apache.camel;

import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.apache.camel.CamelUtil;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;

@Weave(type = MatchType.Interface, originalName = "org.apache.camel.Processor")
public class Processor_Instrumentation {

    @Trace(async = true, excludeFromTransactionTrace = true)
    public void process(Exchange exchange) {
        if (exchange instanceof Exchange_Instrumentation) {
            Exchange_Instrumentation exchangeInstrumentation = (Exchange_Instrumentation) exchange;
            if (exchangeInstrumentation.token != null) {
                exchangeInstrumentation.token.link();
            } else if (exchangeInstrumentation.fromConsumer) {
                ExchangeProcessor exchangeProcessor = CamelUtil.getExchangeProcessor(CamelUtil.getEndpoint(exchange));
                CamelUtil.startTxn(exchange, exchangeProcessor);
            }
        }


        Weaver.callOriginal();
    }
}
