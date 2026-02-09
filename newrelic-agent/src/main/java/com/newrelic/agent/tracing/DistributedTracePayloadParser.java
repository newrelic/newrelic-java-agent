/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.newrelic.agent.MetricNames;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.api.agent.MetricAggregator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Level;

import static com.newrelic.agent.tracing.DistributedTraceUtil.*;

public class DistributedTracePayloadParser {
    private final MetricAggregator metricAggregator;
    private final DistributedTraceService distributedTraceService;
    private final IAgentLogger logger;

  public DistributedTracePayloadParser(MetricAggregator metricAggregator,
        DistributedTraceService distributedTraceService,
        IAgentLogger logger) {
        this.metricAggregator = metricAggregator;
        this.distributedTraceService = distributedTraceService;
        this.logger=logger;
    }

    public DistributedTracePayloadImpl parse(DistributedTracePayloadImpl outboundPayloadData, String payload) {
        if (payload == null) {
            logger.log(Level.FINER, "Incoming distributed trace payload is null.");
            metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_NULL);
            return null;
        }
        // record supportability error if someone called createDistributedTracePayload already
        if (outboundPayloadData != null) {
            logger.log(Level.WARNING, "Error: createDistributedTracePayload was called before acceptDistributedTracePayload. Ignoring Call");
            metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_CREATE_BEFORE_ACCEPT);
            return null;
        }

        logger.log(Level.INFO, "DTPayload: raw payload in parse(): " + payload);
        if (!payload.trim().isEmpty()) {
            payload = payload.trim();
            char firstChar = payload.charAt(0);
            if (firstChar != '{') {
                // This must be base64 encoded, decode it
                payload = new String(Base64.getDecoder().decode(payload), StandardCharsets.UTF_8);
            }
        }

        JSONParser parser = new JSONParser();
        try {
            JSONObject object = (JSONObject) parser.parse(payload);

            // ignore payload if major version is higher than our own
            JSONArray version = (JSONArray) object.get(VERSION);
            final Long majorVersion = (Long) version.get(0);
            int majorSupportedVersion = distributedTraceService.getMajorSupportedCatVersion();
            if (majorVersion > majorSupportedVersion) {
                logger.log(Level.FINER,
                        "Incoming distributed trace payload major version: {0} is newer than supported agent"
                                + " version: {1}. Ignoring payload.", majorVersion, majorSupportedVersion);
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_MAJOR_VERSION);
                return null;
            }

            JSONObject data = (JSONObject) object.get(DATA);

            // ignore payload if accountId isn't trusted
            String payloadAccountId = (String) data.get(ACCOUNT_ID);

            // ignore payload if isn't trusted
            String payloadTrustKey = (String) data.get(TRUSTED_ACCOUNT_KEY);
            String trustKey = distributedTraceService.getTrustKey();

            logger.log(Level.INFO, "DTPayload: payloadAcctId: {0}   payloadTrustKey: {1}   trustKey: {2}", payloadAccountId, payloadTrustKey, trustKey);

            //the agent obtains the trustKey from the connect payload
            if (trustKey == null) {
                logger.log(Level.FINER,
                        "The agent has not connected yet. Unable to accept incoming distributed trace payload.");
                return null;
            }

            if (payloadAccountId == null) {
                logger.log(Level.FINER, "Invalid payload {0}. Payload missing accountId.", data);
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            String applicationId = (String) data.get(APPLICATION_ID);
            logger.log(Level.INFO, "DTPayload: applicationId: {0}", applicationId);
            if (applicationId == null) {
                logger.log(Level.FINER, "Incoming distributed trace payload is missing application id");
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            // If payload doesn't have a tk, use accountId
            boolean isTrustedAccountKey = trustKey.equals(payloadTrustKey == null ? payloadAccountId : payloadTrustKey);
            if (!isTrustedAccountKey) {
                logger.log(Level.FINER,
                        "Incoming distributed trace payload trustKey: {0} does not match trusted account key: {1}."
                                + " Ignoring payload.", payloadTrustKey, trustKey);
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_UNTRUSTED_ACCOUNT);
                return null;
            }

            long timestamp = (Long) data.get(TIMESTAMP);
            logger.log(Level.INFO, "DTPayload: timestamp: {0}", timestamp);
            if (timestamp <= 0) {
                logger.log(Level.FINER, "Invalid payload {0}. Payload missing keys.", data);
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            String parentType = (String) data.get(PARENT_TYPE);
            logger.log(Level.INFO, "DTPayload: parentType: {0}", parentType);
            if (parentType == null) {
                logger.log(Level.FINER, "Incoming distributed trace payload is missing type");
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            String traceId = (String) data.get(TRACE_ID);
            logger.log(Level.INFO, "DTPayload: traceId: {0}", traceId);
            if (traceId == null) {
                logger.log(Level.FINER, "Incoming distributed trace payload is missing traceId");
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            String guid = (String) data.get(GUID);
            String txnId = (String) data.get(TX);
            logger.log(Level.INFO, "DTPayload: guid: {0}   txnId: {1}", guid, txnId);
            if (guid == null && txnId == null) {
                // caller has span events disabled and there's no transaction?
                // they must be using txn-less api, but no spans?
                logger.log(Level.FINER, "Incoming distributed trace payload is missing traceId");
                metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
                return null;
            }

            Number priorityNumber = (Number) data.get(PRIORITY);
            Float priority = priorityNumber != null ? priorityNumber.floatValue() : null;
            Sampled sampled = Sampled.parse(data.get(SAMPLED));

            DistributedTracePayloadImpl distributedTracePayload = new DistributedTracePayloadImpl(timestamp,
                    parentType, payloadAccountId, payloadTrustKey, applicationId, guid, traceId, txnId, priority, sampled);

            if (logger.isFinestEnabled()) {
                logger.log(Level.FINEST, "Parsed distributed trace payload: {0}", distributedTracePayload);
            }

            return distributedTracePayload;
        } catch (Exception e) {
            logger.log(Level.FINEST, e, "Failed to parse distributed trace payload");
            metricAggregator.incrementCounter(MetricNames.SUPPORTABILITY_ACCEPT_PAYLOAD_IGNORED_PARSE_EXCEPTION);
            return null;
        }
    }
}
