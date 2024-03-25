# spring-batch-4.0.0 Instrumentation Module

This module provides instrumentation for the Spring Batch 4 framework (up to, but not including version 5).

Only single process, single threaded jobs are supported. Single process, multi-threaded and 
multi-process jobs are not supported.

### Details
The instrumentation will weave the following classes:

- `org.springframework.batch.core.Job`: In Spring Batch, the Job class wraps the steps and processors that make a up a complete batch job implementation. 
The execute() method will be the transaction dispatcher. The transaction name will be `OtherTransaction/SpringBatch/Job/{jobName}`

- `org.springframework.batch.core.Step`: The Step class represents a single step of the target batch job. The execute() method will be instrumented in order
to track the total execution time of a job step. The timing of a Step task will include the time to read the data 
from the defined data source, the processing of the data and the time to write the data to the target data source.

- `org.springframework.batch.item.ItemWriter`: The ItemWriter class is used to write the processed/transformed items to a data store. There will be the same number 
of ItemWriter segments as Step segments, since there is one write operation per Step.

The ItemReader and ItemProcessor classes will not be instrumented since these are each called once per data item, 
which would cause the Transaction traces to be inflated unnecessarily. The timings for these calls are included 
in the Step instrumentation timing.

### Metrics
The following metrics will be reported:

- `SpringBatch/Job/{jobName}/Step/{stepName}/read`
- `SpringBatch/Job/{jobName}/Step/{stepName}/write`
- `SpringBatch/Job/{jobName}/Step/{stepName}/skipped`

These metrics represent the total number of items read, written and skipped for a particular step for a completed job.
