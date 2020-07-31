/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.nr.agent.instrumentation.sns;

import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.AddPermissionRequest;
import software.amazon.awssdk.services.sns.model.AddPermissionResponse;
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutRequest;
import software.amazon.awssdk.services.sns.model.CheckIfPhoneNumberIsOptedOutResponse;
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
import software.amazon.awssdk.services.sns.model.OptInPhoneNumberRequest;
import software.amazon.awssdk.services.sns.model.OptInPhoneNumberResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.RemovePermissionRequest;
import software.amazon.awssdk.services.sns.model.RemovePermissionResponse;
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
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.TagResourceRequest;
import software.amazon.awssdk.services.sns.model.TagResourceResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;
import software.amazon.awssdk.services.sns.model.UnsubscribeResponse;
import software.amazon.awssdk.services.sns.model.UntagResourceRequest;
import software.amazon.awssdk.services.sns.model.UntagResourceResponse;
import software.amazon.awssdk.services.sns.paginators.ListEndpointsByPlatformApplicationPublisher;
import software.amazon.awssdk.services.sns.paginators.ListPlatformApplicationsPublisher;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsByTopicPublisher;
import software.amazon.awssdk.services.sns.paginators.ListSubscriptionsPublisher;
import software.amazon.awssdk.services.sns.paginators.ListTopicsPublisher;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SnsAsyncClientMock implements SnsAsyncClient {
    @Override
    public String serviceName() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<AddPermissionResponse> addPermission(AddPermissionRequest addPermissionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<AddPermissionResponse> addPermission(Consumer<AddPermissionRequest.Builder> addPermissionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CheckIfPhoneNumberIsOptedOutResponse> checkIfPhoneNumberIsOptedOut(
            CheckIfPhoneNumberIsOptedOutRequest checkIfPhoneNumberIsOptedOutRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CheckIfPhoneNumberIsOptedOutResponse> checkIfPhoneNumberIsOptedOut(
            Consumer<CheckIfPhoneNumberIsOptedOutRequest.Builder> checkIfPhoneNumberIsOptedOutRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ConfirmSubscriptionResponse> confirmSubscription(ConfirmSubscriptionRequest confirmSubscriptionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ConfirmSubscriptionResponse> confirmSubscription(Consumer<ConfirmSubscriptionRequest.Builder> confirmSubscriptionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreatePlatformApplicationResponse> createPlatformApplication(CreatePlatformApplicationRequest createPlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreatePlatformApplicationResponse> createPlatformApplication(
            Consumer<CreatePlatformApplicationRequest.Builder> createPlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreatePlatformEndpointResponse> createPlatformEndpoint(CreatePlatformEndpointRequest createPlatformEndpointRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreatePlatformEndpointResponse> createPlatformEndpoint(
            Consumer<CreatePlatformEndpointRequest.Builder> createPlatformEndpointRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreateTopicResponse> createTopic(CreateTopicRequest createTopicRequest) {
        return null;
    }

    @Override
    public CompletableFuture<CreateTopicResponse> createTopic(Consumer<CreateTopicRequest.Builder> createTopicRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeleteEndpointResponse> deleteEndpoint(DeleteEndpointRequest deleteEndpointRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeleteEndpointResponse> deleteEndpoint(Consumer<DeleteEndpointRequest.Builder> deleteEndpointRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeletePlatformApplicationResponse> deletePlatformApplication(DeletePlatformApplicationRequest deletePlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeletePlatformApplicationResponse> deletePlatformApplication(
            Consumer<DeletePlatformApplicationRequest.Builder> deletePlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeleteTopicResponse> deleteTopic(DeleteTopicRequest deleteTopicRequest) {
        return null;
    }

    @Override
    public CompletableFuture<DeleteTopicResponse> deleteTopic(Consumer<DeleteTopicRequest.Builder> deleteTopicRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetEndpointAttributesResponse> getEndpointAttributes(GetEndpointAttributesRequest getEndpointAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetEndpointAttributesResponse> getEndpointAttributes(Consumer<GetEndpointAttributesRequest.Builder> getEndpointAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetPlatformApplicationAttributesResponse> getPlatformApplicationAttributes(
            GetPlatformApplicationAttributesRequest getPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetPlatformApplicationAttributesResponse> getPlatformApplicationAttributes(
            Consumer<GetPlatformApplicationAttributesRequest.Builder> getPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetSmsAttributesResponse> getSMSAttributes(GetSmsAttributesRequest getSmsAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetSmsAttributesResponse> getSMSAttributes(Consumer<GetSmsAttributesRequest.Builder> getSmsAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetSmsAttributesResponse> getSMSAttributes() {
        return null;
    }

    @Override
    public CompletableFuture<GetSubscriptionAttributesResponse> getSubscriptionAttributes(GetSubscriptionAttributesRequest getSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetSubscriptionAttributesResponse> getSubscriptionAttributes(
            Consumer<GetSubscriptionAttributesRequest.Builder> getSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetTopicAttributesResponse> getTopicAttributes(GetTopicAttributesRequest getTopicAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<GetTopicAttributesResponse> getTopicAttributes(Consumer<GetTopicAttributesRequest.Builder> getTopicAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListEndpointsByPlatformApplicationResponse> listEndpointsByPlatformApplication(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListEndpointsByPlatformApplicationResponse> listEndpointsByPlatformApplication(
            Consumer<ListEndpointsByPlatformApplicationRequest.Builder> listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationPublisher listEndpointsByPlatformApplicationPaginator(
            ListEndpointsByPlatformApplicationRequest listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public ListEndpointsByPlatformApplicationPublisher listEndpointsByPlatformApplicationPaginator(
            Consumer<ListEndpointsByPlatformApplicationRequest.Builder> listEndpointsByPlatformApplicationRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListPhoneNumbersOptedOutResponse> listPhoneNumbersOptedOut(ListPhoneNumbersOptedOutRequest listPhoneNumbersOptedOutRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListPhoneNumbersOptedOutResponse> listPhoneNumbersOptedOut(
            Consumer<ListPhoneNumbersOptedOutRequest.Builder> listPhoneNumbersOptedOutRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListPhoneNumbersOptedOutResponse> listPhoneNumbersOptedOut() {
        return null;
    }

    @Override
    public CompletableFuture<ListPlatformApplicationsResponse> listPlatformApplications(ListPlatformApplicationsRequest listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListPlatformApplicationsResponse> listPlatformApplications(
            Consumer<ListPlatformApplicationsRequest.Builder> listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListPlatformApplicationsResponse> listPlatformApplications() {
        return null;
    }

    @Override
    public ListPlatformApplicationsPublisher listPlatformApplicationsPaginator() {
        return null;
    }

    @Override
    public ListPlatformApplicationsPublisher listPlatformApplicationsPaginator(ListPlatformApplicationsRequest listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public ListPlatformApplicationsPublisher listPlatformApplicationsPaginator(
            Consumer<ListPlatformApplicationsRequest.Builder> listPlatformApplicationsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListSubscriptionsResponse> listSubscriptions(ListSubscriptionsRequest listSubscriptionsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListSubscriptionsResponse> listSubscriptions(Consumer<ListSubscriptionsRequest.Builder> listSubscriptionsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListSubscriptionsResponse> listSubscriptions() {
        return null;
    }

    @Override
    public CompletableFuture<ListSubscriptionsByTopicResponse> listSubscriptionsByTopic(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListSubscriptionsByTopicResponse> listSubscriptionsByTopic(
            Consumer<ListSubscriptionsByTopicRequest.Builder> listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicPublisher listSubscriptionsByTopicPaginator(ListSubscriptionsByTopicRequest listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsByTopicPublisher listSubscriptionsByTopicPaginator(
            Consumer<ListSubscriptionsByTopicRequest.Builder> listSubscriptionsByTopicRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsPublisher listSubscriptionsPaginator() {
        return null;
    }

    @Override
    public ListSubscriptionsPublisher listSubscriptionsPaginator(ListSubscriptionsRequest listSubscriptionsRequest) {
        return null;
    }

    @Override
    public ListSubscriptionsPublisher listSubscriptionsPaginator(Consumer<ListSubscriptionsRequest.Builder> listSubscriptionsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListTagsForResourceResponse> listTagsForResource(ListTagsForResourceRequest listTagsForResourceRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListTagsForResourceResponse> listTagsForResource(Consumer<ListTagsForResourceRequest.Builder> listTagsForResourceRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListTopicsResponse> listTopics(ListTopicsRequest listTopicsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListTopicsResponse> listTopics(Consumer<ListTopicsRequest.Builder> listTopicsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<ListTopicsResponse> listTopics() {
        return null;
    }

    @Override
    public ListTopicsPublisher listTopicsPaginator() {
        return null;
    }

    @Override
    public ListTopicsPublisher listTopicsPaginator(ListTopicsRequest listTopicsRequest) {
        return null;
    }

    @Override
    public ListTopicsPublisher listTopicsPaginator(Consumer<ListTopicsRequest.Builder> listTopicsRequest) {
        return null;
    }

    @Override
    public CompletableFuture<OptInPhoneNumberResponse> optInPhoneNumber(OptInPhoneNumberRequest optInPhoneNumberRequest) {
        return null;
    }

    @Override
    public CompletableFuture<OptInPhoneNumberResponse> optInPhoneNumber(Consumer<OptInPhoneNumberRequest.Builder> optInPhoneNumberRequest) {
        return null;
    }

    @Override
    public CompletableFuture<PublishResponse> publish(PublishRequest publishRequest) {
        if (publishRequest.message().contains("fail me")) {
            return CompletableFuture.supplyAsync(() -> {
                throw new RuntimeException("I was told to fail!");
            });
        }
        return CompletableFuture.completedFuture(PublishResponse.builder().build());
    }

    @Override
    public CompletableFuture<PublishResponse> publish(Consumer<PublishRequest.Builder> publishRequest) {
        return null;
    }

    @Override
    public CompletableFuture<RemovePermissionResponse> removePermission(RemovePermissionRequest removePermissionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<RemovePermissionResponse> removePermission(Consumer<RemovePermissionRequest.Builder> removePermissionRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetEndpointAttributesResponse> setEndpointAttributes(SetEndpointAttributesRequest setEndpointAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetEndpointAttributesResponse> setEndpointAttributes(Consumer<SetEndpointAttributesRequest.Builder> setEndpointAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetPlatformApplicationAttributesResponse> setPlatformApplicationAttributes(
            SetPlatformApplicationAttributesRequest setPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetPlatformApplicationAttributesResponse> setPlatformApplicationAttributes(
            Consumer<SetPlatformApplicationAttributesRequest.Builder> setPlatformApplicationAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetSmsAttributesResponse> setSMSAttributes(SetSmsAttributesRequest setSmsAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetSmsAttributesResponse> setSMSAttributes(Consumer<SetSmsAttributesRequest.Builder> setSmsAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetSubscriptionAttributesResponse> setSubscriptionAttributes(SetSubscriptionAttributesRequest setSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetSubscriptionAttributesResponse> setSubscriptionAttributes(
            Consumer<SetSubscriptionAttributesRequest.Builder> setSubscriptionAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetTopicAttributesResponse> setTopicAttributes(SetTopicAttributesRequest setTopicAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SetTopicAttributesResponse> setTopicAttributes(Consumer<SetTopicAttributesRequest.Builder> setTopicAttributesRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SubscribeResponse> subscribe(SubscribeRequest subscribeRequest) {
        return null;
    }

    @Override
    public CompletableFuture<SubscribeResponse> subscribe(Consumer<SubscribeRequest.Builder> subscribeRequest) {
        return null;
    }

    @Override
    public CompletableFuture<TagResourceResponse> tagResource(TagResourceRequest tagResourceRequest) {
        return null;
    }

    @Override
    public CompletableFuture<TagResourceResponse> tagResource(Consumer<TagResourceRequest.Builder> tagResourceRequest) {
        return null;
    }

    @Override
    public CompletableFuture<UnsubscribeResponse> unsubscribe(UnsubscribeRequest unsubscribeRequest) {
        return null;
    }

    @Override
    public CompletableFuture<UnsubscribeResponse> unsubscribe(Consumer<UnsubscribeRequest.Builder> unsubscribeRequest) {
        return null;
    }

    @Override
    public CompletableFuture<UntagResourceResponse> untagResource(UntagResourceRequest untagResourceRequest) {
        return null;
    }

    @Override
    public CompletableFuture<UntagResourceResponse> untagResource(Consumer<UntagResourceRequest.Builder> untagResourceRequest) {
        return null;
    }
}
