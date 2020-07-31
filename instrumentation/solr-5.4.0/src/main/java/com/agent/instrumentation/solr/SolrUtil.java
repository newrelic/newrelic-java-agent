/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.solr;

import org.apache.lucene.search.Query;
import org.apache.solr.schema.IndexSchema;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SolrUtil {

    // solr framework agent attributes
    public static final String SOLR_LUCENE_QUERY = "library.solr.lucene_query";
    public static final String SOLR_LUCENE_QUERY_STRING = "library.solr.lucene_query_string";
    public static final String SOLR_QUERY_STRING = "library.solr.query_string";
    public static final String SOLR_RAW_QUERY_STRING = "library.solr.raw_query_string";
    public static final String SOLR_DEBUG_INFO_ERROR = "library.solr.solr_debug_info_error";

    public static Map<String, String> getSimpleParameterMap(Map<String, String[]> parameterMap, int maxSizeLimit) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> parameters = new HashMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String name = entry.getKey();
            String[] values = entry.getValue();
            String value = getValue(values, maxSizeLimit);
            if (value != null) {
                parameters.put(name, value);
            }
        }
        return parameters;
    }

    private static String getValue(String[] values, int maxSizeLimit) {
        if (values == null || values.length == 0) {
            return null;
        }
        String value = values.length == 1 ? values[0] : Arrays.asList(values).toString();
        if (value != null && value.length() > maxSizeLimit) {
            if (values.length == 1) {
                value = value.substring(0, maxSizeLimit);
            } else {
                value = value.substring(0, maxSizeLimit - 1) + ']';
            }
        }
        return value;
    }

    // haven't found a replacement for this in Solr 4.0.0 yet
    public static Query parseQuery(String qs, String defaultField, IndexSchema schema) {
        Query query = null;
//        try {
//            query = schema.getSolrQueryParser(defaultField).parse(qs);
//        } catch (Exception e) {
//        }
        return query;
    }

}
