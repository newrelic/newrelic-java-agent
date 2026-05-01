package com.nr.instrumentation.apache.camel.processors;

import com.newrelic.agent.bridge.Transaction;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TransactionNamePriority;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.util.StringHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class KafkaExchangeProcessor implements ExchangeProcessor {

    private static final String TOPIC = "kafka.TOPIC";
    private static final String KAFKA = "Kafka";
    public static String APACHE_CAMEL = "ApacheCamel";

    @Override
    public void processInbound(Transaction transaction, Exchange exchange) {
        if (transaction != null) {
            NewRelic.getAgent().getTracedMethod().reportAsExternal(MessageConsumeParameters
                    .library(APACHE_CAMEL + KAFKA)
                    .destinationType(DestinationType.NAMED_TOPIC)
                    .destinationName(getTopic(exchange))
                    .inboundHeaders(new KafkaInboundWrapper(exchange))
                    .build());
        }
    }

    @Override
    public void nameTransaction(Transaction transaction, Exchange exchange) {
        if (transaction == null) {
            return;
        }
        String operation = KAFKA;
        String topic = getTopic(exchange);
        if (topic != null) {
            operation = operation + "/" + topic;
        }
        transaction.setTransactionName(TransactionNamePriority.FRAMEWORK_LOW, false, APACHE_CAMEL, operation);
        transaction.getTracedMethod().setMetricName(APACHE_CAMEL, operation);
    }

    private static String getTopic(Exchange exchange) {
        String topic = (String) exchange.getIn().getHeader(TOPIC);
        if (topic == null) {
            Map<String, String> queryParameters = toQueryParameters(exchange.getFromEndpoint().getEndpointUri());
            topic = queryParameters.get("topic");
        }

        if (topic == null) {
            topic = endpointPath(exchange.getFromEndpoint());
        }
        return topic;
    }

    private static String endpointPath(Endpoint endpoint) {
        String component = "";
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

    private static class KafkaInboundWrapper implements Headers {
        private final Map<String, String> headers;

        public KafkaInboundWrapper(Exchange exchange) {
            Map<String, String> headers = new HashMap<>();
            for (String header : exchange.getIn().getHeaders().keySet()) {
                String value = exchange.getIn().getHeader(header, String.class);
                if (value != null) {
                    headers.put(header, value);
                }
            }
            this.headers = headers;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.MESSAGE;
        }

        @Override
        public String getHeader(String name) {
            return headers.get(name);
        }

        @Override
        public List<String> getHeaders(String name) {
            String header = headers.get(name);
            return Collections.singletonList(header);
        }

        @Override
        public void setHeader(String name, String value) {
            // No-Op
        }

        @Override
        public void addHeader(String name, String value) {
            // No-Op
        }

        @Override
        public Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public boolean containsHeader(String name) {
            return headers.containsKey(name);
        }
    }
}
