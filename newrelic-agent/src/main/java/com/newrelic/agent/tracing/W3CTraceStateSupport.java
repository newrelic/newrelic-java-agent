/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.tracing;

import com.google.common.base.Joiner;
import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricNames;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.NewRelic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import static com.newrelic.agent.tracing.W3CTraceStateHeader.MULTI_TENANT_VENDOR_STATE_KEY;
import static com.newrelic.agent.tracing.W3CTraceStateHeader.NR_TRACE_STATE_DELIMITER;
import static com.newrelic.agent.tracing.W3CTraceStateHeader.NR_VENDOR;
import static com.newrelic.agent.tracing.W3CTraceStateHeader.VENDOR_STATE_KEY_VALUE_DELIMITER;

public class W3CTraceStateSupport {
    // Reference: https://w3c.github.io/trace-context/#key
    private static final String SINGLE_TENANT_VENDOR_STATE_KEY = "[a-z][_0-9a-z\\-*/]{0,255}";

    private static final String VENDOR_STATE_KEY = "(" + SINGLE_TENANT_VENDOR_STATE_KEY + "|" + MULTI_TENANT_VENDOR_STATE_KEY + ")";

    // Reference: https://w3c.github.io/trace-context/#value
    private static final String VENDOR_STATE_VALUE = "[\\x20-\\x2b\\x2d-\\x3c\\x3e-\\x7e]{0,255}[\\x21-\\x2b\\x2d-\\x3c\\x3e-\\x7e]";

    private static final Pattern VENDOR_STATE_PATTERN = Pattern.compile(
            "^"
                    + VENDOR_STATE_KEY
                    + VENDOR_STATE_KEY_VALUE_DELIMITER
                    + VENDOR_STATE_VALUE
                    + "$"
    );

    static final int NR_HEADER_VERSION_INT = 0;
    public static final String W3C_TRACE_STATE_VENDOR_DELIMITER = ",";
    private static final int MAX_VENDOR_STATE_SIZE = 31;
    private static final int LONG_VENDOR_STATE_SIZE = 128;

    static W3CTraceState parseHeaders(List<String> traceStateHeaders) {
        if (traceStateHeaders == null || traceStateHeaders.isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceStateHeaders length is null or empty; returning null");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_STATE_HEADER_COUNT);
            return null;
        }

        String agentTrustKey = ServiceFactory.getDistributedTraceService().getTrustKey();
        Agent.LOG.log(Level.INFO, "DTTrace: traceStateHeaders agentTrustKey: {0}", agentTrustKey);
        VendorStateResult vendorStateResult = flattenVendorStatesAndExtractNrState(traceStateHeaders, agentTrustKey);

        List<String> vendorStates = vendorStateResult.getVendorStates();
        if (vendorStates != null && !vendorStates.isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceStateHeaders vendorStates...");
            for (String v : vendorStates) {
                Agent.LOG.log(Level.INFO, "    DTTrace: {0}", v);
            }
        }

        String nrState = vendorStateResult.getNrState();
        Agent.LOG.log(Level.INFO, "DTTrace: traceStateHeaders nrState: {0}", nrState);
        W3CTraceState traceState = new W3CTraceState(traceStateHeaders, vendorStates);

        if (nrState == null) {
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_NO_NR_ENTRY);
            // could not find matching NR state in the trace state header, propagate other vendor states
            return traceState;
        }

        String[] trustKeyAndFields = nrState.split(NR_VENDOR);
        if (trustKeyAndFields.length != 2) {
            Agent.LOG.log(Level.INFO, "DTTrace: trustKeyAndFields is invalid (length != 2");
            // NR state header must have a key and a value separated by an "=" and the key ending in "@nr"
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            return traceState;
        }

