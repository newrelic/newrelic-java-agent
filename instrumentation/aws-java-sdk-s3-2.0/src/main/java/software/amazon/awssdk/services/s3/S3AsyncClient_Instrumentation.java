/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.s3;

import com.agent.instrumentation.awsjavasdk2.services.s3.ResultWrapper;
import com.agent.instrumentation.awsjavasdk2.services.s3.S3ResponseResultWrapper;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.util.concurrent.CompletableFuture;

@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.s3.S3AsyncClient")
public class S3AsyncClient_Instrumentation {

    public CompletableFuture<CreateBucketResponse> createBucket(CreateBucketRequest createBucketRequest) {
        String uri = "s3://" + createBucketRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "createBucket");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<CreateBucketResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "createBucket"));
    }

    public CompletableFuture<DeleteBucketResponse> deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        String uri = "s3://" + deleteBucketRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteBucket");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteBucketResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "deleteBucket"));
    }

    public CompletableFuture<ListBucketsResponse> listBuckets(ListBucketsRequest listBucketsRequest) {
        String uri = "s3://amazon/";
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "listBuckets");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ListBucketsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "listBuckets"));
    }

    public CompletableFuture<GetBucketLocationResponse> getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        String uri = "s3://" + getBucketLocationRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "getBucketLocation");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<GetBucketLocationResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "getBucketLocation"));
    }

    public <ReturnT> CompletableFuture<ReturnT> getObject(GetObjectRequest getObjectRequest, AsyncResponseTransformer asyncResponseTransformer) {
        String uri = "s3://" + getObjectRequest.bucket() + "/" + getObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "getObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ReturnT> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<ListObjectsResponse> listObjects(ListObjectsRequest listObjectsRequest) {
        String uri = "s3://" + listObjectsRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "listObjects");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ListObjectsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "listObjects"));
    }

    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody asyncRequestBody) {
        String uri = "s3://" + putObjectRequest.bucket() + "/" + putObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "putObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<PutObjectResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "putObject"));
    }

    public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest deleteObjectRequest) {
        String uri = "s3://" + deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteObjectResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "deleteObject"));
    }

    public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        String uri = "s3://" + deleteObjectsRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteObjects");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteObjectsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new S3ResponseResultWrapper<>(segment, uri, "deleteObjects"));
    }
}
