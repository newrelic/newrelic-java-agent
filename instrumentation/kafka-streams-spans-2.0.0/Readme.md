# Kafka Streams Spans instrumentation
Kafka Streams is a library that runs on top of kafka clients to stream and process data from kafka.
This instrumentation module creates transaction every time records gets polled and then processed from kafka.

## Troubleshooting

If you are using kafka streams and encounter a transaction with the name: `Kafka/Streams/APPLICATION_ID_UNKNOWN`,
here are the possible causes:

1. You are using at least 2 Kafka Stream instances with the same `client.id` configured but then closed one of the streams instances. 
A possible workaround is to give a different `client.id` for each instance. Another is to run each instance in a separate app.
2. If the above does not apply, and you are using the latest Kafka Streams instrumentation module, it is possible this is a silent failure created from a new 
Kafka Streams version. This would likely have happened due to Kafka Streams naming their threads differently for the new version. This is because under the hood 
we generally name our transactions by parsing the name of the current thread the transaction began in to get the client id. Then we use the client id to access 
the `application.id` configured for your Kafka Streams instance. If this happens, please report this as this will signal us that we need a new instrumentation 
module for more up-to-date Kafka Streams versions.