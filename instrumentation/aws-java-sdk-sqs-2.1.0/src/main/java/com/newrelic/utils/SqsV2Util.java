/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class SqsV2Util {

    public static final String LIBRARY = "SQS";
    public static final String OTEL_LIBRARY = "aws_sqs";
    public static final String[] DT_HEADERS = new String[] {"newrelic","NEWRELIC","NewRelic","tracestate","TraceState","TRACESTATE"};

    /*
    5000 bytes is larger than the actual size of new relic headers but setting it to that value
    makes it so exceptionally large messages will not have distributed tracing
    */
    public static int NR_HEADERS_BYTES_SIZE = 5000;

    public static int SDK_MAX_MESSAGE_BYTES_SIZE = 262144; // 262144 bytes
    public static int DT_MAX_MESSAGE_BYTES_SIZE = SDK_MAX_MESSAGE_BYTES_SIZE - NR_HEADERS_BYTES_SIZE;

    public static MessageProduceParameters generateExternalProduceMetrics(String queueUrl) {
        DestinationData destinationData = DestinationData.parse(queueUrl);

        MessageProduceParameters params = MessageProduceParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(destinationData.getQueueName())
                .outboundHeaders(null)
                .cloudRegion(destinationData.getRegion())
                .cloudAccountId(destinationData.getAccountId())
                .build();
        return params;
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(String queueUrl) {
        DestinationData destinationData = DestinationData.parse(queueUrl);

        MessageConsumeParameters params = MessageConsumeParameters
                .library(LIBRARY, OTEL_LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(destinationData.getQueueName())
                .inboundHeaders(null)
                .cloudRegion(destinationData.getRegion())
                .cloudAccountId(destinationData.getAccountId())
                .build();
        return params;
    }

    public static void finishSegment(Segment segment) {
        try {
            segment.end();
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
    }

    public static boolean canAddDtHeaders(SendMessageRequest message) {
        int bodySize = message.messageBody() != null ? message.messageBody().getBytes().length : 0;
        if (message.messageAttributes().size() > 8) {
            return false;
        }
        int attributesBytesSize = attributesBytesSize(message.messageAttributes().entrySet());
        int messageBytesSize = bodySize + attributesBytesSize;
        return messageBytesSize < DT_MAX_MESSAGE_BYTES_SIZE;
    }

    public static boolean canAddDtHeaders(SendMessageBatchRequestEntry message) {
        int bodyBytesSize = message.messageBody() != null ? message.messageBody().getBytes().length : 0;
        int messageAttributesCount = message.messageAttributes().size();
        if (messageAttributesCount > 8) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST, "SQS message has too many attributes for distributed tracing. " +
                    "The maximum limit is 8 and the message has {0} attributes.", messageAttributesCount);
            return false;
        }
        int attributesBytesSize = attributesBytesSize(message.messageAttributes().entrySet());
        int messageBytesSize = bodyBytesSize + attributesBytesSize;
        boolean messageWithinSizeLimits = messageBytesSize < DT_MAX_MESSAGE_BYTES_SIZE;
        if(!messageWithinSizeLimits) {
            AgentBridge.getAgent().getLogger().log(Level.FINEST,
                    "SQS message is too large for distributed tracing. The message body has {0} bytes. " +
                            "The total number of bytes within attributes is {1} bytes. Total of both is {2} bytes.",
                    bodyBytesSize, attributesBytesSize, messageBytesSize);

        }
        return messageWithinSizeLimits;
    }

    public static int attributesBytesSize(Set<Map.Entry<String, MessageAttributeValue>> attributes) {
        int totalAttributeSize = 0;
        for (Map.Entry<String, MessageAttributeValue> attr : attributes) {
            totalAttributeSize += attr.getKey().getBytes().length;
            String stringValue = attr.getValue().stringValue();
            if (stringValue != null) {
                totalAttributeSize += stringValue.getBytes().length;
                continue;
            }
            SdkBytes sdkBytes = attr.getValue().binaryValue();
            if (sdkBytes != null) {
                totalAttributeSize += sdkBytes.asByteArray().length;
                continue;
            }
            List<SdkBytes> sdkBytesList = attr.getValue().binaryListValues();
            if (sdkBytesList != null) {
                for (SdkBytes bytesEntry : sdkBytesList) {
                    if (bytesEntry != null) {
                        totalAttributeSize += bytesEntry.asByteArray().length;
                    }
                }
            }
        }
        return totalAttributeSize;
    }

}
