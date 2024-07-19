# hikaricp-2.4.0 Instrumentation Module
This module supports HikariCP version 2.4.0 to current.

### Important Note Regarding Use of the Legacy HikariCP Extension Module
This built in instrumentation module replaces the extension module that can be downloaded from the New Relic
[download site](https://download.newrelic.com/newrelic/java-agent/extensions/). The `hikaricp-2.4.0.jar` 
artifact should be removed from the agent's `extension` folder if present.

### Reported Metrics
Every 5 seconds, the following metrics will be reported:
- `Database Connection/HikariCP/Busy Count[connections]`: Retrieved from HikariPool.getActiveConnections()
- `Database Connection/HikariCP/Idle Count[connections]`: Retrieved from HikariPool.getIdleConnections()
- `Database Connection/HikariCP/Total Count[connections]`: Retrieved from HikariPool.getTotalConnections()
- `Database Connection/HikariCP/Threads Awaiting Count[connections]`: Retrieved from HikariPool.getThreadsAwaitingConnection()
- `Database Connection/HikariCP/Max Pool Size[connections]`: Retrieved from HikariConfig.getMaximumPoolSize()
- `Database Connection/HikariCP/Minimum Idle Size[connections]`: Retrieved from HikariConfig.getMinimumIdle()
- `Database Connection/HikariCP/Connection Timeout`: Retrieved from HikariConfig.getConnectionTimeout()
- `Database Connection/HikariCP/Idle Timeout`: Retrieved from HikariConfig.getIdleTimeout()
- `Database Connection/HikariCP/Leak Detection Threshold`: Retrieved from HikariConfig.getLeakDetectionThreshold()
- `Database Connection/HikariCP/Maximum Lifetime`: Retrieved from HikariConfig.getMaxLifetime()
- `Database Connection/HikariCP/Validation Timeout`: Retrieved from HikariConfig.getValidationTimeout()