/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent;

import com.google.gson.Gson;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.TransportType;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.Map;
import java.util.logging.Level;

import static com.newrelic.agent.HeadersUtil.parseAndAcceptDistributedTraceHeaders;

/**
 * Instances of this immutable class represent the NewRelic-specific inbound headers for this transaction. This class is thread safe.
 */
public class InboundHeaderState {
    private static final String CONTENT_LENGTH_REQUEST_HEADER = "Content-Length";
    private static final String NEWRELIC_ID_HEADER_SEPARATOR = "#";
    private static final int CURRENT_SYNTHETICS_VERSION = 1;

    private final Transaction tx;
    private final InboundHeaders inboundHeaders; // this value can be null
    private final CatState catState;
    private final SyntheticsState synState;
    private final SyntheticsInfoState synInfoState;

    /**
     * Create this inbound header state object.
     * <p>
     * Implementation note: the interface {@link Request} extends {@link InboundHeaders}. If the calling class has a
     * Request object, the Request should be passed directly; code in this class may test the type of the inboundHeaders
     * argument and provide more sophisticated processing if the InboundHeaders are in fact a Request.
     * <p>
     * We go to extra pain to code this in a way that allows all the state in this class to be final.
     *
     * @param tx our owning Transaction. May not be null.
     * @param inboundHeaders the deobfuscated inbound headers. This value may be null.
     */
    public InboundHeaderState(Transaction tx, InboundHeaders inboundHeaders) {
        this.tx = tx;
        this.inboundHeaders = inboundHeaders;

        if (inboundHeaders == null) {
            this.synState = SyntheticsState.NONE;
            this.synInfoState = SyntheticsInfoState.NONE;
            this.catState = CatState.NONE;
        } else {
            this.synState = parseSyntheticsHeader();
            if (inboundHeaders.getHeader("X-NewRelic-Synthetics-Info") == null) {
                this.synInfoState = SyntheticsInfoState.NONE;
            } else {
                this.synInfoState = parseSyntheticsInfoHeader();
            }
            if (tx.getAgentConfig().getDistributedTracingConfig().isEnabled() && tx.getSpanProxy().getInboundDistributedTracePayload() == null) {
                parseDistributedTraceHeaders();
                this.catState = CatState.NONE;
            } else if (tx.getCrossProcessConfig().isCrossApplicationTracing()) {
                this.catState = parseCatHeaders();
            } else {
                this.catState = CatState.NONE;
            }
        }
    }

    /**
     * Get the encoded, unparsed synthetics header from the inbound request.
     * <p>
     * This method is public because the inbound synthetic header is copied directly to outbound CAT requests. There's
     * no need to parse and regenerate the header in these cases.
     *
     * @return the synthetics header from the inbound request or null if there is no such header. Also returns null if
     * synthetics is globally disabled.
     */
    public String getUnparsedSyntheticsHeader() {
        String result = null;
        if (inboundHeaders != null) {
            result = HeadersUtil.getSyntheticsHeader(inboundHeaders);
        }
        return result;
    }

