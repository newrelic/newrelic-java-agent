/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.s3;

import com.agent.instrumentation.awsjavasdk2.services.s3.S3MetricUtil;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
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


@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.s3.S3Client")
public class S3Client_Instrumentation {

    @Trace
    public CreateBucketResponse createBucket(CreateBucketRequest createBucketRequest) {
        String uri = "s3://" + createBucketRequest.bucket();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "createBucket");
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteBucketResponse deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        String uri = "s3://" + deleteBucketRequest.bucket();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteBucket");
        return Weaver.callOriginal();
    }

    @Trace
    public ListBucketsResponse listBuckets(ListBucketsRequest listBucketsRequest) {
        String uri = "s3://amazon/";
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listBuckets");
        return Weaver.callOriginal();
    }

    @Trace
    public GetBucketLocationResponse getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        String uri = "s3://" + getBucketLocationRequest.bucket();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getBucketLocation");
        return Weaver.callOriginal();
    }

    @Trace
    public <T> T getObject(GetObjectRequest getObjectRequest, ResponseTransformer ResponseTransformer) {
        String uri = "s3://" + getObjectRequest.bucket() + "/" + getObjectRequest.key();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri,"getObject");
        return Weaver.callOriginal();
    }

    @Trace
    public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) {
        String uri = "s3://" + listObjectsRequest.bucket();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listObjects");
        return Weaver.callOriginal();
    }

    @Trace
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody RequestBody) {
        String uri = "s3://" + putObjectRequest.bucket() + "/" + putObjectRequest.key();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "putObject");
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        String uri = "s3://" + deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObject");
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        String uri = "s3://" + deleteObjectsRequest.bucket();
        S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObjects");
        return Weaver.callOriginal();
    }
}

