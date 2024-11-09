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
import com.newrelic.api.agent.NewRelic;

import java.util.function.Function;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LambdaUtil {

    private static final String PLATFORM = "aws_lambda";
    private static final String NULL_ARN = "";
    private static final FunctionProcessedData NULL_DATA = new FunctionProcessedData(NULL_ARN, NULL_ARN);
    private static final String PREFIX = "arn:aws:lambda:";
    private static final Pattern FUNC_REF_PATTERN = Pattern.compile(
            "(arn:(aws[a-zA-Z-]*)?:lambda:)?" + // arn prefix
            "((?<region>[a-z]{2}((-gov)|(-iso([a-z]?)))?-[a-z]+-\\d{1}):)?" + // region
            "((?<accountId>\\d{12}):)?" + // account id
            "(function:)?" + // constant
            "(?<functionName>[a-zA-Z0-9-\\.]+)" + // function name (only required part)
            "(:(?<qualifier>\\$LATEST|[a-zA-Z0-9-]+))?"); // qualifier: version or alias
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
        String arn = NULL_ARN;

        String functionRef = data.getFunctionRef();
        Matcher matcher = FUNC_REF_PATTERN.matcher(functionRef);
        if (!matcher.matches()) {
            return NULL_DATA;
        }
        String region = matcher.group("region");
        String accountId = matcher.group("accountId");
        String qualifier = matcher.group("qualifier");
        String functionName = matcher.group("functionName");

        if (functionName == null) {
            // will not be able to add any data
            NewRelic.getAgent().getLogger().log(Level.INFO, "aws-lambda: Unable to assemble ARN: " + functionRef);
            return NULL_DATA;
        }

        if (region == null) {
            // if region is not provided, we will try to get it from the SDK client
            region = data.getRegion();
        }

        if (accountId == null) {
            // if account id is not provided, we will try to get it from the config
            accountId = getAccountId(data.getSdkClient());
        }

        if (region != null && accountId != null) {
            if (qualifier == null) {
                qualifier = data.getQualifier();
            }

            if (qualifier == null || qualifier.isEmpty() || "$LATEST".equals(qualifier)) {
                arn = PREFIX + region + ":" + accountId + ":function:" + functionName;
            } else {
                arn = PREFIX + region + ":" + accountId + ":function:" + functionName + ":" + qualifier;
            }
        } else {
            NewRelic.getAgent().getLogger().log(Level.INFO, "aws-lambda: Missing information to assemble ARN.");
        }
        return new FunctionProcessedData(functionName, arn);
    }

    private static String getAccountId(Object sdkClient) {
        return AgentBridge.cloud.getAccountInfo(sdkClient, CloudAccountInfo.AWS_ACCOUNT_ID);
    }

    public static String getSimpleFunctionName(FunctionRawData functionRawData) {
        return CACHE.apply(functionRawData).getFunctionName();
    }
}
