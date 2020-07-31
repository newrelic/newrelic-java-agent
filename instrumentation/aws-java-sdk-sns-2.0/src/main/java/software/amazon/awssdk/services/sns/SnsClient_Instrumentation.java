/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sns;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.TracedMethod;
import com.newrelic.api.agent.ExternalParameters;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.agent.instrumentation.SnsClientInstrumentationHelper;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.EndpointDisabledException;
import software.amazon.awssdk.services.sns.model.InternalErrorException;
import software.amazon.awssdk.services.sns.model.InvalidParameterException;
import software.amazon.awssdk.services.sns.model.InvalidParameterValueException;
import software.amazon.awssdk.services.sns.model.InvalidSecurityException;
import software.amazon.awssdk.services.sns.model.KmsAccessDeniedException;
import software.amazon.awssdk.services.sns.model.KmsDisabledException;
import software.amazon.awssdk.services.sns.model.KmsInvalidStateException;
import software.amazon.awssdk.services.sns.model.KmsNotFoundException;
import software.amazon.awssdk.services.sns.model.KmsOptInRequiredException;
import software.amazon.awssdk.services.sns.model.KmsThrottlingException;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.PlatformApplicationDisabledException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.sns.SnsClient")
public class SnsClient_Instrumentation {

    @Trace
    public PublishResponse publish(PublishRequest publishRequest) throws InvalidParameterException,
            InvalidParameterValueException, InternalErrorException, NotFoundException, EndpointDisabledException,
            PlatformApplicationDisabledException, AuthorizationErrorException, KmsDisabledException, KmsInvalidStateException,
            KmsNotFoundException, KmsOptInRequiredException, KmsThrottlingException, KmsAccessDeniedException,
            InvalidSecurityException, AwsServiceException, SdkClientException, SnsException {
        TracedMethod tracedMethod = AgentBridge.getAgent().getTracedMethod();
        ExternalParameters params = SnsClientInstrumentationHelper.makeMessageProducerParameters(publishRequest);
        tracedMethod.reportAsExternal(params);

        return Weaver.callOriginal();
    }
}
