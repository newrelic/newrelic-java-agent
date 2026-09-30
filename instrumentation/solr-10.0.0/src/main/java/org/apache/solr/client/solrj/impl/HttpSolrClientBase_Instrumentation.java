/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.apache.solr.client.solrj.impl;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.common.util.NamedList;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;

/*
 * Solr 10 removed the deprecated HttpSolrClient that we previously instrumented.
 * The alternatives are the new HttpJettySolrClient and HttpJdkSolrClient, both
 * of which extend this HttpSolrClientBase class.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.apache.solr.client.solrj.impl.HttpSolrClientBase")
public class HttpSolrClientBase_Instrumentation {

    @Trace
    public NamedList<Object> requestWithBaseUrl(
            String baseUrl, SolrRequest<?> solrRequest, String collection) {
        NamedList<Object> namedList = Weaver.callOriginal();

        try {
            URL url = new URI(getBaseURL()).toURL();

            DatastoreParameters params = DatastoreParameters
                    .product(DatastoreVendor.Solr.name())
                    .collection(collection)
                    .operation(solrRequest.getMethod().toString())
                    .instance(url.getHost(), url.getPort())
                    .databaseName(null)
                    .build();

            NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        } catch (MalformedURLException | URISyntaxException e) {
            NewRelic.getAgent().getLogger().log(Level.WARNING, "Could not parse host and/or port from HTTP Solr Client", e);
        }
        return namedList;
    }

    public String getBaseURL() {
        return Weaver.callOriginal();
    }
}
