# Kafka Clients Node Metrics instrumentation

This instrumentation is a replacement for the existing `kafka-clients-metrics` instrumentation.
It has a smaller scope as it just reads from KafkaMetrics and does not provide other functionality present in `kafka-client-metrics` instrumentation.

## Instrumentation

Whenever a Consumer or Producer is instantiated this instrumentation adds a MetricsReporter to the respective Metrics object.
This MetricsReporter will then be provided all the metrics related to that Consumer/Producer as well as some metrics related to the Kafka infrastructure.
Every period the MetricsReporter will then iterate thru all the metrics, convert into agent metrics, and then queue those metrics for sending.

This functionality is very similar to the metrics functionality of the `kafka-clients-metrics` instrumentation. The difference is that this instrumentation module will read the tags in the metric and add details about the node and the client to the metrics sent to New Relic.

## Configuration

Option                               | Default | Description
-------------------------------------|---------|--------------------------------------------
kafka.metrics.debug.enabled          | false   | Whether to log debug information.
kafka.metrics.node.metrics.disabled  | false   | Whether to send list of node metrics.
kafka.metrics.topic.metrics.disabled | false   | Whether to send list of node/topic metrics.
kafka.metrics.interval               | 30      | Number of seconds between metrics reports. 

## Comparison to kafka-client-metrics

The `kafka-clients-metrics` instrumentation has the following functionality which is not present in this instrumentation module:
- metrics about serialization and deserialization;
- tracing `KafkaProducer.doSend`;
- reporting `KafkaProducer.doSend` and `KafkaConsumer.poll` as externals;
- noticing errors when `KafkaProducer.doSend` or `KafkaConsumer.poll` throws an exception;
- rebalancing metrics;
- metrics that list the nodes and topics consumed/produce by each.