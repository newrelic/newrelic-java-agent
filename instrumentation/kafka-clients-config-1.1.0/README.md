# Kafka Clients Config instrumentation

This instrumentation sends Kafka's configuration as events to New Relic.

## Events

The type of the event describes the type of configuration and each configuration option is sent as an attribute in that event.

The event names start with either KafkaConsumer, KafkaProducer or KafkaAdmin depending on what is being configured. It can also start with KafkaUnknownClient if the agent cannot define what is being configured.

Then the event name ends with Config, OverriddenDefaultConfig, SslConfig or SaslConfig.

So a KafkaConsumerConfig event will have the current consumer configuration options as properties.
While a KafkaProducerOverridenDefaultConfig event will have default values for producer configuration options that were changed.

## Configuration

Option                                        | Default | Description
----------------------------------------------|---------|------------
kafka.config.reporting.configurationCap       | 15      | The number of configurations to send. See note below.
kafka.config.reporting.enabled                | true    | Whether this instrumentation should send reports.
kafka.config.events.overriddenGeneralDefaults | true    | Whether to report default configurations that were overridden.
kafka.config.events.ssl                       | false   | Whether to report ssl configuration.
kafka.config.events.sasl                      | false   | Whether to report sasl configuration.
kafka.config.reporting.delay                  | PT5M    | The delay until the first report. In ISO-8601 duration format.
kafka.config.reporting.frequency              | PT1H    | The delay between reports. In ISO-8601 duration format.

### Configuration cap

Set a cap on the number clients for which configuration is reportable. The intention is to cap configuration reporting so that if an application is over-creating consumers/producers/admins with unique client IDs then we won't accrue a ridiculous number of configurations (as they are never removed) and we event reporting is not dominated by Kafka Config. 

This isn't a strict cap (it's possible we may report more clients than the cap indicates), but mainly a release valve to ensure that we don't accumulate a never-ending list of configuration events should client construction not be bounded. The default value should be sufficient for most well-behaved services, but otherwise this should be set to accommodate the number of expected Kafka clients during a service lifetime (assuming it's reasonable!), possibly with a bit of extra slop.