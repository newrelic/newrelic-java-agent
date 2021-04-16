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
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
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
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Weave(type = MatchType.Interface, originalName = "software.amazon.awssdk.services.s3.S3Client")
public class S3Client_Instrumentation {

    @Trace
    public CreateBucketResponse createBucket(CreateBucketRequest createBucketRequest) {
        String uri = "s3://" + createBucketRequest.bucket();
        try {
            CreateBucketResponse createBucketResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, createBucketResponse, "createBucket");
            return createBucketResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "createBucket");
            throw e;
        }
    }

    @Trace
    public DeleteBucketResponse deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        String uri = "s3://" + deleteBucketRequest.bucket();
        try {
            DeleteBucketResponse deleteBucketResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, deleteBucketResponse, "deleteBucket");
            return deleteBucketResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteBucket");
            throw e;
        }
    }

    @Trace
    public ListBucketsResponse listBuckets(ListBucketsRequest listBucketsRequest) {
        String uri = "s3://amazon/";
        try {
            ListBucketsResponse listBucketsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, listBucketsResponse, "listBuckets");
            return listBucketsResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listBuckets");
            throw e;
        }
    }

    @Trace
    public GetBucketLocationResponse getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        String uri = "s3://" + getBucketLocationRequest.bucket();
        try {
            GetBucketLocationResponse getBucketLocationResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, getBucketLocationResponse, "getBucketLocation");
            return getBucketLocationResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getBucketLocation");
            throw e;
        }
    }

    /*
     * This method does not return an S3Response like all the others, so the instrumentation is drastically different.
     * If the method returns properly, it is assumed as a 200 status.
     * Exceptional cases either use the status on the exception or assume a specific value.
     * The original method may throw a SdkClientException, which may happen before the request is made, thus no status code.
     * statusCode is an Integer to account for this last case.
     */
    @Trace
    public <T> T getObject(GetObjectRequest getObjectRequest, ResponseTransformer responseTransformer) {
        String uri = "s3://" + getObjectRequest.bucket() + "/" + getObjectRequest.key();
        Integer statusCode = null;
        try {
            T t = Weaver.callOriginal();
            statusCode = 200;
            return t;
        } catch (NoSuchKeyException noSuchKeyException) {
            statusCode = 404;
            throw noSuchKeyException;
        } catch (AwsServiceException awsServiceException) {
            statusCode = awsServiceException.statusCode();
            throw awsServiceException;
        } finally {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "getObject");
        }
    }

    @Trace
    public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) {
        String uri = "s3://" + listObjectsRequest.bucket();
        try {
            ListObjectsResponse listObjectsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, listObjectsResponse, "listObjects");
            return listObjectsResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listObjects");
            throw e;
        }
    }

    @Trace
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody RequestBody) {
        String uri = "s3://" + putObjectRequest.bucket() + "/" + putObjectRequest.key();
        try {
            PutObjectResponse putObjectResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, putObjectResponse, "putObject");
            return putObjectResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "putObject");
            throw e;
        }
    }

    @Trace
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        String uri = "s3://" + deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key();
        try {
            DeleteObjectResponse deleteObjectResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, deleteObjectResponse, "deleteObject");
            return deleteObjectResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObject");
            throw e;
        }
    }

    @Trace
    public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        String uri = "s3://" + deleteObjectsRequest.bucket();
        try {
            DeleteObjectsResponse deleteObjectsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, deleteObjectsResponse, "deleteObjects");
            return deleteObjectsResponse;
        } catch (Exception e) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObjects");
            throw e;
        }
    }
}