    private SyntheticsState parseSyntheticsHeader() {
        String synHeader = getUnparsedSyntheticsHeader();
        if (synHeader == null || synHeader.length() == 0) {
            return SyntheticsState.NONE;
        }

        // If CAT is disabled, we cannot access the decoding key via the CrossApplicationConfig object.
        // But parsing and storing the key from the server is never really disabled (and there is a unit
        // test to verify this) so we can always access the key via the flat config.

        JSONArray arr = getJSONArray(synHeader);
        if (arr == null || arr.size() == 0) {
            Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: unable to decode header in transaction {0}.", tx);
            return SyntheticsState.NONE;
        }

        Agent.LOG.log(Level.FINEST, "Decoded synthetics header => {0} in transaction {1}", arr, tx);

        Integer version = null;
        try {
            version = Integer.parseInt(arr.get(0).toString());
        } catch (NumberFormatException nfe) {
            Agent.LOG.log(Level.FINEST, "Could not determine synthetics version. Value => {0}. Class => {1}.",
                    arr.get(0), arr.get(0).getClass());
            return SyntheticsState.NONE;
        }

        if (version > CURRENT_SYNTHETICS_VERSION) {
            Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: invalid version {0} in transaction {1}", version, tx);
            return SyntheticsState.NONE;
        }

        SyntheticsState result;

        try {
            // Sample synthetics header:
            // [1,417446,"fd09bfa1-bd85-4f8a-9bee-8d51582f5a54","77cbc5dc-327b-4542-90f0-335644134bed","3e5c28ac-7cf3-4faf-ae52-ff36bc93504a"]
            result = new SyntheticsState(version, (Number) arr.get(1), (String) arr.get(2), (String) arr.get(3), (String) arr.get(4));
        } catch (RuntimeException rex) { // class cast exception, not enough elements in the JSON array, etc.
            Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: while parsing header: {0}: {1} in transaction {2}",
                    rex.getClass().getSimpleName(), rex.getLocalizedMessage(), tx);
            result = SyntheticsState.NONE;
        }

