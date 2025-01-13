# AWS Java SQS Spans Instrumentation

Enable this module and disable the `aws-java-sdk-sqs` module if
you want to see distributed tracing for SQS at the cost of 
losing visibility of HTTP calls to the AWS API.