        String trustKey = trustKeyAndFields[0];
        Agent.LOG.log(Level.INFO, "DTTrace: trustKey: {0}", trustKey);
        Agent.LOG.log(Level.INFO, "DTTrace: traceFields: {0}", trustKeyAndFields[1]);
        boolean isTrustedAccountKey = agentTrustKey.equals(trustKey);
        if (!isTrustedAccountKey) {
            // not a trusted account
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_UNTRUSTED_ACCOUNT);
            return traceState;
        }

        String[] traceFields = trustKeyAndFields[1].split(NR_TRACE_STATE_DELIMITER, 10);
        if (traceFields.length < 9) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields length < 9");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // NR state header requires 9 or more fields
            return traceState;
        }

        if (traceFields[0].isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[0] is empty");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // version cannot be empty
            return traceState;
        }

        int version = Integer.parseInt(traceFields[0]);
        if (version < NR_HEADER_VERSION_INT) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[0] (version) is unsupported");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // unsupported NR version
            return traceState;
        }

        if (traceFields[1].isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[1] is empty");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // must provide parent type
            return traceState;
        }

        ParentType parentType = ParentType.getParentTypeFromValue(Integer.parseInt(traceFields[1]));
        if (parentType == null || parentType.value < ParentType.App.value || parentType.value > ParentType.Mobile.value) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[1] (parentType) is unsupported");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // the provided parentType value was not one of the values we support
            return traceState;
        }

        String accountId = traceFields[2];
        if (accountId.isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[2] is empty");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // must provide account id
            return traceState;
        }

        String applicationId = traceFields[3];
        if (applicationId.isEmpty()) {
            Agent.LOG.log(Level.INFO, "DTTrace: traceFields[3] is empty");
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            // must provide account id
            return traceState;
        }

        // need extra tests
        Sampled sampled = Sampled.parse(traceFields[6]);

        Float priority;
        try {
            priority = !traceFields[7].isEmpty() ? Float.parseFloat(traceFields[7]) : null;
        } catch (NumberFormatException ignored) {
            priority = null;
        }

        String guid = traceFields[4];
        String txnId = !traceFields[5].isEmpty() ? traceFields[5] : null;
        String unparsedTimestamp = traceFields[8];

        Agent.LOG.log(Level.INFO, "DTTrace: guid: {0}  txnId: {1}  unparsedTimestamp: {2}", guid, txnId, unparsedTimestamp);

        if (unparsedTimestamp.isEmpty()) {
            // must provide timestamp
            NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_NR_ENTRY);
            return traceState;
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(unparsedTimestamp);
            // if timestamp is in the future, discard ?
        } catch (NumberFormatException ignored) {
            return traceState;
        }

        return new W3CTraceState(traceStateHeaders, vendorStates, true, version, trustKey, parentType, accountId, applicationId,
                guid, txnId, sampled, priority, timestamp);

    }

    static List<String> truncateVendorStates(List<String> vendorStates) {
        if (vendorStates.size() <= MAX_VENDOR_STATE_SIZE) {
            return vendorStates;
        }
        NewRelic.incrementCounter(MetricNames.SUPPORTABILITY_TRACE_CONTEXT_INVALID_STATE_VENDOR_COUNT);
        int longTraceStatesToKeep = countLongStatesToKeep(vendorStates);
        return truncateVendorStates(vendorStates, longTraceStatesToKeep);
    }

    private static List<String> truncateVendorStates(List<String> vendorStates, int longTraceStatesToKeep) {
        List<String> newStates = new ArrayList<>(MAX_VENDOR_STATE_SIZE);
        for (String vendor : vendorStates) {
            if (vendor.length() <= LONG_VENDOR_STATE_SIZE) {
                newStates.add(vendor);
            } else if (longTraceStatesToKeep > 0) {
                newStates.add(vendor);
                longTraceStatesToKeep--;
            }
            if (newStates.size() >= MAX_VENDOR_STATE_SIZE) {
                return newStates;
            }
        }
        return newStates;
    }

    private static int countLongStatesToKeep(List<String> vendorStates) {
        int numberOfLongStates = 0;
        for (String vendorState : vendorStates) {
            if (vendorState.length() > LONG_VENDOR_STATE_SIZE) {
                numberOfLongStates++;
            }
        }
        return MAX_VENDOR_STATE_SIZE - (vendorStates.size() - numberOfLongStates);
    }

    private static VendorStateResult flattenVendorStatesAndExtractNrState(List<String> traceStateHeaders, String agentTrustKey) {
        List<String> vendorStates = new LinkedList<>();
        String nrState = null;
        for (String header : traceStateHeaders) {
            String[] splitVendors = header.split(W3C_TRACE_STATE_VENDOR_DELIMITER);
            for (String vendor : splitVendors) {
                String trimmedVendor = vendor.trim();
                if (trimmedVendor.isEmpty()) {
                    continue;
                }
                if (trimmedVendor.contains(NR_VENDOR)) {
                    // Pull out and remove the NR vendor state from the list of states if the trust key matches
                    if (trimmedVendor.startsWith(agentTrustKey + NR_VENDOR)) {
                        nrState = trimmedVendor;
                        continue;
                    }
                }
                vendorStates.add(trimmedVendor);
            }
        }

        List<String> vendorKeys = vendorStatesToVendorKeys(vendorStates);
        if (containsDuplicates(vendorKeys) || anyVendorStateIsInvalid(vendorStates)) {
            return new VendorStateResult(Collections.<String>emptyList(), nrState);
        }

        return new VendorStateResult(vendorStates, nrState);
    }

    static String concatenateVendorStates(List<String> vendorStates) {
        return Joiner.on(W3C_TRACE_STATE_VENDOR_DELIMITER).join(vendorStates);
    }

    public static Set<String> buildVendorKeys(W3CTraceState state) {
        List<String> vendorStates = state.getVendorStates();
        List<String> vendorKeys = vendorStatesToVendorKeys(vendorStates);
        if (containsDuplicates(vendorKeys) || anyVendorStateIsInvalid(vendorStates)) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(vendorKeys);
    }

    private static List<String> vendorStatesToVendorKeys(List<String> vendorStates) {
        List<String> vendorKeys = new LinkedList<>();
        for (String vendorState : vendorStates) {
            String vendorKey = vendorState.split(VENDOR_STATE_KEY_VALUE_DELIMITER)[0];
            vendorKeys.add(vendorKey);
        }
        return vendorKeys;
    }

    private static boolean containsDuplicates(List<String> list) {
        Set<String> set = new HashSet<>(list);
        return set.size() != list.size();
    }

    private static boolean anyVendorStateIsInvalid(List<String> vendorStates) {
        for (String vendorState : vendorStates) {
            if (!VENDOR_STATE_PATTERN.matcher(vendorState).matches()) {
                return true;
            }
        }
        return false;
    }

}
