package com.nr.instrumentation.apache.camel;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.nr.instrumentation.apache.camel.processors.DefaultExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.ExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.KafkaExchangeProcessor;
import com.nr.instrumentation.apache.camel.processors.NoOpExchangeProcessor;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Exchange_Instrumentation;
import org.apache.camel.util.StringHelper;

import java.util.HashMap;
import java.util.Map;

public class CamelUtil {

    private static final Map<String, ExchangeProcessor> EXCHANGE_PROCESSORS = loadExchangeProcessors();
    private static final DefaultExchangeProcessor DEFAULT_EXCHANGE_PROCESSOR = new DefaultExchangeProcessor();

    private static Map<String, ExchangeProcessor> loadExchangeProcessors() {
        Map<String, ExchangeProcessor> result = new HashMap<>();
        result.put("kafka", new KafkaExchangeProcessor());
        result.put("direct", new NoOpExchangeProcessor());
        result.put("direct-vm", new NoOpExchangeProcessor());
        result.put("disruptor", new NoOpExchangeProcessor());
        result.put("disruptor-vm", new NoOpExchangeProcessor());
        result.put("log", new NoOpExchangeProcessor());
        result.put("seda", new NoOpExchangeProcessor());
        result.put("vm", new NoOpExchangeProcessor());
        return result;
    }

    public static String endpointOperation(Endpoint endpoint) {
        String component = "";
        String uri = endpoint.getEndpointUri();
        String[] splitUri = StringHelper.splitOnCharacter(uri, ":", 2);
        if (splitUri[1] != null) {
            component = splitUri[0];
        }
        return component;
    }

    public static ExchangeProcessor getExchangeProcessor(Endpoint endpoint) {
        String component = endpointOperation(endpoint);
        return EXCHANGE_PROCESSORS.getOrDefault(component, DEFAULT_EXCHANGE_PROCESSOR);
    }

    @Trace(dispatcher = true)
    public static void startTxn(Exchange exchange, ExchangeProcessor exchangeProcessor) {
        if (exchange instanceof Exchange_Instrumentation) {
            Exchange_Instrumentation exchangeInstrumentation = (Exchange_Instrumentation) exchange;

            if (exchangeInstrumentation.token == null) {
                exchangeInstrumentation.token = NewRelic.getAgent().getTransaction().getToken();
            }
            exchangeInstrumentation.consumerTxnStarted = true;
        }
    }
}
