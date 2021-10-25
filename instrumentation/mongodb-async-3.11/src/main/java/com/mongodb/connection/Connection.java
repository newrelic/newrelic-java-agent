package com.mongodb.connection;

import org.bson.codecs.Decoder;

import com.mongodb.MongoNamespace;
import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.mongo.MongoUtil;

@Weave(type = MatchType.Interface)
public abstract class Connection {

    @Trace(leaf = true)
    public <T> QueryResult<T> getMore(MongoNamespace namespace, long cursorId, int numberToReturn,
            Decoder<T> resultDecoder) {

        DatastoreParameters params = DatastoreParameters
                .product(DatastoreVendor.MongoDB.name())
                .collection(namespace.getCollectionName())
                .operation(MongoUtil.OP_GET_MORE)
                .instance(getDescription().getServerAddress().getHost(), getDescription().getServerAddress().getPort())
                .databaseName(namespace.getDatabaseName())
                .build();
        NewRelic.getAgent().getTracedMethod().reportAsExternal(params);

        return Weaver.callOriginal();
    }

    public abstract ConnectionDescription getDescription();

}
