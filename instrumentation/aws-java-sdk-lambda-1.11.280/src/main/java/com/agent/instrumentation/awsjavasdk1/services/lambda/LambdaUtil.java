/*
 *
 *  * Copyright 2024 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdk1.services.lambda;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.CloudAccountInfo;
import com.newrelic.api.agent.CloudParameters;

import java.util.function.Function;

public class LambdaUtil {

    private static final String PLATFORM = "aws_lambda";
    private static final String NULL_ARN = "";
    private static final String PREFIX = "arn:aws:lambda:";
    private static final Function<FunctionRawData, FunctionProcessedData> CACHE =
            AgentBridge.collectionFactory.createAccessTimeBasedCache(3600, 8, LambdaUtil::processData);

    public static CloudParameters getCloudParameters(FunctionRawData functionRawData) {
        FunctionProcessedData data = CACHE.apply(functionRawData);
        String arn = data.getArn();
        CloudParameters.ResourceIdParameter cloudParameters = CloudParameters.provider(PLATFORM);
        // the cache will always return the NULL_ARN when it is not possible to calculate the ARN
        // so saving a few cycles by using != instead of equals.
        if (arn != NULL_ARN) {
            cloudParameters.resourceId(arn);
        }

        return cloudParameters.build();
    }

    /**
     * <p>
     *     Calculates the simple function name and ARN given
     *     the function name, qualifier, and possibly region (provided by config).
     * </p>
     * <p>
     *     Aliases are returned as part of the ARN, but versions are removed
     *     because they would make it harder to link to Lambdas/Alias entities.
     * </p>
     * <p>
     *     If qualifiers are provided both in the function ref, and as a qualifier, the one in function ref "wins".
     *     If they differ, the LambdaClient will throw an exception.
     * </p>
     *
     * @return a FunctionProcessedData object with the function name and ARN.
     *         If any of its values cannot be calculated, it will be the NULL_ARN.
     */
    // Visible for testing
    static FunctionProcessedData processData(FunctionRawData data) {
        String functionRef = data.getFunctionRef();

        String[] parts = functionRef.split(":");

        String functionName = NULL_ARN;
        String arn = NULL_ARN;

        if (parts.length == 1) {
            // function name: {function-name}
            String accountId = getAccountId(data.getSdkClient());
            if (accountId != null) {
                String qualifier = data.getQualifier();
                if (qualifier == null) {
                    arn = PREFIX + data.getRegion() + ":" + accountId + ":function:" + functionRef;
                } else {
                    arn = PREFIX + data.getRegion() + ":" + accountId + ":function:" + functionRef + ":" + qualifier;
                }
            }
            functionName = functionRef;

        } else if (parts.length == 2) {
            // function name + qualifier: {function-name}:{qualifier}
            String accountId = getAccountId(data.getSdkClient());
            if (accountId != null) {
                arn = PREFIX + data.getRegion() + ":" + accountId + ":function:" + functionRef;
            }
            functionName = parts[0];

        } else if (parts.length == 3) {
            // partial ARN: {account-id}:function:{function-name}
            functionName = parts[2];
            String qualifier = data.getQualifier();
            if (qualifier == null) {
                arn = PREFIX + data.getRegion() + ":" + functionRef;
            } else {
                arn = PREFIX + data.getRegion() + ":" + functionRef + ":" + qualifier;
            }

        } else if (parts.length == 4) {
            // partial ARN with qualifier: {account-id}:function:{function-name}:{qualifier}
            functionName = parts[2];
            arn = PREFIX + data.getRegion() + ":" + functionRef;

        } else if (parts.length == 7) {
            // full ARN: arn:aws:lambda:{region}:{account-id}:function:{function-name}
            functionName = parts[6];
            String qualifier = data.getQualifier();
            if (qualifier == null) {
                arn = functionRef;
            } else {
                arn = functionRef + ":" + qualifier;
            }

        } else if (parts.length == 8) {
            // full ARN with qualifier: arn:aws:lambda:{region}:{account-id}:function:{function-name}:{qualifier}
            functionName = parts[6];
            arn = functionRef;
        }
        // reference should be invalid if the number of parts do not match any of the expected cases

        return new FunctionProcessedData(functionName, arn);
    }

    private static String getAccountId(Object sdkClient) {
        return AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
    }

    public static String getSimpleFunctionName(FunctionRawData functionRawData) {
        return CACHE.apply(functionRawData).getFunctionName();
    }
}
