# Database Connection Detection: jdbc-socket to SQL Tracer

## Overview

The agent uses a layered approach: a low-level socket weave captures the actual TCP endpoint, generic JDBC weaves stitch that address to a `Connection` object and register a `ConnectionFactory`, vendor-specific weaves pre-populate the vendor lookup, and finally the `DefaultSqlTracer` assembles everything into a reportable datastore segment.

---

## Layer 1 — Socket-level address capture

**Module:** `instrumentation/jdbc-socket`

Two weave classes — `Socket_Instrumentation` (weaves `java.net.Socket.connect`) and `SocketChannel_Instrumentation` (weaves `java.nio.channels.SocketChannel.open`) — fire immediately after the TCP handshake completes. Each checks `DatastoreInstanceDetection.shouldDetectConnectionAddress()` and, if true, calls `DatastoreInstanceDetection.saveAddress(InetSocketAddress)`.

`saveAddress` stores the endpoint in a `ThreadLocal<InetSocketAddress>`. If two different addresses are detected on the same thread (e.g., Azure SQL proxy redirect), the `datastore_multihost_preference` config (`FIRST`, `LAST`, or `NONE`) controls which wins.

Detection is gated by a second `ThreadLocal<ConnectionState>` (enum: `DO_NOT_DETECT_ADDRESS` / `DETECT_ADDRESS`), defaulting to off. The socket weaves only record an address when the state is explicitly `DETECT_ADDRESS` — preventing spurious captures from unrelated socket activity on the same thread.

---

## Layer 2 — JDBC Driver/DataSource weaves

**Module:** `instrumentation/jdbc-generic`

`Driver_Weaved` and `DataSource_Weaved` bracket the actual connection establishment:

```
detectConnectionAddress()         ← flip ThreadLocal state to DETECT_ADDRESS
Weaver.callOriginal()             ← real connect; socket weave fires inside here
associateAddress(connection)      ← move address from ThreadLocal into the map
stopDetectingConnectionAddress()  ← flip state back, clear ThreadLocal address
```

`associateAddress` writes into `DatastoreInstanceDetection.connectionToAddress`, a **weak-keyed concurrent map** (`Map<Object, InetSocketAddress>`). Keying weakly on the `Connection` object means entries are automatically evicted when the connection is GC'd — no manual cleanup needed.

The "first in path" guard (`!shouldDetectConnectionAddress()` before flipping state on) prevents nested calls (e.g., a DataSource that internally uses a Driver) from prematurely stopping detection mid-stack.

After the connection is established, if no `ConnectionFactory` is registered yet for this URL:
1. The URL is retrieved via `connection.getMetaData().getURL()` (with a `ThreadLocal<Boolean> connectionLookup` recursion guard to prevent infinite loops if `getMetaData()` itself opens a connection)
2. The vendor is resolved (see Layer 3)
3. A `JdbcDriverConnectionFactory` or `JdbcDataSourceConnectionFactory` is created and stored in `JdbcHelper.urlToFactory` — a URL-keyed map with **time-based eviction (default 7200s)**

`Connection_Weaved` also intercepts every `prepareStatement`/`prepareCall` call and caches the SQL text in `JdbcHelper.statementToSql` (a **weak-keyed map** by `Statement` object) so it can be retrieved later at execution time without touching the statement again.

---

## Layer 3 — Vendor detection

**Modules:** vendor-specific (e.g., `instrumentation/jdbc-mysql-8.0.11`) + `JdbcHelper`

Vendor-specific weave classes (e.g., `NonRegisteringDriver`, `MysqlDataSource`) call `JdbcHelper.putVendor(driverClass, MySQLDatabaseVendor.INSTANCE)` early in the driver's own `connect()` path. This populates two maps in `JdbcHelper`:

- `classToVendorLookup` — `Map<Class<?>, DatabaseVendor>`, **weak-keyed by driver class**, the fast path
- `typeToVendorLookup` — `Map<String, DatabaseVendor>` keyed by JDBC URL scheme (e.g., `"mysql"`), the fallback

`JdbcHelper.getVendor(driverClass, url)` tries the class map first; if that misses (e.g., a driver with no vendor-specific module), it applies `VENDOR_PATTERN` (`jdbc:([^:]*)..*`) against the URL to extract the scheme and hits the type map.

---

## Layer 4 — SQL execution and DefaultSqlTracer

When a `Statement.execute*()` call is intercepted, a `DefaultSqlTracer` is created and `provideConnection(connection)` is called. This method assembles all the cached state:

1. **ConnectionFactory** — `JdbcHelper.getConnectionFactory(conn)` → URL lookup in `urlToFactory` → provides vendor identity
2. **Host/port** — `DatastoreInstanceDetection.getAddressForConnection(conn)` → lookup in `connectionToAddress` (weak map). If found, sets `host` and `port` directly.
3. **In-memory databases** — if no TCP address, falls back to `JdbcHelper.getCachedIdentifierForConnection(conn)` (or parses it fresh from the URL via regex) and sets `host="localhost"`.
4. **Database name** — `JdbcHelper.getDatabaseName(conn)` → `connection.getCatalog()`, cached in `urlToDatabaseName` (also URL-keyed, 7200s eviction).

At tracer finish, `recordMetrics()` builds a `DatastoreParameters` object from all of the above and reports the segment. SQL text comes from `JdbcHelper.getSql(statement)` — retrieved from `statementToSql`. After execution, `JdbcHelper.removeStatement(statement)` evicts both the SQL and params from their weak maps.

---

## Cache summary

| Cache | Key type | Value | Eviction |
|---|---|---|---|
| `connectionToAddress` | Weak `Object` (Connection) | `InetSocketAddress` | GC of connection |
| `connectionToURL` | Weak `Connection` | `String` (URL or `"UNKNOWN"` sentinel) | GC of connection |
| `statementToSql` | Weak `Statement` | `String` SQL | GC of statement, or explicit `removeStatement()` |
| `statementToParams` | Weak `Statement` | `Object[]` | Same as above |
| `classToVendorLookup` | Weak `Class<?>` | `DatabaseVendor` | GC of class |
| `typeToVendorLookup` | `String` (URL scheme) | `DatabaseVendor` | Never (singletons) |
| `urlToFactory` | `String` (URL) | `ConnectionFactory` | Time-based, 7200s |
| `urlToDatabaseName` | `String` (URL) | `String` database name | Time-based, 7200s |

---

## ThreadLocal summary

| ThreadLocal | Type | Purpose |
|---|---|---|
| `DatastoreInstanceDetection.state` | `ConnectionState` enum | Gates whether socket weaves record addresses |
| `DatastoreInstanceDetection.address` | `InetSocketAddress` | Holds the detected endpoint until `associateAddress()` moves it to the map |
| `JdbcHelper.connectionLookup` | `Boolean` | Recursion guard for `getMetaData().getURL()` calls |

This document partial generated by AI.