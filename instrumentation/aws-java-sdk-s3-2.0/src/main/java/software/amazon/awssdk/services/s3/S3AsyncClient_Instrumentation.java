/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.s3;

import com.agent.instrumentation.awsjavasdk2.services.s3.S3MetricUtil;
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
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Response;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.s3.S3AsyncClient")
public class S3AsyncClient_Instrumentation {

    public CompletableFuture<CreateBucketResponse> createBucket(CreateBucketRequest createBucketRequest) {
        String uri = "s3://" + createBucketRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "createBucket");
        S3MetricUtil.reportExternalMetrics(segment, uri, "createBucket");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<CreateBucketResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<DeleteBucketResponse> deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        String uri = "s3://" + deleteBucketRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteBucket");
        S3MetricUtil.reportExternalMetrics(segment, uri, "deleteBucket");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteBucketResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<ListBucketsResponse> listBuckets(ListBucketsRequest listBucketsRequest) {
        String uri = "s3://amazon/";
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "listBuckets");
        S3MetricUtil.reportExternalMetrics(segment, uri, "listBuckets");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ListBucketsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<GetBucketLocationResponse> getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        String uri = "s3://" + getBucketLocationRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "getBucketLocation");
        S3MetricUtil.reportExternalMetrics(segment, uri, "getBucketLocation");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<GetBucketLocationResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public <ReturnT> CompletableFuture<ReturnT> getObject(GetObjectRequest getObjectRequest, AsyncResponseTransformer asyncResponseTransformer) {
        String uri = "s3://" + getObjectRequest.bucket() + "/" + getObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "getObject");
        S3MetricUtil.reportExternalMetrics(segment, uri, "getObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ReturnT> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<ListObjectsResponse> listObjects(ListObjectsRequest listObjectsRequest) {
        String uri = "s3://" + listObjectsRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "listObjects");
        S3MetricUtil.reportExternalMetrics(segment, uri, "listObjects");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ListObjectsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<PutObjectResponse> putObject(PutObjectRequest putObjectRequest, AsyncRequestBody asyncRequestBody) {
        String uri = "s3://" + putObjectRequest.bucket() + "/" + putObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "putObject");
        S3MetricUtil.reportExternalMetrics(segment, uri, "putObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<PutObjectResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<DeleteObjectResponse> deleteObject(DeleteObjectRequest deleteObjectRequest) {
        String uri = "s3://" + deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteObject");
        S3MetricUtil.reportExternalMetrics(segment, uri, "deleteObject");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteObjectResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    public CompletableFuture<DeleteObjectsResponse> deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        String uri = "s3://" + deleteObjectsRequest.bucket();
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("S3", "deleteObjects");
        S3MetricUtil.reportExternalMetrics(segment, uri, "deleteObjects");
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<DeleteObjectsResponse> result = Weaver.callOriginal();

        return result.whenComplete(new ResultWrapper<>(segment));
    }

    private class ResultWrapper<T, U> implements BiConsumer<T, U> {
        private Segment segment;

        public ResultWrapper(Segment segment) {
            this.segment = segment;
        }

        @Override
        public void accept(T t, U u) {
            try {
                segment.end();
            } catch (Throwable t1) {
                AgentBridge.instrumentation.noticeInstrumentationError(t1, Weaver.getImplementationTitle());
            }
        }
    }
}
