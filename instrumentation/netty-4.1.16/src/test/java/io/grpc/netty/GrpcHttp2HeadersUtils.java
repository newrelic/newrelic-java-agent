/*
 *
 *  * Copyright 2026 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package io.grpc.netty;

import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.util.AsciiString;

/**
 * Test-only fixtures that mirror the class names and AsciiString-key restriction of
 * grpc-netty's real io.grpc.netty.GrpcHttp2HeadersUtils inbound header implementations,
 * without pulling in a grpc-netty dependency.
 */
public class GrpcHttp2HeadersUtils {

    private static void requireAsciiString(CharSequence name) {
        if (!(name instanceof AsciiString)) {
            throw new IllegalArgumentException("AsciiString expected. Was: " + name.getClass().getName());
        }
    }

    public static final class GrpcHttp2RequestHeaders extends DefaultHttp2Headers {
        @Override
        public CharSequence get(CharSequence name) {
            requireAsciiString(name);
            return super.get(name);
        }

        @Override
        public java.util.List<CharSequence> getAll(CharSequence name) {
            requireAsciiString(name);
            return super.getAll(name);
        }

        @Override
        public boolean contains(CharSequence name) {
            requireAsciiString(name);
            return super.contains(name);
        }
    }

    public static final class GrpcHttp2ResponseHeaders extends DefaultHttp2Headers {
        @Override
        public CharSequence get(CharSequence name) {
            requireAsciiString(name);
            return super.get(name);
        }

        @Override
        public java.util.List<CharSequence> getAll(CharSequence name) {
            requireAsciiString(name);
            return super.getAll(name);
        }

        @Override
        public boolean contains(CharSequence name) {
            requireAsciiString(name);
            return super.contains(name);
        }

        @Override
        public CharSequence method() {
            throw new UnsupportedOperationException("method() is not available on a response");
        }

        @Override
        public CharSequence path() {
            throw new UnsupportedOperationException("path() is not available on a response");
        }

        @Override
        public CharSequence authority() {
            throw new UnsupportedOperationException("authority() is not available on a response");
        }
    }
}
