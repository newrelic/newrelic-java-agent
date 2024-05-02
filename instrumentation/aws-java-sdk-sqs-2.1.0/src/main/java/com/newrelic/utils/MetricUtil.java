/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.newrelic.utils;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.util.AwsAccountUtil;
import com.newrelic.api.agent.DestinationType;
import com.newrelic.api.agent.MessageConsumeParameters;
import com.newrelic.api.agent.MessageProduceParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.regions.Region;

import java.util.logging.Level;

public class MetricUtil {

    public static final String LIBRARY = "SQS";
    public static final String UNKNOWN = "unknown";

    public static MessageProduceParameters generateExternalProduceMetrics(String queueUrl, SdkClientConfiguration clientConfiguration) {
        String queueName = UNKNOWN;
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) {
            queueName = queueUrl.substring(index + 1);
        }

        String arn = null;
        if (clientConfiguration != null) {
            arn = getArn(queueName, clientConfiguration);
        }

        MessageProduceParameters params = MessageProduceParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName)
                .outboundHeaders(null)
                .cloudResourceId(arn)
                .build();
        return params;
    }

    public static MessageConsumeParameters generateExternalConsumeMetrics(String queueUrl, SdkClientConfiguration clientConfiguration) {
        String queueName = UNKNOWN;
        int index = queueUrl.lastIndexOf('/');
        if (index > 0) {
            queueName = queueUrl.substring(index + 1);
        }

        String arn = null;
        if (clientConfiguration != null) {
            arn = getArn(queueName, clientConfiguration);
        }

        MessageConsumeParameters params = MessageConsumeParameters
                .library(LIBRARY)
                .destinationType(DestinationType.NAMED_QUEUE)
                .destinationName(queueName)
                .inboundHeaders(null)
                .cloudResourceId(arn)
                .build();
        return params;
    }

    private static String getArn(String queueName, SdkClientConfiguration clientConfiguration) {
        if (queueName == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Queue name is null.");
            return null;
        }
        AwsCredentialsProvider credentialsProvider = clientConfiguration.option(AwsClientOption.CREDENTIALS_PROVIDER);
        if (credentialsProvider == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Credentials provider is null.");
            return null;
        }
        Region region = clientConfiguration.option(AwsClientOption.AWS_REGION);
        if (region == null || region.id() == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Region is null.");
            return null;
        }
        String accessKey = credentialsProvider.resolveCredentials().accessKeyId();
        if (accessKey == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Access key is null.");
            return null;
        }
        Long accountId = AwsAccountUtil.get().decodeAccount(accessKey);
        if (accountId == null) {
            NewRelic.getAgent().getLogger().log(Level.FINEST, "Unable to assemble ARN. Unable to decode account.");
            return null;
        }
        // arn:${Partition}:sqs:${Region}:${Account}:${QueueName}
        return "arn:aws:sqs:" + region.id() + ":" + accountId + ":" + queueName;
    }

    public static void finishSegment(Segment segment) {
        try {
            segment.end();
        } catch (Throwable t) {
            AgentBridge.instrumentation.noticeInstrumentationError(t, Weaver.getImplementationTitle());
        }
    }
}
