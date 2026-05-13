package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import com.newrelic.api.agent.TransportType;
import com.nr.instrumentation.apache.camel.CamelUtil;
import com.nr.instrumentation.apache.camel.ExchangeHeadersWrapper;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.StringHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class KafkaExchangeProcessor implements ExchangeProcessor {

    private static final String TOPIC = "kafka.TOPIC";
    public static final String CATEGORY = "Message";
    public static final boolean DT_CONSUMER_BATCH_ENABLED = NewRelic.getAgent().getConfig()
            .getValue("kafka.spans.distributed_trace.consume_many.enabled", false);
    public static String LIBRARY_NAME = "ApacheCamelKafka";

    @Override
    public void nameTransaction(Transaction transaction, Exchange exchange) {
        if (transaction == null) {
            return;
        }
        if (exchange.getIn().getBody() instanceof List) {
            for (Object item : exchange.getIn().getBody(List.class)) {
                if (item instanceof Exchange) {
                    nameTxnPerExchange(transaction, (Exchange) item);
                    return;
                }
            }
        } else {
            nameTxnPerExchange(transaction, exchange);
        }
    }

    private static void nameTxnPerExchange(Transaction transaction, Exchange exchange) {
        String operation = "Kafka/Topic/Consume/Named";
        String topicName = getTopic(exchange);
        if (topicName != null) {
            operation = operation + "/" + topicName;
        }
        transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, CATEGORY, operation);
    }

    private static String getTopic(Exchange exchange) {
        String topic = (String) exchange.getIn().getHeader(TOPIC);
        if (topic == null) {
            Endpoint endpoint = CamelUtil.getEndpoint(exchange);
            if (endpoint != null) {
                Map<String, String> queryParameters = toQueryParameters(endpoint.getEndpointUri());
                topic = queryParameters.get("topic");
            }

        }

        if (topic == null) {
            topic = endpointPath(CamelUtil.getEndpoint(exchange));
        }
        return topic;
    }

    private static String endpointPath(Endpoint endpoint) {
        String component = "";
        if (endpoint == null) {
            return component;
        }
        String uri = endpoint.getEndpointUri();
        String[] splitUri = StringHelper.splitOnCharacter(uri, ":", 2);
        if (splitUri[1] != null) {
            component = splitUri[1];
        }
        if (component.startsWith("//")) {
            component = component.length() > 2 ? component.substring(2) : "";
        }
        String[] splitPath = StringHelper.splitOnCharacter(component, "?", 2);
        if (splitPath[0] != null) {
            component = splitPath[0];
        }
        return component;
    }

    private static Map<String, String> toQueryParameters(String uri) {
        int index = uri.indexOf('?');
        if (index != -1) {
            String queryString = uri.substring(index + 1);
            Map<String, String> map = new HashMap<>();
            for (String param : queryString.split("&")) {
                String[] parts = param.split("=");
                if (parts.length == 2) {
                    map.put(parts[0], parts[1]);
                }
            }
            return map;
        }
        return emptyMap();
    }

    @Override
    public boolean shouldStartTransaction() {
        return true;
    }

    @Override
    public void processInbound(Transaction txn, Exchange exchange) {
        if (txn == null) {
            return;
        }
        if (exchange.getIn().getBody() instanceof List && DT_CONSUMER_BATCH_ENABLED) {
            for (Object item : exchange.getIn().getBody(List.class)) {
                if (item instanceof Exchange) {
                    doProcessInbound(txn, (Exchange) item);
                }
            }
        } else {
            doProcessInbound(txn, exchange);
        }
    }

    private static void doProcessInbound(Transaction txn, Exchange exchange) {
        if (txn != null) {
            txn.acceptDistributedTraceHeaders(TransportType.Kafka, new ExchangeHeadersWrapper(exchange));
        }
        NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters.library(LIBRARY_NAME)
                .destinationType(DestinationType.NAMED_TOPIC)
                .destinationName(getTopic(exchange))
                .inboundHeaders(null)
                .build());
    }
}
