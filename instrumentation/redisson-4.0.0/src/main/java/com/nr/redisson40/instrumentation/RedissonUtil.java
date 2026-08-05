package com.nr.redisson40.instrumentation;

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

    public static DatastoreParameters createDatastoreParameters(String operationName, NrRedisUri nrRedisUri) {
        DatastoreParameters.InstanceParameter param = DatastoreParameters.product("Redisson")
                .collection(null)
                .operation(operationName);
        if (nrRedisUri != null) {
            return param.instance(nrRedisUri.getHost(), nrRedisUri.getPort()).build();
        }
        return param.build();
    }

    public static NrRedisUri extractUri(ConnectionManager params) {
        if (params instanceof MasterSlaveConnectionManager) {
            MasterSlaveConnectionManager masterSlaveConnectionManager = ((MasterSlaveConnectionManager)params);
            for (MasterSlaveEntry entry : masterSlaveConnectionManager.getEntrySet()) {
                InetSocketAddress address = entry.getClient().getAddr();
                return new NrRedisUri(address.getHostString(), address.getPort());
            }
        }
        RedisURI redisURI = params.getLastClusterNode();
        if (redisURI != null) {
            return new NrRedisUri(redisURI.getHost(), redisURI.getPort());
        }
        return null;
    }

    public static class NrRedisUri {
        private String host;
        private Integer port;
        public NrRedisUri(String host, Integer port) {
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
