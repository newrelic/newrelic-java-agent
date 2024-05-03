package com.newrelic.agent.bridge.datastore;

import com.newrelic.api.agent.QueryConverter;

public class SqlQueryConverter implements QueryConverter<String> {
    public static final QueryConverter<String> INSTANCE = new SqlQueryConverter();

    private SqlQueryConverter() {}

    @Override
    public String toRawQueryString(String rawQuery) {
        return rawQuery;
    }

    @Override
    public String toObfuscatedQueryString(String rawQuery) {
        return null;
    }
}
