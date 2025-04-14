package com.nr.instrumentation.couchbase;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.agent.bridge.datastore.JdbcDatabaseVendor;

public class CouchbaseVendor extends JdbcDatabaseVendor {
    
    

    public CouchbaseVendor() {
        super("Couchbase", "couchbase", false);
    }

    @Override
    public DatastoreVendor getDatastoreVendor() {
        return DatastoreVendor.JDBC;
    }


}
