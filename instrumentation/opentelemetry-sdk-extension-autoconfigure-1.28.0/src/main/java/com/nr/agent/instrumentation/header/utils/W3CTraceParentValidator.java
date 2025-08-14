/*
 *
 *  * Copyright 2025 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.header.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nr.agent.instrumentation.header.utils.W3CTraceParentHeader.W3C_VERSION;
import static java.util.regex.Pattern.compile;

public class W3CTraceParentValidator {

    private static final String INVALID_VERSION = "ff";
    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000"; // 32 characters
    private static final String INVALID_PARENT_ID = "0000000000000000"; // 16 characters
    private static final Pattern HEXADECIMAL_PATTERN = compile("\\p{XDigit}+");

    private final String traceParentHeader;
    private final String version;
    private final String traceId;
    private final String parentId;
    private final String flags;

    private W3CTraceParentValidator(Builder builder) {
        this.traceParentHeader = builder.traceParentHeader;
        this.version = builder.version;
        this.traceId = builder.traceId;
        this.parentId = builder.parentId;
        this.flags = builder.flags;
    }

    private boolean isValid() {
        return isValidVersion() && isValidTraceId() && isValidParentId() && isValidFlags();
    }

    /**
     * Version can only be 2 hexadecimal characters, `ff` is not allowed and if it matches our expected version the length must be 55 characters
     */
    boolean isValidVersion() {
        return version.length() == 2 && isHexadecimal(version.charAt(0)) && isHexadecimal(version.charAt(1)) && !version.equals(INVALID_VERSION) &&
                !(version.equals(W3C_VERSION) && traceParentHeaderLengthIsInvalid());
    }

    private boolean traceParentHeaderLengthIsInvalid() {
        return traceParentHeader.length() != 55;
    }

    boolean isHexadecimal(char character) {
        return Character.digit(character, 16) != -1;
    }

    boolean isHexadecimal(String input) {
        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(input);
        return matcher.matches();
    }

    /**
     * TraceId must be 32 characters, not all zeros and must be hexadecimal
     */
    boolean isValidTraceId() {
        return traceId.length() == 32 && !traceId.equals(INVALID_TRACE_ID) && isHexadecimal(traceId);
    }

    /**
     * ParentId must be 16 characters, not all zeros and must be hexadecimal
     */
    boolean isValidParentId() {
        return parentId.length() == 16 && !parentId.equals(INVALID_PARENT_ID) && isHexadecimal(parentId);
    }

    /**
     * Flags must be 2 characters and must be hexadecimal
     */
    boolean isValidFlags() {
        return flags.length() == 2 && isHexadecimal(flags);
    }

    static Builder forHeader(String traceParentHeader) {
        return new Builder(traceParentHeader);
    }

    static class Builder {
        private final String traceParentHeader;
        private String version;
        private String traceId;
        private String parentId;
        private String flags;

        Builder(String traceParentHeader) {
            this.traceParentHeader = traceParentHeader;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder flags(String flags) {
            this.flags = flags;
            return this;
        }

        W3CTraceParentValidator build() {
            return new W3CTraceParentValidator(this);
        }

        public boolean isValid() {
            return build().isValid();
        }
    }
}
