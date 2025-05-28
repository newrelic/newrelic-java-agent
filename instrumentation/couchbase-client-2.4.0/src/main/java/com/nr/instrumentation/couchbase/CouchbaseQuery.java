package com.nr.instrumentation.couchbase;

import com.couchbase.client.java.document.Document;

public class CouchbaseQuery {

    private String id;
    private String bucket;
    private String operation;
    private Document<?> document;
    
    protected String getId() {
        return id;
    }

    protected String getBucket() {
        return bucket;
    }

    protected String getOperation() {
        return operation;
    }

    protected Document<?> getDocument() {
        return document;
    }

    public CouchbaseQuery(String i, String b, String o) {
        id = i;
        bucket = b;
        operation = o;
    }

    public CouchbaseQuery(Document<?> d, String b, String o) {
        document = d;
        bucket = b;
        operation = o;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("bucket: ");
        sb.append(bucket);
        sb.append(", operation: ");
        sb.append(operation);
        if(id != null) {
            sb.append(", id: ");
            sb.append(id);
        } else if(document != null) {
            sb.append(", document: id-");
            sb.append(document.id());
        } else {
            sb.append(", id: null, document: null");
        }
        return sb.toString();
    }

    
}
