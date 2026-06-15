# Lettuce 6.0 Instrumentation

This module instruments the Lettuce Redis client starting with version 6.0.0, and up to (not including) Lettuce 6.5.0. 
It provides comprehensive monitoring for synchronous, asynchronous, and reactive Redis operations.

## How It Works

### Architecture

The instrumentation uses a two-stage approach:

1. **Connection URI Capture** - The Redis URI is captured when a connection is created and stored on the connection object using `@NewField`
    - For the standard (non-cluster) client, this is the Redis URI used to build the client.
    - For the cluster client, this is the **first seed URI** used to build the client. See the note below on Cluster Mode.
2. **Command Instrumentation** - Redis commands retrieve the stored URI from the connection to create datastore metrics

### Redis Cluster Mode Specifics

Redis Cluster Clients are initialized with one or more **seed** URIs, from which the rest of the nodes of the cluster are discovered.

In Cluster Mode, we do not know the actual node a Redis Command will be routed to at the time its dispatch method is called. For this reason -
and to represent the entire cluster with a single, shared identity in the New Relic Platform - all externals will be reported under the
**first seed URI**, regardless of the node that eventually fulfills the command.

Eg: If you have a Redis cluster of six nodes, `node-1, node-2, node-3, node-4, node-5, node-6`, and seed the Cluster Client with the addresses of the
first two nodes:

```java
RedisURI node1 = RedisURI.create("redis://node-1:6379");
RedisURI node2 = RedisURI.create("redis://node-2:6379");
RedisClusterClient clusterClient = RedisClusterClient.create(Arrays.asList(node1, node2));
```

... **all datastore instance metrics** will be reported under the host and port `node-1:6379`. We will continue to report externals to this host for the
lifetime of the client, even if the first seed node is decommissioned during a cluster refresh. 