/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracers;

import java.net.URI;
import java.text.MessageFormat;
import java.util.logging.Level;

import com.newrelic.agent.util.ExternalsUtil;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.newrelic.agent.Agent;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.agent.util.Strings;

/**
 * {@link MetricNameFormat} implementation for an external call to another agent.
 */
public class CrossProcessNameFormat implements MetricNameFormat {

    private final String transactionName;
    private final String crossProcessId;
    private final String hostName;
    private final String uri;
    private final String transactionId;

    private static final String UNKNOWN_HOST = "Unknown";

    private CrossProcessNameFormat(String transactionName, String crossProcessId, String hostName, String uri,
            String transactionId) {
        this.hostName = hostName;
        this.crossProcessId = crossProcessId;
        this.transactionName = transactionName;
        this.uri = uri;
        this.transactionId = transactionId;
    }

    public String getHostCrossProcessIdRollupMetricName() {
        return Strings.join('/', "ExternalApp", hostName, crossProcessId, "all");
    }

    public String getTransactionId() {
        return transactionId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("host:").append(hostName).append(" crossProcessId:").append(crossProcessId).append(
                " transactionName:").append(transactionName).append(" uri:").append(uri).append(" transactionId:").append(
                transactionId);
        return sb.toString();
    }

    @Override
    public String getMetricName() {
        return Strings.join('/', "ExternalTransaction", hostName, crossProcessId, transactionName);
    }

    @Override
    public String getTransactionSegmentName() {
        return getMetricName();
    }

    @Override
    public String getTransactionSegmentUri() {
        return uri;
    }

    public static CrossProcessNameFormat create(String host, String uri, String decodedAppData) {
        if (decodedAppData == null) {
            return null;
        }

        if (host == null || host.length() == 0) {
            return null;
        }

        /*
          This should really be a constant in MetricNames, and it should have the same value as the one we use in ExternalMetrics.
         */
        String strURI = UNKNOWN_HOST;
        try {
            URI parsedURI = URI.create(uri);
            URI sanitizedURI = ExternalsUtil.sanitizeURI(parsedURI);
            if (sanitizedURI != null) {
                strURI = sanitizedURI.toString();
            }
        } catch (Throwable ignored) {
        }

        if (strURI.equals(UNKNOWN_HOST)){
            Agent.LOG.log(Level.FINER, "Unable to parse uri {0}. Using {1} instead", uri, UNKNOWN_HOST);
        }


        try {
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(decodedAppData);
            String crossProcessId = (String) arr.get(0);
            String transactionName = (String) arr.get(1);
            if (transactionName == null) {
                Agent.LOG.log(Level.FINER, "Null transactionName received from application data: {0}. Ignoring incoming CAT response.", decodedAppData);
                return null;
            }
            String transactionId = null;
            if (arr.size() > 5) {
                transactionId = (String) arr.get(5);
            }
            return new CrossProcessNameFormat(transactionName, crossProcessId, host, strURI, transactionId);
        } catch (ParseException ex) {
            if (Agent.LOG.isFinerEnabled()) {
                String msg = MessageFormat.format("Unable to parse application data {0}: {1}", decodedAppData, ex);
                Agent.LOG.finer(msg);
            }
        }
        return null;

    }

    public static CrossProcessNameFormat create(String host, String uri, String encodedAppData, String encodingKey) {
        if (encodedAppData == null) {
            return null;
        }
        if (encodingKey == null) {
            return null;
        }
        if (host == null || host.length() == 0) {
            return null;
        }
        String decodedAppData = Obfuscator.deobfuscateNameUsingKey(encodedAppData, encodingKey);

        return create(host, uri, decodedAppData);

    }

}
