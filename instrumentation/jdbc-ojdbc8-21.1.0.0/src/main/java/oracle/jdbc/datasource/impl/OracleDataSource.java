package oracle.jdbc.datasource.impl;

import com.newrelic.agent.bridge.datastore.JdbcHelper;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.jdbc.oracle.OracleDatabaseVendor;

import java.sql.Connection;

@Weave(type = MatchType.BaseClass, originalName = "oracle.jdbc.datasource.impl.OracleDataSource")
public class OracleDataSource {

    public Connection getConnection(String userID, String pass) {
        JdbcHelper.putVendor(getClass(), OracleDatabaseVendor.INSTANCE);
        return Weaver.callOriginal();
    }

}
