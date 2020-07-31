/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sns;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.AddPermissionRequest;
import software.amazon.awssdk.services.sns.model.AddPermissionResponse;
import software.amazon.awssdk.services.sns.model.AuthorizationErrorException;
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest;
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutResponse;
import software.amazon.awssdk.services.sns.model.ConcurrentAccessException;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionRequest;
import software.amazon.awssdk.services.sns.model.ConfirmSubscriptionResponse;
import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformApplicationResponse;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointRequest;
import software.amazon.awssdk.services.sns.model.CreatePlatformEndpointResponse;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.CreateTopicResponse;
import software.amazon.awssdk.services.sns.model.DeleteEndpointRequest;
import software.amazon.awssdk.services.sns.model.DeleteEndpointResponse;
import software.amazon.awssdk.services.sns.model.DeletePlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.DeletePlatformApplicationResponse;
import software.amazon.awssdk.services.sns.model.DeleteTopicRequest;
import software.amazon.awssdk.services.sns.model.DeleteTopicResponse;
import software.amazon.awssdk.services.sns.model.EndpointDisabledException;
import software.amazon.awssdk.services.sns.model.FilterPolicyLimitExceededException;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetPlatformApplicationAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetSmsAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetSmsAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetSubscriptionAttributesResponse;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
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
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationRequest;
import software.amazon.awssdk.services.sns.model.ListEndpointsByPlatformApplicationResponse;
import software.amazon.awssdk.services.sns.model.ListPhoneNumbersOptedOutRequest;
import software.amazon.awssdk.services.sns.model.ListPhoneNumbersOptedOutResponse;
import software.amazon.awssdk.services.sns.model.ListPlatformApplicationsRequest;
import software.amazon.awssdk.services.sns.model.ListPlatformApplicationsResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsByTopicResponse;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsRequest;
import software.amazon.awssdk.services.sns.model.ListSubscriptionsResponse;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.sns.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.sns.model.ListTopicsRequest;
import software.amazon.awssdk.services.sns.model.ListTopicsResponse;
import software.amazon.awssdk.services.sns.model.NotFoundException;
import software.amazon.awssdk.services.sns.model.OptInPhoneNumberRequest;
import software.amazon.awssdk.services.sns.model.OptInPhoneNumberResponse;
import software.amazon.awssdk.services.sns.model.PlatformApplicationDisabledException;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sns.model.RemovePermissionResponse;
import software.amazon.awssdk.services.sns.model.ResourceNotFoundException;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetEndpointAttributesResponse;
import software.amazon.awssdk.services.sns.model.SetPlatformApplicationAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetPlatformApplicationAttributesResponse;
import software.amazon.awssdk.services.sns.model.SetSmsAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetSmsAttributesResponse;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetSubscriptionAttributesResponse;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.SetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sns.model.StaleTagException;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.SubscriptionLimitExceededException;
import software.amazon.awssdk.services.sns.model.TagLimitExceededException;
import software.amazon.awssdk.services.sns.model.TagPolicyException;
import software.amazon.awssdk.services.sns.model.TagResourceRequest;
import software.amazon.awssdk.services.sns.model.TagResourceResponse;
import software.amazon.awssdk.services.sns.model.ThrottledException;
import software.amazon.awssdk.services.sns.model.TopicLimitExceededException;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeResponse;
import software.amazon.awssdk.services.sns.model.UntagResourceRequest;
import software.amazon.awssdk.services.sns.model.UntagResourceResponse;
import software.amazon.awssdk.services.sns.paginators.ListEndpointsByPlatformApplicationIterable;
import software.amazon.awssdk.services.sns.paginators.ListPlatformApplicationsIterable;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsByTopicIterable;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsIterable;
import software.amazon.awssdk.services.sns.paginators.ListTopicsIterable;

import java.util.function.Consumer;