        return result;
    }

    public String getUnparsedSyntheticsInfoHeader() {
        String result = null;
        if (inboundHeaders != null) {
            result = HeadersUtil.getSyntheticsInfoHeader(inboundHeaders);
        }
        return result;
    }

    private SyntheticsInfoState parseSyntheticsInfoHeader() {
        String synInfoHeader = getUnparsedSyntheticsInfoHeader();
        if (synInfoHeader == null || synInfoHeader.isEmpty()) {
            return SyntheticsInfoState.NONE;
        }

        Map<String, Object> jsonMap = getJSONMap(synInfoHeader);

        if (jsonMap == null || synInfoHeader.isEmpty()) {
            Agent.LOG.log(Level.FINE, "Synthetic Info transaction tracing failed: unable to decode header " +
                    "in transaction {0}.", tx);
            return SyntheticsInfoState.NONE;
        }

        Agent.LOG.log(Level.FINEST, "Decoded synthetics info header => {0} in transaction {1}", jsonMap, tx);

        String type;
        try {
            type = (String) jsonMap.get("type");
        } catch (NumberFormatException nfe) {
            Agent.LOG.log(Level.FINEST, "Could not determine synthetics-info type.",
                    jsonMap.get("type"), jsonMap.get("type").getClass());
            return SyntheticsInfoState.NONE;
        }

        SyntheticsInfoState result;

        try {
            // Sample synthetics-info header:
            // {"version":"1", "type":"scheduled", "initiator":"cli", "attributes": "{"keyOne":"valueOne",
            // "keyTwo":"valueTwo", "keyThree":"valueThree" }"}
            result = new SyntheticsInfoState((String) jsonMap.get("version"), (String) jsonMap.get("type"),
                    (String) jsonMap.get("initiator"), (Map) jsonMap.get("attributes"));
        } catch (RuntimeException rex) { // class cast exception, not enough elements in the JSON map, etc.
            Agent.LOG.log(Level.FINE, "Synthetic transaction tracing failed: while parsing header: {0}: " +
                            "{1} in transaction {2}",
            rex.getClass().getSimpleName(), rex.getLocalizedMessage(), tx);
            result = SyntheticsInfoState.NONE;
        }

        return result;
    }

    private CatState parseCatHeaders() {
        String clientCrossProcessID = HeadersUtil.getIdHeader(inboundHeaders);
        if (clientCrossProcessID == null || tx.isIgnore()) {
            return CatState.NONE;
        }

        if (!tx.getCrossProcessConfig().isCrossApplicationTracing()) {
            return CatState.NONE;
        }

        if (!isClientCrossProcessIdTrusted(clientCrossProcessID)) {
            return CatState.NONE;
        }

        Agent.LOG.log(Level.FINEST, "Client cross process id is {0} in transaction {1} ", clientCrossProcessID, tx);

        String transactionHeader = HeadersUtil.getTransactionHeader(inboundHeaders);
        JSONArray arr = getJSONArray(transactionHeader);
        if (arr == null) {
            return new CatState(clientCrossProcessID, null, Boolean.FALSE, null, null);
        }

        return new CatState(clientCrossProcessID, (arr.size() >= 1) ? (String) arr.get(0) : null,
                (arr.size() >= 2) ? (Boolean) arr.get(1) : null, (arr.size() >= 3) ? (String) arr.get(2) : null,
                (arr.size() >= 4) ? ServiceUtils.hexStringToInt((String) arr.get(3)) : null);
    }

    private void parseDistributedTraceHeaders() {
        //Don't override TransportType if it has already been set
        if (TransportType.Unknown.equals(tx.getTransportType())) {
            TransportType transportType = inboundHeaders.getHeaderType() == HeaderType.MESSAGE ?
                    TransportType.JMS : TransportType.HTTP;
            tx.setTransportType(transportType);
        }
        parseAndAcceptDistributedTraceHeaders(tx, inboundHeaders);
    }

    // Public interface - getters for the various information parsed from the headers

    /**
     * Get the version. Callers should generally use {@link #isSupportedSyntheticsVersion} rather than calling here and
     * then performing their own checks on supported versions. External caller <b>must</b> check validity of the version
     * or customer privacy may be compromised. Currently, the only callers outside this class are non-shipping tests.
     *
     * @return the Synthetics version from the request headers, or {@link HeadersUtil#SYNTHETICS_VERSION_NONE}.
     */
    public int getSyntheticsVersion() {
        Integer obj = synState.getVersion();
        if (obj == null) {
            return HeadersUtil.SYNTHETICS_VERSION_NONE;
        }

        int version = obj;
        if (version < 0) {
            // regularize bogus negative value
            return HeadersUtil.SYNTHETICS_VERSION_NONE;
        }

        return version;
    }

    /**
     * Return true if the request contains a synthetics header with a supported version. This method does not check
     * whether the Account ID is trusted.
     *
     * @return true if the request contains a synthetics header with a supported version.
     */
    private boolean isSupportedSyntheticsVersion() {
        int version = getSyntheticsVersion();
        return version >= HeadersUtil.SYNTHETICS_MIN_VERSION && version <= HeadersUtil.SYNTHETICS_MAX_VERSION;
    }

    /**
     * Return true if this is a trusted request from New Relic Synthetics. The request is trusted if the calling ID can
     * be decoded to a non-null value and the version in the header is supported.
     * <p>
     * Implementation note: None of the callers ever care what the actual value of the Synthetics account ID is, and
     * hence this boolean accessor instead of a standard getter. The same underlying "trust logic" is used for both CAT
     * and Synthetics.
     *
     * @return true if this is a trusted request from New Relic Synthetics.
     */
    public boolean isTrustedSyntheticsRequest() {
        return isSupportedSyntheticsVersion() && synState.getAccountId() != null;
    }

    public String getSyntheticsResourceId() {
        return synState.getSyntheticsResourceId();
    }

    public String getSyntheticsJobId() {
        return synState.getSyntheticsJobId();
    }

    public String getSyntheticsMonitorId() {
        return synState.getSyntheticsMonitorId();
    }

    public String getSyntheticsType() {
        return synInfoState.getSyntheticsType();
    }

    public String getSyntheticsInitiator() {
        return synInfoState.getSyntheticsInitiator();
    }

    public Map<String, String> getSyntheticsAttrs() {
        return synInfoState.getSyntheticsAttributes();
    }



    /**
     * Return true if this is a trusted cross-process request.
     * <p>
     * Implementation note: the request is trusted if the calling ID can be decoded to a non-null value. Most callers
     * don't care what the actual value of the ID is, and hence this boolean accessor in addition to a String getter.
     * The same underlying "trust logic" is used for both CAT and Synthetics.
     *
     * @return true if this is a trusted cross-process request.
     */
    public boolean isTrustedCatRequest() {
        return catState.getClientCrossProcessId() != null;
    }

    public String getClientCrossProcessId() {
        return catState.getClientCrossProcessId();
    }

    public String getReferrerGuid() {
        return catState.getReferrerGuid();
    }

    public boolean forceTrace() {
        return catState.forceTrace();
    }

    public Integer getReferringPathHash() {
        return catState.getReferringPathHash();
    }

    public String getInboundTripId() {
        return catState.getInboundTripId();
    }

    /**
     * It's not always possible for clients to determine the number of bytes sent, so reply with the number of bytes
     * received.
     *
     * @return the value of the Content-Length request header, or -1 if none
     */
    public long getRequestContentLength() {
        long contentLength = -1;
        String contentLengthString = inboundHeaders == null ? null
                : inboundHeaders.getHeader(CONTENT_LENGTH_REQUEST_HEADER);
        if (contentLengthString != null) {
            try {
                contentLength = Long.parseLong(contentLengthString);
            } catch (NumberFormatException e) {
                Agent.LOG.log(Level.FINER, "Error parsing {0} response header: {1}: {2} in transaction {3}",
                        CONTENT_LENGTH_REQUEST_HEADER, contentLengthString, e, tx);
            }
        }
        return contentLength;
    }

    private boolean isClientCrossProcessIdTrusted(String clientCrossProcessId) {
        String accountId = getAccountId(clientCrossProcessId);
        if (accountId != null) {
            if (tx.getCrossProcessConfig().isTrustedAccountId(accountId)) {
                return true;
            }
            Agent.LOG.log(Level.FINEST, "Account id {0} in client cross process id {1} is not trusted in transaction {2}",
                    accountId, clientCrossProcessId, tx);
        } else {
            Agent.LOG.log(Level.FINER, "Account id not found in client cross process id {0} in transaction {1}",
                    clientCrossProcessId, tx);
        }
        return false;
    }

    /**
     * Get the account id from the client cross process id. The account id is everything before the
     * {@value #NEWRELIC_ID_HEADER_SEPARATOR}.
     *
     * @return the account id or null if not found
     */
    private String getAccountId(String clientCrossProcessId) {
        String accountId = null;
        int index = clientCrossProcessId.indexOf(NEWRELIC_ID_HEADER_SEPARATOR);
        if (index > 0) {
            accountId = clientCrossProcessId.substring(0, index);
        }
        return accountId;
    }

    private JSONArray getJSONArray(String json) {
        JSONArray result = null;
        if (json != null) {
            try {
                JSONParser parser = new JSONParser();
                result = (JSONArray) parser.parse(json);
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINER, "Unable to parse header in transaction {0}: {1}", tx, ex);
            }
        }
        return result;
    }

    private Map<String, Object> getJSONMap(String string) {
        Map<String, Object> result = null;
        if (string != null) {
            try {
                Gson gson = new Gson();
                result = gson.fromJson(string, Map.class);
            } catch (Exception ex) {
                Agent.LOG.log(Level.FINER, "Unable to parse header in transaction {0}: {1}", tx, ex);
            }
        }
        return result;
    }



    /**
     * Instances of this immutable class represent the CAT header state of this transaction.
     */
    static final class CatState {
        private final String clientCrossProcessId;
        private final String referrerGuid;
        private final Boolean forceTrace;
        private final String inboundTripId;
        private final Integer referringPathHash;

        CatState(String clientCrossProcessId, String referrerGuid, Boolean forceTrace, String inboundTripId,
                Integer referringPathHash) {
            this.clientCrossProcessId = clientCrossProcessId;
            this.referrerGuid = referrerGuid;
            this.forceTrace = forceTrace;
            this.inboundTripId = inboundTripId;
            this.referringPathHash = referringPathHash;
        }

        static final CatState NONE = new CatState(null, null, Boolean.FALSE, null, null);

        String getClientCrossProcessId() {
            return clientCrossProcessId;
        }

        String getReferrerGuid() {
            return referrerGuid;
        }

        boolean forceTrace() {
            return forceTrace;
        }

        String getInboundTripId() {
            return inboundTripId;
        }

        Integer getReferringPathHash() {
            return referringPathHash;
        }
    }

    /**
     * Instances of this immutable class represent the synthetics header state of this transaction.
     */
    static final class SyntheticsState {
        /*-
         * From the spec ("Agent Support for Synthetics" in Confluence)
         *
         * version (Number) is the protocol version for future improvements (currently 1)
         * accountId (Number) is the APM account id of the owner of the web application being monitored.
         * syntheticsResourceId (String) is a unique identifier of the synthetic request
         * syntheticsJobId (String) is a unique identifier of the synthetic job that generated this request
         * syntheticsMonitorId (String) is a unique identifier of the synthetic monitor that generated this job
         */
        private final Integer version;
        private final Number accountId;
        private final String syntheticsResourceId;
        private final String syntheticsJobId;
        private final String syntheticsMonitorId;

        /**
         * This object has will appear untrusted, shutting off all synthetics behaviors here.
         */
        static final SyntheticsState NONE = new SyntheticsState(null, null, null, null, null);

        SyntheticsState(Integer version, Number accountId, String syntheticsResourceId, String syntheticsJobId,
                String syntheticsMonitorId) {
            this.version = version;
            this.accountId = accountId;
            this.syntheticsResourceId = syntheticsResourceId;
            this.syntheticsJobId = syntheticsJobId;
            this.syntheticsMonitorId = syntheticsMonitorId;
        }

        Integer getVersion() {
            return version;
        }

        Number getAccountId() {
            return accountId;
        }

        String getSyntheticsResourceId() {
            return syntheticsResourceId;
        }

        String getSyntheticsJobId() {
            return syntheticsJobId;
        }

        String getSyntheticsMonitorId() {
            return syntheticsMonitorId;
        }
    }

    static final class SyntheticsInfoState {
        /*-
         * From the spec ("Agent Support for Synthetics" in Confluence)
         *
         * version (String) is the protocol version for future improvements (currently 1)
         * type (String) is the type of the synthetics test. May be one of 'scheduled', 'automatedTest', or additional
         *                  types that will be introduced in the future (such as load testing)
         * initiator (String) is the source of the synthetics test. May be 'graphql', 'cli', or specific name of
         *                  CI integration tools
         * attributes (map) are the additional attributes that may or may not be present in any specific synthetics
         *                  event
         */
        private final String syntheticsVersion;
        private final String syntheticsType;
        private final String syntheticsInitiator;
        private final Map<String, String> syntheticsAttributes;

        /**
         * This object will appear untrusted, shutting off all synthetics behaviors here.
         */
        static final SyntheticsInfoState NONE = new SyntheticsInfoState(null, null, null, null);

        SyntheticsInfoState(String syntheticsVersion, String syntheticsType, String syntheticsInitiator, Map<String, String> syntheticsAttributes) {
            this.syntheticsVersion = syntheticsVersion;
            this.syntheticsType = syntheticsType;
            this.syntheticsInitiator = syntheticsInitiator;
            this.syntheticsAttributes = syntheticsAttributes;
        }

        String getSyntheticsVersion() {
            return this.syntheticsVersion;
        }

        String getSyntheticsType() {
            return this.syntheticsType;
        }

        String getSyntheticsInitiator() {
            return this.syntheticsInitiator;
        }

        Map<String, String> getSyntheticsAttributes() {
            return this.syntheticsAttributes;
        }

    }

}
