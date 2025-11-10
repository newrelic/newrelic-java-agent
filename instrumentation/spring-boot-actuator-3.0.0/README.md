# spring-actuator-3.0.0 Instrumentation Module

By default, built-in actuator endpoints and custom actuator endpoints (using the @Endpoint annotation
and it's subclasses) will all be named as "OperationHandler/handle" in New Relic. Activating this
module will result in the transaction name reflecting the actual base actuator endpoint URI.
For example, invoking "/actuator/loggers" or "actuator/loggers/com.newrelic" will result in the
transaction name "actuator/loggers (GET)". This is to prevent MGI.

To activate actuator naming, set the following configuration to true:
- `class_transformer.name_actuator_endpoints`

The default value is false.