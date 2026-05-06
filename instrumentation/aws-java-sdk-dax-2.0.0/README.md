# aws-java-sdk-dax-2.0.0 Instrumentation Module

This module provides instrumentation for AWS DAX (DynamoDB Accelerator) clients using the 
`software.amazon.dax:amazon-dax-client:2.0.0` library. DAX is an in-memory cache for DynamoDB.
This module targets the DAX SDK from version 2.0.0 to present.

## Supported Operations

DAX only supports **data-plane operations** (not table management operations). This module instruments the following operations:

| Operation | Description |
|-----------|-------------|
| `getItem` | Retrieve a single item by primary key |
| `putItem` | Create or replace an item |
| `deleteItem` | Delete a single item |
| `updateItem` | Modify an existing item's attributes |
| `batchGetItem` | Retrieve multiple items in a single request |
| `batchWriteItem` | Put or delete multiple items in a single request |
| `query` | Query items by partition key and optional sort key conditions |
| `scan` | Scan all items in a table |
| `transactGetItems` | Retrieve multiple items in a transaction |
| `transactWriteItems` | Write multiple items in a transaction |

## Instrumented Classes

The module instruments the async client implementation class:

- **Async Client**: `software.amazon.dax.MetricAsyncClient` (used via `ClusterDaxAsyncClient`)

Both synchronous and asynchronous DAX operations are captured because the sync client (`DelegateSyncClient`)
internally delegates all operations to the async client (`MetricAsyncClient`). This architecture means instrumenting
only the async client captures all DAX operations regardless of which client type the application uses.

```
ClusterDaxClient (sync)                    ClusterDaxAsyncClient (async)
  └─> DelegateSyncClient                     └─> MetricAsyncClient  ← instrumented
        └─> delegates to MetricAsyncClient ──────┘
```

## Configuration Capture

Unlike the standard DynamoDB SDK client, DAX clients do not expose their `Configuration` object after construction.
To capture endpoint and region information for metrics, this module uses a caching strategy:

When `MetricAsyncClient` is constructed, it receives a `Configuration` object. The instrumentation captures this
configuration and stores it in a `WeakHashMap` cache keyed by the client instance. This cached configuration is
then used during operation instrumentation to extract host, port, region, and credentials for ARN construction.

This approach ensures all DAX operations have access to the configuration needed for accurate metrics reporting.

## Metrics Reported

Each DAX operation is reported as a datastore external with the following attributes:

| Attribute | Value |
|-----------|-------|
| Product | `DAX` |
| Collection | Table name (or `batch`/`transaction` for multi-table operations) |
| Operation | The operation name (e.g., `getItem`, `query`) |
| Host | DAX cluster endpoint hostname |
| Port | DAX cluster port (default: 8111) |
| Cloud Resource ID | DynamoDB table ARN (when available) |

## Example Usage

```java
// Create a DAX client
DynamoDbClient daxClient = ClusterDaxClient.builder()
    .region(Region.US_EAST_1)
    .credentialsProvider(DefaultCredentialsProvider.create())
    .overrideConfiguration(c -> c.addMetricPublisher(myPublisher))
    .endpointDiscovery(e -> e.enabled(true))
    .endpoints("dax://my-cluster.abc123.dax-clusters.us-east-1.amazonaws.com")
    .build();

// Use the client - operations are automatically instrumented
GetItemResponse response = daxClient.getItem(GetItemRequest.builder()
    .tableName("MyTable")
    .key(Map.of("id", AttributeValue.builder().s("123").build()))
    .build());
```

A full example app can be found on the AWS docs [site](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DAX.client.TryDax.java.html).

## Transaction Naming

DAX operations appear as external datastore calls within your transactions. The metric names follow the pattern:

```
Datastore/statement/DAX/{tableName}/{operation}
Datastore/operation/DAX/{operation}
```

For example:
- `Datastore/statement/DAX/MyTable/getItem`
- `Datastore/statement/DAX/batch/batchWriteItem`
- `Datastore/statement/DAX/transaction/transactGetItems`

## Notes

- DAX tables are DynamoDB tables, so the ARN format uses `dynamodb` in the service portion: `arn:aws:dynamodb:{region}:{account}:table/{tableName}`
- If configuration information is unavailable, the module gracefully falls back to default values (host: `amazon`, no ARN)
- The module captures configuration during client construction for later use in metrics reporting
