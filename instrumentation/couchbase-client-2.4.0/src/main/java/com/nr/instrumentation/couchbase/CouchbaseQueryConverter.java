package com.nr.instrumentation.couchbase;

import com.newrelic.api.agent.QueryConverter;

public class CouchbaseQueryConverter implements QueryConverter<CouchbaseQuery> {

    @Override
    public String toRawQueryString(CouchbaseQuery rawQuery) {
        StringBuffer sb = new StringBuffer();
        sb.append(rawQuery.getOperation());
        sb.append(' ');
        sb.append(rawQuery.getOperation());
        if(rawQuery.getId() != null) {
            sb.append(" id: ");
            sb.append(rawQuery.getId());
        } else if(rawQuery.getDocument() != null) {
            sb.append(" document: ");
            sb.append(rawQuery.getDocument().id());
        }
        return sb.toString();
    }

    @Override
    public String toObfuscatedQueryString(CouchbaseQuery rawQuery) {
        StringBuffer sb = new StringBuffer();
        sb.append(rawQuery.getOperation());
        sb.append(' ');
        sb.append(rawQuery.getOperation());
        sb.append(' ');
        if(rawQuery.getId() != null) {
            sb.append(" id: ?");
        } else if(rawQuery.getDocument() != null) {
            sb.append(" document: ?");
        }
        return sb.toString();
    }

}
