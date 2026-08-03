package com.newrelic.instrumentation.labs.redisson;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import org.redisson.connection.ConnectionManager;
import org.redisson.connection.MasterSlaveConnectionManager;
import org.redisson.connection.MasterSlaveEntry;
import org.redisson.misc.RedisURI;

import java.net.InetSocketAddress;

public class RedissonUtil {

    public static Segment createSegment(String prefix, String operationName) {
        return NewRelic.getAgent().getTransaction().startSegment(prefix + "-" + operationName);
    }

    public static DatastoreParameters createDatastoreParameters(String operationName, RedisAddr redisAddr) {
        DatastoreParameters.InstanceParameter param = DatastoreParameters.product("Redisson")
                .collection(null)
                .operation(operationName);
        if (redisAddr != null) {
            return param.instance(redisAddr.getHost(), redisAddr.getHost()).build();
        }
        return param.build();
    }

    public static RedisAddr getHost(ConnectionManager params) {
        if (params instanceof MasterSlaveConnectionManager) {
            MasterSlaveConnectionManager masterSlaveConnectionManager = ((MasterSlaveConnectionManager)params);
            for (MasterSlaveEntry entry : masterSlaveConnectionManager.getEntrySet()) {
                InetSocketAddress address = entry.getClient().getAddr();
                return new RedisAddr(address.getHostString(), address.getPort());
            }
        }
        RedisURI redisURI = params.getLastClusterNode();
        if (redisURI != null) {
            return new RedisAddr(redisURI.getHost(), redisURI.getPort());
        }
        return null;
    }

    public static class RedisAddr {
        private String host;
        private Integer port;
        public RedisAddr(String host, Integer port) {
            this.host = host;
            this.port = port;
        }
        public String getHost() {
            return host;
        }
        public Integer getPort() {
            return port;
        }
    }

}
