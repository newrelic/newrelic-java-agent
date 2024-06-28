# hikaricp-2.4.0 Instrumentation Module

### Important Note Regarding Use of the Legacy HikariCP Extension Module
This built in instrumentation module replaces the extension module that can be downloaded from the New Relic
[download size](https://download.newrelic.com/newrelic/java-agent/extensions/). The `hikaricp-2.4.0.jar` 
artifact should be removed from the agent's `extension` folder if present.

### Reported Metrics
Every 5 seconds, the following metrics will be reported:
- `Database Connection/HikariCP/{PoolName}/Busy Count[connections]`: Retrieved from the [HikariPoolMXBean](https://www.javadoc.io/doc/com.zaxxer/HikariCP/2.4.6/com/zaxxer/hikari/HikariPoolMXBean.html) instance
- `Database Connection/HikariCP/{PoolName}/Idle Count[connections]`: Retrieved from the [HikariPoolMXBean](https://www.javadoc.io/doc/com.zaxxer/HikariCP/2.4.6/com/zaxxer/hikari/HikariPoolMXBean.html) instance
- `Database Connection/HikariCP/{PoolName}/Max Pool Size[connections]`: Retrieved from the [HikariConfig](https://www.javadoc.io/static/com.zaxxer/HikariCP/2.4.6/index.html?com/zaxxer/hikari/pool/HikariPool.html) instance
