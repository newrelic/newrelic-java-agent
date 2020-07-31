/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sns;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sns.AmazonSNSAsync;
import com.amazonaws.services.sns.model.AddPermissionRequest;
import com.amazonaws.services.sns.model.AddPermissionResult;
import com.amazonaws.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest;
import com.amazonaws.services.sns.model.CheckIfPhoneNumberIsOptedOutResult;
import com.amazonaws.services.sns.model.ConfirmSubscriptionRequest;
import com.amazonaws.services.sns.model.ConfirmSubscriptionResult;
import com.amazonaws.services.sns.model.CreatePlatformApplicationRequest;
import com.amazonaws.services.sns.model.CreatePlatformApplicationResult;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.DeleteEndpointRequest;
import com.amazonaws.services.sns.model.DeleteEndpointResult;
import com.amazonaws.services.sns.model.DeletePlatformApplicationRequest;
import com.amazonaws.services.sns.model.DeletePlatformApplicationResult;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.DeleteTopicResult;
import com.amazonaws.services.sns.model.GetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.GetEndpointAttributesResult;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.GetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.GetSMSAttributesRequest;
import com.amazonaws.services.sns.model.GetSMSAttributesResult;
import com.amazonaws.services.sns.model.GetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.model.GetSubscriptionAttributesResult;
import com.amazonaws.services.sns.model.GetTopicAttributesRequest;
import com.amazonaws.services.sns.model.GetTopicAttributesResult;
import com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationRequest;
import com.amazonaws.services.sns.model.ListEndpointsByPlatformApplicationResult;
import com.amazonaws.services.sns.model.ListPhoneNumbersOptedOutRequest;
import com.amazonaws.services.sns.model.ListPhoneNumbersOptedOutResult;
import com.amazonaws.services.sns.model.ListPlatformApplicationsRequest;
import com.amazonaws.services.sns.model.ListPlatformApplicationsResult;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListSubscriptionsRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.ListTagsForResourceRequest;
import com.amazonaws.services.sns.model.ListTagsForResourceResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.OptInPhoneNumberRequest;
import com.amazonaws.services.sns.model.OptInPhoneNumberResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sns.model.RemovePermissionRequest;
import com.amazonaws.services.sns.model.RemovePermissionResult;
import com.amazonaws.services.sns.model.SetEndpointAttributesRequest;
import com.amazonaws.services.sns.model.SetEndpointAttributesResult;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesRequest;
import com.amazonaws.services.sns.model.SetPlatformApplicationAttributesResult;
import com.amazonaws.services.sns.model.SetSMSAttributesRequest;
import com.amazonaws.services.sns.model.SetSMSAttributesResult;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesRequest;
import com.amazonaws.services.sns.model.SetSubscriptionAttributesResult;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.SetTopicAttributesResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.TagResourceRequest;
import com.amazonaws.services.sns.model.TagResourceResult;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.amazonaws.services.sns.model.UnsubscribeResult;
import com.amazonaws.services.sns.model.UntagResourceRequest;
import com.amazonaws.services.sns.model.UntagResourceResult;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class AmazonSNSMock implements AmazonSNSAsync {
    @Override
    public void setEndpoint(String endpoint) {

    }

    @Override
    public void setRegion(Region region) {

    }

    @Override
    public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) {
        return null;
    }

    @Override
    public AddPermissionResult addPermission(String topicArn, String label, List<String> aWSAccountIds, List<String> actionNames) {
        return null;
    }

    @Override
    public CheckIfPhoneNumberIsOptedOutResult checkIfPhoneNumberIsOptedOut(CheckIfPhoneNumberIsOptedOutRequest checkIfPhoneNumberIsOptedOutRequest) {
        return null;
    }

    @Override
    public ConfirmSubscriptionResult confirmSubscription(ConfirmSubscriptionRequest confirmSubscriptionRequest) {
        return null;
    }

    @Override
    public ConfirmSubscriptionResult confirmSubscription(String topicArn, String token, String authenticateOnUnsubscribe) {
        return null;
    }

    @Override
    public ConfirmSubscriptionResult confirmSubscription(String topicArn, String token) {
        return null;
    }

    @Override
    public CreatePlatformApplicationResult createPlatformApplication(CreatePlatformApplicationRequest createPlatformApplicationRequest) {
        return null;
    }

    @Override
    public CreatePlatformEndpointResult createPlatformEndpoint(CreatePlatformEndpointRequest createPlatformEndpointRequest) {
        return null;
    }

    @Override
    public CreateTopicResult createTopic(CreateTopicRequest createTopicRequest) {
        return null;
    }

    @Override
    public CreateTopicResult createTopic(String name) {
        return null;
    }

    @Override
    public DeleteEndpointResult deleteEndpoint(DeleteEndpointRequest deleteEndpointRequest) {
        return null;
    }

    @Override
    public DeletePlatformApplicationResult deletePlatformApplication(DeletePlatformApplicationRequest deletePlatformApplicationRequest) {
        return null;
    }

    @Override
    public DeleteTopicResult deleteTopic(DeleteTopicRequest deleteTopicRequest) {
        return null;
    }

    @Override
    public DeleteTopicResult deleteTopic(String topicArn) {
        return null;
    }

    @Override
    public GetEndpointAttributesResult getEndpointAttributes(GetEndpointAttributesRequest getEndpointAttributesRequest) {
        return null;
    }

    @Override
    public GetPlatformApplicationAttributesResult getPlatformApplicationAttributes(
            GetPlatformApplicationAttributesRequest getPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public GetSMSAttributesResult getSMSAttributes(GetSMSAttributesRequest getSMSAttributesRequest) {
        return null;
    }

    @Override
    public GetSubscriptionAttributesResult getSubscriptionAttributes(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public GetSubscriptionAttributesResult getSubscriptionAttributes(String subscriptionArn) {
        return null;
    }

    @Override
    public GetTopicAttributesResult getTopicAttributes(GetTopicAttributesRequest getTopicAttributesRequest) {
        return null;
    }

    @Override
    public GetTopicAttributesResult getTopicAttributes(String topicArn) {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationResult listEndpointsByPlatformApplication(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public ListPhoneNumbersOptedOutResult listPhoneNumbersOptedOut(ListPhoneNumbersOptedOutRequest listPhoneNumbersOptedOutRequest) {
        return null;
    }

    @Override
    public ListPlatformApplicationsResult listPlatformApplications(ListPlatformApplicationsRequest listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public ListPlatformApplicationsResult listPlatformApplications() {
        return null;
    }

    @Override
    public ListSubscriptionsResult listSubscriptions(ListSubscriptionsRequest listSubscriptionsRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsResult listSubscriptions() {
        return null;
    }

    @Override
    public ListSubscriptionsResult listSubscriptions(String nextToken) {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicResult listSubscriptionsByTopic(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicResult listSubscriptionsByTopic(String topicArn) {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicResult listSubscriptionsByTopic(String topicArn, String nextToken) {
        return null;
    }

    @Override
    public ListTagsForResourceResult listTagsForResource(ListTagsForResourceRequest listTagsForResourceRequest) {
        return null;
    }

    @Override
    public ListTopicsResult listTopics(ListTopicsRequest listTopicsRequest) {
        return null;
    }

    @Override
    public ListTopicsResult listTopics() {
        return null;
    }

    @Override
    public ListTopicsResult listTopics(String nextToken) {
        return null;
    }

    @Override
    public OptInPhoneNumberResult optInPhoneNumber(OptInPhoneNumberRequest optInPhoneNumberRequest) {
        return null;
    }

    @Override
    public PublishResult publish(PublishRequest publishRequest) {
        return new PublishResult();
    }

    @Override
    public PublishResult publish(String topicArn, String message) {
        return null;
    }

    @Override
    public PublishResult publish(String topicArn, String message, String subject) {
        return null;
    }

    @Override
    public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest) {
        return null;
    }

    @Override
    public RemovePermissionResult removePermission(String topicArn, String label) {
        return null;
    }

    @Override
    public SetEndpointAttributesResult setEndpointAttributes(SetEndpointAttributesRequest setEndpointAttributesRequest) {
        return null;
    }

    @Override
    public SetPlatformApplicationAttributesResult setPlatformApplicationAttributes(
            SetPlatformApplicationAttributesRequest setPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public SetSMSAttributesResult setSMSAttributes(SetSMSAttributesRequest setSMSAttributesRequest) {
        return null;
    }

    @Override
    public SetSubscriptionAttributesResult setSubscriptionAttributes(SetSubscriptionAttributesRequest setSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public SetSubscriptionAttributesResult setSubscriptionAttributes(String subscriptionArn, String attributeName, String attributeValue) {
        return null;
    }

    @Override
    public SetTopicAttributesResult setTopicAttributes(SetTopicAttributesRequest setTopicAttributesRequest) {
        return null;
    }

    @Override
    public SetTopicAttributesResult setTopicAttributes(String topicArn, String attributeName, String attributeValue) {
        return null;
    }

    @Override
    public SubscribeResult subscribe(SubscribeRequest subscribeRequest) {
        return null;
    }

    @Override
    public SubscribeResult subscribe(String topicArn, String protocol, String endpoint) {
        return null;
    }

    @Override
    public TagResourceResult tagResource(TagResourceRequest tagResourceRequest) {
        return null;
    }

    @Override
    public UnsubscribeResult unsubscribe(UnsubscribeRequest unsubscribeRequest) {
        return null;
    }

    @Override
    public UnsubscribeResult unsubscribe(String subscriptionArn) {
        return null;
    }

    @Override
    public UntagResourceResult untagResource(UntagResourceRequest untagResourceRequest) {
        return null;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return null;
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest) {
        return null;
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(AddPermissionRequest addPermissionRequest,
            AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(String topicArn, String label, List<String> aWSAccountIds, List<String> actionNames) {
        return null;
    }

    @Override
    public Future<AddPermissionResult> addPermissionAsync(String topicArn, String label, List<String> aWSAccountIds, List<String> actionNames,
            AsyncHandler<AddPermissionRequest, AddPermissionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<CheckIfPhoneNumberIsOptedOutResult> checkIfPhoneNumberIsOptedOutAsync(
            CheckIfPhoneNumberIsOptedOutRequest checkIfPhoneNumberIsOptedOutRequest) {
        return null;
    }

    @Override
    public Future<CheckIfPhoneNumberIsOptedOutResult> checkIfPhoneNumberIsOptedOutAsync(CheckIfPhoneNumberIsOptedOutRequest checkIfPhoneNumberIsOptedOutRequest,
            AsyncHandler<CheckIfPhoneNumberIsOptedOutRequest, CheckIfPhoneNumberIsOptedOutResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(ConfirmSubscriptionRequest confirmSubscriptionRequest) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(ConfirmSubscriptionRequest confirmSubscriptionRequest,
            AsyncHandler<ConfirmSubscriptionRequest, ConfirmSubscriptionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(String topicArn, String token, String authenticateOnUnsubscribe) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(String topicArn, String token, String authenticateOnUnsubscribe,
            AsyncHandler<ConfirmSubscriptionRequest, ConfirmSubscriptionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(String topicArn, String token) {
        return null;
    }

    @Override
    public Future<ConfirmSubscriptionResult> confirmSubscriptionAsync(String topicArn, String token,
            AsyncHandler<ConfirmSubscriptionRequest, ConfirmSubscriptionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<CreatePlatformApplicationResult> createPlatformApplicationAsync(CreatePlatformApplicationRequest createPlatformApplicationRequest) {
        return null;
    }

    @Override
    public Future<CreatePlatformApplicationResult> createPlatformApplicationAsync(CreatePlatformApplicationRequest createPlatformApplicationRequest,
            AsyncHandler<CreatePlatformApplicationRequest, CreatePlatformApplicationResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<CreatePlatformEndpointResult> createPlatformEndpointAsync(CreatePlatformEndpointRequest createPlatformEndpointRequest) {
        return null;
    }

    @Override
    public Future<CreatePlatformEndpointResult> createPlatformEndpointAsync(CreatePlatformEndpointRequest createPlatformEndpointRequest,
            AsyncHandler<CreatePlatformEndpointRequest, CreatePlatformEndpointResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<CreateTopicResult> createTopicAsync(CreateTopicRequest createTopicRequest) {
        return null;
    }

    @Override
    public Future<CreateTopicResult> createTopicAsync(CreateTopicRequest createTopicRequest, AsyncHandler<CreateTopicRequest, CreateTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<CreateTopicResult> createTopicAsync(String name) {
        return null;
    }

    @Override
    public Future<CreateTopicResult> createTopicAsync(String name, AsyncHandler<CreateTopicRequest, CreateTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<DeleteEndpointResult> deleteEndpointAsync(DeleteEndpointRequest deleteEndpointRequest) {
        return null;
    }

    @Override
    public Future<DeleteEndpointResult> deleteEndpointAsync(DeleteEndpointRequest deleteEndpointRequest,
            AsyncHandler<DeleteEndpointRequest, DeleteEndpointResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<DeletePlatformApplicationResult> deletePlatformApplicationAsync(DeletePlatformApplicationRequest deletePlatformApplicationRequest) {
        return null;
    }

    @Override
    public Future<DeletePlatformApplicationResult> deletePlatformApplicationAsync(DeletePlatformApplicationRequest deletePlatformApplicationRequest,
            AsyncHandler<DeletePlatformApplicationRequest, DeletePlatformApplicationResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<DeleteTopicResult> deleteTopicAsync(DeleteTopicRequest deleteTopicRequest) {
        return null;
    }

    @Override
    public Future<DeleteTopicResult> deleteTopicAsync(DeleteTopicRequest deleteTopicRequest, AsyncHandler<DeleteTopicRequest, DeleteTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<DeleteTopicResult> deleteTopicAsync(String topicArn) {
        return null;
    }

    @Override
    public Future<DeleteTopicResult> deleteTopicAsync(String topicArn, AsyncHandler<DeleteTopicRequest, DeleteTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetEndpointAttributesResult> getEndpointAttributesAsync(GetEndpointAttributesRequest getEndpointAttributesRequest) {
        return null;
    }

    @Override
    public Future<GetEndpointAttributesResult> getEndpointAttributesAsync(GetEndpointAttributesRequest getEndpointAttributesRequest,
            AsyncHandler<GetEndpointAttributesRequest, GetEndpointAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetPlatformApplicationAttributesResult> getPlatformApplicationAttributesAsync(
            GetPlatformApplicationAttributesRequest getPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public Future<GetPlatformApplicationAttributesResult> getPlatformApplicationAttributesAsync(
            GetPlatformApplicationAttributesRequest getPlatformApplicationAttributesRequest,
            AsyncHandler<GetPlatformApplicationAttributesRequest, GetPlatformApplicationAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetSMSAttributesResult> getSMSAttributesAsync(GetSMSAttributesRequest getSMSAttributesRequest) {
        return null;
    }

    @Override
    public Future<GetSMSAttributesResult> getSMSAttributesAsync(GetSMSAttributesRequest getSMSAttributesRequest,
            AsyncHandler<GetSMSAttributesRequest, GetSMSAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetSubscriptionAttributesResult> getSubscriptionAttributesAsync(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public Future<GetSubscriptionAttributesResult> getSubscriptionAttributesAsync(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest,
            AsyncHandler<GetSubscriptionAttributesRequest, GetSubscriptionAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetSubscriptionAttributesResult> getSubscriptionAttributesAsync(String subscriptionArn) {
        return null;
    }

    @Override
    public Future<GetSubscriptionAttributesResult> getSubscriptionAttributesAsync(String subscriptionArn,
            AsyncHandler<GetSubscriptionAttributesRequest, GetSubscriptionAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetTopicAttributesResult> getTopicAttributesAsync(GetTopicAttributesRequest getTopicAttributesRequest) {
        return null;
    }

    @Override
    public Future<GetTopicAttributesResult> getTopicAttributesAsync(GetTopicAttributesRequest getTopicAttributesRequest,
            AsyncHandler<GetTopicAttributesRequest, GetTopicAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<GetTopicAttributesResult> getTopicAttributesAsync(String topicArn) {
        return null;
    }

    @Override
    public Future<GetTopicAttributesResult> getTopicAttributesAsync(String topicArn,
            AsyncHandler<GetTopicAttributesRequest, GetTopicAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListEndpointsByPlatformApplicationResult> listEndpointsByPlatformApplicationAsync(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public Future<ListEndpointsByPlatformApplicationResult> listEndpointsByPlatformApplicationAsync(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest,
            AsyncHandler<ListEndpointsByPlatformApplicationRequest, ListEndpointsByPlatformApplicationResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListPhoneNumbersOptedOutResult> listPhoneNumbersOptedOutAsync(ListPhoneNumbersOptedOutRequest listPhoneNumbersOptedOutRequest) {
        return null;
    }

    @Override
    public Future<ListPhoneNumbersOptedOutResult> listPhoneNumbersOptedOutAsync(ListPhoneNumbersOptedOutRequest listPhoneNumbersOptedOutRequest,
            AsyncHandler<ListPhoneNumbersOptedOutRequest, ListPhoneNumbersOptedOutResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListPlatformApplicationsResult> listPlatformApplicationsAsync(ListPlatformApplicationsRequest listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public Future<ListPlatformApplicationsResult> listPlatformApplicationsAsync(ListPlatformApplicationsRequest listPlatformApplicationsRequest,
            AsyncHandler<ListPlatformApplicationsRequest, ListPlatformApplicationsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListPlatformApplicationsResult> listPlatformApplicationsAsync() {
        return null;
    }

    @Override
    public Future<ListPlatformApplicationsResult> listPlatformApplicationsAsync(
            AsyncHandler<ListPlatformApplicationsRequest, ListPlatformApplicationsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync(ListSubscriptionsRequest listSubscriptionsRequest) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync(ListSubscriptionsRequest listSubscriptionsRequest,
            AsyncHandler<ListSubscriptionsRequest, ListSubscriptionsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync() {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync(AsyncHandler<ListSubscriptionsRequest, ListSubscriptionsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync(String nextToken) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsResult> listSubscriptionsAsync(String nextToken,
            AsyncHandler<ListSubscriptionsRequest, ListSubscriptionsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest,
            AsyncHandler<ListSubscriptionsByTopicRequest, ListSubscriptionsByTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(String topicArn) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(String topicArn,
            AsyncHandler<ListSubscriptionsByTopicRequest, ListSubscriptionsByTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(String topicArn, String nextToken) {
        return null;
    }

    @Override
    public Future<ListSubscriptionsByTopicResult> listSubscriptionsByTopicAsync(String topicArn, String nextToken,
            AsyncHandler<ListSubscriptionsByTopicRequest, ListSubscriptionsByTopicResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListTagsForResourceResult> listTagsForResourceAsync(ListTagsForResourceRequest listTagsForResourceRequest) {
        return null;
    }

    @Override
    public Future<ListTagsForResourceResult> listTagsForResourceAsync(ListTagsForResourceRequest listTagsForResourceRequest,
            AsyncHandler<ListTagsForResourceRequest, ListTagsForResourceResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync(ListTopicsRequest listTopicsRequest) {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync(ListTopicsRequest listTopicsRequest, AsyncHandler<ListTopicsRequest, ListTopicsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync() {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync(AsyncHandler<ListTopicsRequest, ListTopicsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync(String nextToken) {
        return null;
    }

    @Override
    public Future<ListTopicsResult> listTopicsAsync(String nextToken, AsyncHandler<ListTopicsRequest, ListTopicsResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<OptInPhoneNumberResult> optInPhoneNumberAsync(OptInPhoneNumberRequest optInPhoneNumberRequest) {
        return null;
    }

    @Override
    public Future<OptInPhoneNumberResult> optInPhoneNumberAsync(OptInPhoneNumberRequest optInPhoneNumberRequest,
            AsyncHandler<OptInPhoneNumberRequest, OptInPhoneNumberResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<PublishResult> publishAsync(PublishRequest publishRequest) {
        return publishAsync(publishRequest, null);
    }

    @Override
    public Future<PublishResult> publishAsync(final PublishRequest publishRequest, final AsyncHandler<PublishRequest, PublishResult> asyncHandler) {
        final FutureTask<PublishResult> result = new FutureTask<>(new Callable<PublishResult>() {
            @Override
            public PublishResult call() throws Exception {
                return new PublishResult();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                result.run();
                if (asyncHandler != null) {
                    if (publishRequest.getMessage().contains("fail me")) {
                        asyncHandler.onError(new IOException("failing this one!"));
                    } else {
                        asyncHandler.onSuccess(publishRequest, new PublishResult());
                    }
                }
            }
        }).start();
        return result;
    }

    @Override
    public Future<PublishResult> publishAsync(String topicArn, String message) {
        return null;
    }

    @Override
    public Future<PublishResult> publishAsync(String topicArn, String message, AsyncHandler<PublishRequest, PublishResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<PublishResult> publishAsync(String topicArn, String message, String subject) {
        return null;
    }

    @Override
    public Future<PublishResult> publishAsync(String topicArn, String message, String subject, AsyncHandler<PublishRequest, PublishResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest) {
        return null;
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(RemovePermissionRequest removePermissionRequest,
            AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(String topicArn, String label) {
        return null;
    }

    @Override
    public Future<RemovePermissionResult> removePermissionAsync(String topicArn, String label,
            AsyncHandler<RemovePermissionRequest, RemovePermissionResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetEndpointAttributesResult> setEndpointAttributesAsync(SetEndpointAttributesRequest setEndpointAttributesRequest) {
        return null;
    }

    @Override
    public Future<SetEndpointAttributesResult> setEndpointAttributesAsync(SetEndpointAttributesRequest setEndpointAttributesRequest,
            AsyncHandler<SetEndpointAttributesRequest, SetEndpointAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetPlatformApplicationAttributesResult> setPlatformApplicationAttributesAsync(
            SetPlatformApplicationAttributesRequest setPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public Future<SetPlatformApplicationAttributesResult> setPlatformApplicationAttributesAsync(
            SetPlatformApplicationAttributesRequest setPlatformApplicationAttributesRequest,
            AsyncHandler<SetPlatformApplicationAttributesRequest, SetPlatformApplicationAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetSMSAttributesResult> setSMSAttributesAsync(SetSMSAttributesRequest setSMSAttributesRequest) {
        return null;
    }

    @Override
    public Future<SetSMSAttributesResult> setSMSAttributesAsync(SetSMSAttributesRequest setSMSAttributesRequest,
            AsyncHandler<SetSMSAttributesRequest, SetSMSAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetSubscriptionAttributesResult> setSubscriptionAttributesAsync(SetSubscriptionAttributesRequest setSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public Future<SetSubscriptionAttributesResult> setSubscriptionAttributesAsync(SetSubscriptionAttributesRequest setSubscriptionAttributesRequest,
            AsyncHandler<SetSubscriptionAttributesRequest, SetSubscriptionAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetSubscriptionAttributesResult> setSubscriptionAttributesAsync(String subscriptionArn, String attributeName, String attributeValue) {
        return null;
    }

    @Override
    public Future<SetSubscriptionAttributesResult> setSubscriptionAttributesAsync(String subscriptionArn, String attributeName, String attributeValue,
            AsyncHandler<SetSubscriptionAttributesRequest, SetSubscriptionAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetTopicAttributesResult> setTopicAttributesAsync(SetTopicAttributesRequest setTopicAttributesRequest) {
        return null;
    }

    @Override
    public Future<SetTopicAttributesResult> setTopicAttributesAsync(SetTopicAttributesRequest setTopicAttributesRequest,
            AsyncHandler<SetTopicAttributesRequest, SetTopicAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SetTopicAttributesResult> setTopicAttributesAsync(String topicArn, String attributeName, String attributeValue) {
        return null;
    }

    @Override
    public Future<SetTopicAttributesResult> setTopicAttributesAsync(String topicArn, String attributeName, String attributeValue,
            AsyncHandler<SetTopicAttributesRequest, SetTopicAttributesResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SubscribeResult> subscribeAsync(SubscribeRequest subscribeRequest) {
        return null;
    }

    @Override
    public Future<SubscribeResult> subscribeAsync(SubscribeRequest subscribeRequest, AsyncHandler<SubscribeRequest, SubscribeResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<SubscribeResult> subscribeAsync(String topicArn, String protocol, String endpoint) {
        return null;
    }

    @Override
    public Future<SubscribeResult> subscribeAsync(String topicArn, String protocol, String endpoint,
            AsyncHandler<SubscribeRequest, SubscribeResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<TagResourceResult> tagResourceAsync(TagResourceRequest tagResourceRequest) {
        return null;
    }

    @Override
    public Future<TagResourceResult> tagResourceAsync(TagResourceRequest tagResourceRequest, AsyncHandler<TagResourceRequest, TagResourceResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<UnsubscribeResult> unsubscribeAsync(UnsubscribeRequest unsubscribeRequest) {
        return null;
    }

    @Override
    public Future<UnsubscribeResult> unsubscribeAsync(UnsubscribeRequest unsubscribeRequest, AsyncHandler<UnsubscribeRequest, UnsubscribeResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<UnsubscribeResult> unsubscribeAsync(String subscriptionArn) {
        return null;
    }

    @Override
    public Future<UnsubscribeResult> unsubscribeAsync(String subscriptionArn, AsyncHandler<UnsubscribeRequest, UnsubscribeResult> asyncHandler) {
        return null;
    }

    @Override
    public Future<UntagResourceResult> untagResourceAsync(UntagResourceRequest untagResourceRequest) {
        return null;
    }

    @Override
    public Future<UntagResourceResult> untagResourceAsync(UntagResourceRequest untagResourceRequest,
            AsyncHandler<UntagResourceRequest, UntagResourceResult> asyncHandler) {
        return null;
    }
}