public class SnsClientMock implements SnsClient {

    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public AddPermissionResponse addPermission(AddPermissionRequest addPermissionRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public AddPermissionResponse addPermission(Consumer<AddPermissionRequest.Builder> addPermissionRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public CheckIfPhoneNumberIsOptedOutResponse checkIfPhoneNumberIsOptedOut(CheckIfPhoneNumberIsOptedOutRequest checkIfPhoneNumberIsOptedOutRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public CheckIfPhoneNumberIsOptedOutResponse checkIfPhoneNumberIsOptedOut(
            Consumer<CheckIfPhoneNumberIsOptedOutRequest.Builder> checkIfPhoneNumberIsOptedOutRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ConfirmSubscriptionResponse confirmSubscription(ConfirmSubscriptionRequest confirmSubscriptionRequest)
            throws SubscriptionLimitExceededException, InvalidParameterException, NotFoundException, InternalErrorException, AuthorizationErrorException,
            FilterPolicyLimitExceededException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ConfirmSubscriptionResponse confirmSubscription(Consumer<ConfirmSubscriptionRequest.Builder> confirmSubscriptionRequest)
            throws SubscriptionLimitExceededException, InvalidParameterException, NotFoundException, InternalErrorException, AuthorizationErrorException,
            FilterPolicyLimitExceededException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public CreatePlatformApplicationResponse createPlatformApplication(CreatePlatformApplicationRequest createPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public CreatePlatformApplicationResponse createPlatformApplication(Consumer<CreatePlatformApplicationRequest.Builder> createPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public CreatePlatformEndpointResponse createPlatformEndpoint(CreatePlatformEndpointRequest createPlatformEndpointRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public CreatePlatformEndpointResponse createPlatformEndpoint(Consumer<CreatePlatformEndpointRequest.Builder> createPlatformEndpointRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public CreateTopicResponse createTopic(CreateTopicRequest createTopicRequest)
            throws InvalidParameterException, TopicLimitExceededException, InternalErrorException, AuthorizationErrorException, InvalidSecurityException,
            TagLimitExceededException, StaleTagException, TagPolicyException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public CreateTopicResponse createTopic(Consumer<CreateTopicRequest.Builder> createTopicRequest)
            throws InvalidParameterException, TopicLimitExceededException, InternalErrorException, AuthorizationErrorException, InvalidSecurityException,
            TagLimitExceededException, StaleTagException, TagPolicyException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeleteEndpointResponse deleteEndpoint(DeleteEndpointRequest deleteEndpointRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeleteEndpointResponse deleteEndpoint(Consumer<DeleteEndpointRequest.Builder> deleteEndpointRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeletePlatformApplicationResponse deletePlatformApplication(DeletePlatformApplicationRequest deletePlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeletePlatformApplicationResponse deletePlatformApplication(Consumer<DeletePlatformApplicationRequest.Builder> deletePlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeleteTopicResponse deleteTopic(DeleteTopicRequest deleteTopicRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, StaleTagException, TagPolicyException,
            ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public DeleteTopicResponse deleteTopic(Consumer<DeleteTopicRequest.Builder> deleteTopicRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, StaleTagException, TagPolicyException,
            ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public GetEndpointAttributesResponse getEndpointAttributes(GetEndpointAttributesRequest getEndpointAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetEndpointAttributesResponse getEndpointAttributes(Consumer<GetEndpointAttributesRequest.Builder> getEndpointAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetPlatformApplicationAttributesResponse getPlatformApplicationAttributes(
            GetPlatformApplicationAttributesRequest getPlatformApplicationAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetPlatformApplicationAttributesResponse getPlatformApplicationAttributes(
            Consumer<GetPlatformApplicationAttributesRequest.Builder> getPlatformApplicationAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetSmsAttributesResponse getSMSAttributes()
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetSmsAttributesResponse getSMSAttributes(GetSmsAttributesRequest getSmsAttributesRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetSmsAttributesResponse getSMSAttributes(Consumer<GetSmsAttributesRequest.Builder> getSmsAttributesRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetSubscriptionAttributesResponse getSubscriptionAttributes(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetSubscriptionAttributesResponse getSubscriptionAttributes(Consumer<GetSubscriptionAttributesRequest.Builder> getSubscriptionAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public GetTopicAttributesResponse getTopicAttributes(GetTopicAttributesRequest getTopicAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public GetTopicAttributesResponse getTopicAttributes(Consumer<GetTopicAttributesRequest.Builder> getTopicAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationResponse listEndpointsByPlatformApplication(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationResponse listEndpointsByPlatformApplication(
            Consumer<ListEndpointsByPlatformApplicationRequest.Builder> listEndpointsByPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationIterable listEndpointsByPlatformApplicationPaginator(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationIterable listEndpointsByPlatformApplicationPaginator(
            Consumer<ListEndpointsByPlatformApplicationRequest.Builder> listEndpointsByPlatformApplicationRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListPhoneNumbersOptedOutResponse listPhoneNumbersOptedOut()
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListPhoneNumbersOptedOutResponse listPhoneNumbersOptedOut(ListPhoneNumbersOptedOutRequest listPhoneNumbersOptedOutRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListPhoneNumbersOptedOutResponse listPhoneNumbersOptedOut(Consumer<ListPhoneNumbersOptedOutRequest.Builder> listPhoneNumbersOptedOutRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsResponse listPlatformApplications()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsResponse listPlatformApplications(ListPlatformApplicationsRequest listPlatformApplicationsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsResponse listPlatformApplications(Consumer<ListPlatformApplicationsRequest.Builder> listPlatformApplicationsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsIterable listPlatformApplicationsPaginator()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsIterable listPlatformApplicationsPaginator(ListPlatformApplicationsRequest listPlatformApplicationsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListPlatformApplicationsIterable listPlatformApplicationsPaginator(Consumer<ListPlatformApplicationsRequest.Builder> listPlatformApplicationsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsResponse listSubscriptions()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsResponse listSubscriptions(ListSubscriptionsRequest listSubscriptionsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsResponse listSubscriptions(Consumer<ListSubscriptionsRequest.Builder> listSubscriptionsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsIterable listSubscriptionsPaginator()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsIterable listSubscriptionsPaginator(ListSubscriptionsRequest listSubscriptionsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsIterable listSubscriptionsPaginator(Consumer<ListSubscriptionsRequest.Builder> listSubscriptionsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicResponse listSubscriptionsByTopic(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicResponse listSubscriptionsByTopic(Consumer<ListSubscriptionsByTopicRequest.Builder> listSubscriptionsByTopicRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicIterable listSubscriptionsByTopicPaginator(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicIterable listSubscriptionsByTopicPaginator(Consumer<ListSubscriptionsByTopicRequest.Builder> listSubscriptionsByTopicRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public ListTagsForResourceResponse listTagsForResource(ListTagsForResourceRequest listTagsForResourceRequest)
            throws ResourceNotFoundException, TagPolicyException, InvalidParameterException, AuthorizationErrorException, ConcurrentAccessException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTagsForResourceResponse listTagsForResource(Consumer<ListTagsForResourceRequest.Builder> listTagsForResourceRequest)
            throws ResourceNotFoundException, TagPolicyException, InvalidParameterException, AuthorizationErrorException, ConcurrentAccessException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsResponse listTopics()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsResponse listTopics(ListTopicsRequest listTopicsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsResponse listTopics(Consumer<ListTopicsRequest.Builder> listTopicsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsIterable listTopicsPaginator()
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsIterable listTopicsPaginator(ListTopicsRequest listTopicsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public ListTopicsIterable listTopicsPaginator(Consumer<ListTopicsRequest.Builder> listTopicsRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public OptInPhoneNumberResponse optInPhoneNumber(OptInPhoneNumberRequest optInPhoneNumberRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public OptInPhoneNumberResponse optInPhoneNumber(Consumer<OptInPhoneNumberRequest.Builder> optInPhoneNumberRequest)
            throws ThrottledException, InternalErrorException, AuthorizationErrorException, InvalidParameterException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public PublishResponse publish(PublishRequest publishRequest)
            throws InvalidParameterException, InvalidParameterValueException, InternalErrorException, NotFoundException, EndpointDisabledException,
            PlatformApplicationDisabledException, AuthorizationErrorException, KmsDisabledException, KmsInvalidStateException, KmsNotFoundException,
            KmsOptInRequiredException, KmsThrottlingException, KmsAccessDeniedException, InvalidSecurityException, AwsServiceException, SdkClientException,
            SnsException {
        return PublishResponse.builder().build();
    }

    @Override
    public PublishResponse publish(Consumer<PublishRequest.Builder> publishRequest)
            throws InvalidParameterException, InvalidParameterValueException, InternalErrorException, NotFoundException, EndpointDisabledException,
            PlatformApplicationDisabledException, AuthorizationErrorException, KmsDisabledException, KmsInvalidStateException, KmsNotFoundException,
            KmsOptInRequiredException, KmsThrottlingException, KmsAccessDeniedException, InvalidSecurityException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public RemovePermissionResponse removePermission(RemovePermissionRequest removePermissionRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public RemovePermissionResponse removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetEndpointAttributesResponse setEndpointAttributes(SetEndpointAttributesRequest setEndpointAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetEndpointAttributesResponse setEndpointAttributes(Consumer<SetEndpointAttributesRequest.Builder> setEndpointAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetPlatformApplicationAttributesResponse setPlatformApplicationAttributes(
            SetPlatformApplicationAttributesRequest setPlatformApplicationAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetPlatformApplicationAttributesResponse setPlatformApplicationAttributes(
            Consumer<SetPlatformApplicationAttributesRequest.Builder> setPlatformApplicationAttributesRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetSmsAttributesResponse setSMSAttributes(SetSmsAttributesRequest setSmsAttributesRequest)
            throws InvalidParameterException, ThrottledException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetSmsAttributesResponse setSMSAttributes(Consumer<SetSmsAttributesRequest.Builder> setSmsAttributesRequest)
            throws InvalidParameterException, ThrottledException, InternalErrorException, AuthorizationErrorException, AwsServiceException, SdkClientException,
            SnsException {
        return null;
    }

    @Override
    public SetSubscriptionAttributesResponse setSubscriptionAttributes(SetSubscriptionAttributesRequest setSubscriptionAttributesRequest)
            throws InvalidParameterException, FilterPolicyLimitExceededException, InternalErrorException, NotFoundException, AuthorizationErrorException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public SetSubscriptionAttributesResponse setSubscriptionAttributes(Consumer<SetSubscriptionAttributesRequest.Builder> setSubscriptionAttributesRequest)
            throws InvalidParameterException, FilterPolicyLimitExceededException, InternalErrorException, NotFoundException, AuthorizationErrorException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public SetTopicAttributesResponse setTopicAttributes(SetTopicAttributesRequest setTopicAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public SetTopicAttributesResponse setTopicAttributes(Consumer<SetTopicAttributesRequest.Builder> setTopicAttributesRequest)
            throws InvalidParameterException, InternalErrorException, NotFoundException, AuthorizationErrorException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public SubscribeResponse subscribe(SubscribeRequest subscribeRequest)
            throws SubscriptionLimitExceededException, FilterPolicyLimitExceededException, InvalidParameterException, InternalErrorException, NotFoundException,
            AuthorizationErrorException, InvalidSecurityException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public SubscribeResponse subscribe(Consumer<SubscribeRequest.Builder> subscribeRequest)
            throws SubscriptionLimitExceededException, FilterPolicyLimitExceededException, InvalidParameterException, InternalErrorException, NotFoundException,
            AuthorizationErrorException, InvalidSecurityException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public TagResourceResponse tagResource(TagResourceRequest tagResourceRequest)
            throws ResourceNotFoundException, TagLimitExceededException, StaleTagException, TagPolicyException, InvalidParameterException,
            AuthorizationErrorException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public TagResourceResponse tagResource(Consumer<TagResourceRequest.Builder> tagResourceRequest)
            throws ResourceNotFoundException, TagLimitExceededException, StaleTagException, TagPolicyException, InvalidParameterException,
            AuthorizationErrorException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public UnsubscribeResponse unsubscribe(UnsubscribeRequest unsubscribeRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public UnsubscribeResponse unsubscribe(Consumer<UnsubscribeRequest.Builder> unsubscribeRequest)
            throws InvalidParameterException, InternalErrorException, AuthorizationErrorException, NotFoundException, InvalidSecurityException,
            AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public UntagResourceResponse untagResource(UntagResourceRequest untagResourceRequest)
            throws ResourceNotFoundException, TagLimitExceededException, StaleTagException, TagPolicyException, InvalidParameterException,
            AuthorizationErrorException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }

    @Override
    public UntagResourceResponse untagResource(Consumer<UntagResourceRequest.Builder> untagResourceRequest)
            throws ResourceNotFoundException, TagLimitExceededException, StaleTagException, TagPolicyException, InvalidParameterException,
            AuthorizationErrorException, ConcurrentAccessException, AwsServiceException, SdkClientException, SnsException {
        return null;
    }
}
