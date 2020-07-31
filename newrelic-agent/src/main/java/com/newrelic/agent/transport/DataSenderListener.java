/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import java.util.Map;

/**
 * A listener that gets notified when data is sent to the collector and when data is received from the collector.
 * 
 * The most common usage of this listener would be for validating data sent to the collector in a test.
 */
public interface DataSenderListener {

    /**
     * This method is called when data has been sent to the collector and contains the raw data that was sent. If
     * the encoding is set to "identity", the data will be uncompressed. If encoding is set to "deflate", the raw
     * data will be compressed using the deflate (zip) compressor.
     * 
     * @param method the method invoked on the collector
     * @param encoding the encoding used for this request/response (identity, deflate)
     * @param uri the URI this data was sent to
     * @param rawDataSent the raw (compressed or uncompressed) payload sent to the collector
     */
    void dataSent(String method, String encoding, String uri, byte[] rawDataSent);

    /**
     * This method is called when data has been received from the collector for a given collector method invocation.
     * The response data is in the form of a map of JSON objects. The exact contents will depend on the method invoked.
     * 
     * @param method the method invoked on the collector
     * @param encoding the encoding used for this request/response (identity, deflate)
     * @param uri the URI this data was sent to
     * @param rawDataReceived the raw (marshalled to a Map) response from the collector
     */
    void dataReceived(String method, String encoding, String uri, Map<?, ?> rawDataReceived);

}
