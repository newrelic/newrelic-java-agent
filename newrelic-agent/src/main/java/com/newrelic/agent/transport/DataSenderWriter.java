/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.agent.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import com.newrelic.agent.service.ServiceFactory;
import org.json.simple.JSONValue;

/**
 * A Writer used as a marker to indicate that JSON strings should not be base 64 encoded.
 * 
 * {@link org.json.simple.JSONStreamAware} objects should call {@link #getJsonifiedOptionallyCompressedEncodedString(Object, Writer)}
 */
public class DataSenderWriter extends OutputStreamWriter {

    private static final int COMPRESSION_LEVEL = Deflater.DEFAULT_COMPRESSION;

    protected DataSenderWriter(OutputStream out) throws UnsupportedEncodingException {
        super(out, "UTF-8");
    }

    public static final String nullValue() {
        return "null";
    }

    public static final boolean isCompressingWriter(Writer writer) {
        return !(writer instanceof DataSenderWriter);
    }

    /**
     * Returns a JSON string that will be deflate compressed and base 64 encoded if {@link com.newrelic.agent.config.AgentConfigImpl#SIMPLE_COMPRESSION_PROPERTY}
     * is disabled (default). The JSON string will be returned raw and uncompressed otherwise.
     * 
     * @param data the data to be serialized to JSON
     * @param writer the writer to use
     * @return the compressed or uncompressed json value for the data passed in
     */
    public static Object getJsonifiedOptionallyCompressedEncodedString(Object data, Writer writer) {
        return getJsonifiedOptionallyCompressedEncodedString(data, writer, COMPRESSION_LEVEL);
    }

    /**
     * Returns a JSON string that will be deflate compressed and base 64 encoded if {@link com.newrelic.agent.config.AgentConfigImpl#SIMPLE_COMPRESSION_PROPERTY}
     * is disabled (default). The JSON string will be returned raw and uncompressed otherwise.
     *
     * @param data the data to be serialized to JSON
     * @param writer the writer to use
     * @param compressionLevel the compression level to use (if the data is compressed)
     * @return the compressed or uncompressed json value for the data passed in
     */
    public static Object getJsonifiedOptionallyCompressedEncodedString(Object data, Writer writer,
            int compressionLevel) {
        if (writer instanceof DataSenderWriter) {
            return toJSONString(data);
        }
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().isSimpleCompression()) {
            return data;
        }

        return getJsonifiedCompressedEncodedString(data, compressionLevel);
    }

    /**
     * Returns a JSON string that will be deflate compressed and base 64 encoded if {@link com.newrelic.agent.config.AgentConfigImpl#SIMPLE_COMPRESSION_PROPERTY}
     * is disabled (default). The JSON string will be returned raw and uncompressed otherwise.
     * 
     * Additionally, the payload will be compressed in all cases to compare its size against the passed in limit. If the
     * result is larger than the limit then a null value will be returned indicating that the JSON is larger than the
     * limit.
     * 
     * If the compressed result is under the limit then the simple_compression rules from the first paragraph apply and
     * either the compressed or uncompressed JSON will be returned.
     * 
     * @param data the data to be serialized to JSON
     * @param writer the writer to use
     * @param compressionLevel the compression level to use (if the data is compressed or to check size limit)
     * @param resultSizeLimitInBytes the maximum size in bytes of the compressed version of the result
     * @return null if the compressed result is over the limit, otherwise the serialized JSON will be returned.
     */
    public static Object getJsonifiedOptionallyCompressedEncodedString(Object data, Writer writer,
            int compressionLevel, int resultSizeLimitInBytes) {
        String compressedResult = getJsonifiedCompressedEncodedString(data, compressionLevel);

        if (compressedResult.length() > resultSizeLimitInBytes) {
            return null;
        }

        if (writer instanceof DataSenderWriter) {
            return toJSONString(data);
        }
        if (ServiceFactory.getConfigService().getDefaultAgentConfig().isSimpleCompression()) {
            return data;
        }

        return compressedResult;
    }

    /**
     * Get a JSON string for the object.
     */
    public static String toJSONString(Object obj) {
        ByteArrayOutputStream oStream = new ByteArrayOutputStream();
        try {
            Writer writer = new DataSenderWriter(oStream);
            JSONValue.writeJSONString(obj, writer);
            writer.close();
            return oStream.toString("UTF-8");
        } catch (IOException e) {
            return JSONValue.toJSONString(obj);
        }
    }

    /**
     * Converts data into a json string, compresses it and returns a base 64 encoded string.
     */
    public static String getJsonifiedCompressedEncodedString(Object data, int compressionLevel) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            OutputStream zipStream = new DeflaterOutputStream(outStream, new Deflater(compressionLevel));
            Writer out = new OutputStreamWriter(zipStream, StandardCharsets.UTF_8);
            JSONValue.writeJSONString(data, out);
            out.flush();
            out.close();
            outStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Base64.getEncoder().encodeToString(outStream.toByteArray());
    }

}
