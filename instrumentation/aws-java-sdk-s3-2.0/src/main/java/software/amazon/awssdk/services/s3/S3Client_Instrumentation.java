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
        try {
            CreateBucketResponse createBucketResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "createBucket", createBucketResponse);
            return createBucketResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "createBucket", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "createBucket", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public DeleteBucketResponse deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        String uri = "s3://" + deleteBucketRequest.bucket();
        try {
            DeleteBucketResponse deleteBucketResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteBucket", deleteBucketResponse);
            return deleteBucketResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteBucket", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteBucket", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public ListBucketsResponse listBuckets(ListBucketsRequest listBucketsRequest) {
        String uri = "s3://amazon/";
        try {
            ListBucketsResponse listBucketsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listBuckets", listBucketsResponse);
            return listBucketsResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listBuckets", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listBuckets", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public GetBucketLocationResponse getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        String uri = "s3://" + getBucketLocationRequest.bucket();
        try {
            GetBucketLocationResponse getBucketLocationResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getBucketLocation", getBucketLocationResponse);
            return getBucketLocationResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getBucketLocation", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getBucketLocation", (Integer) null);
            throw exception;
        }
    }

    /*
     * This method does not return an S3Response like all the others, so the instrumentation is drastically different.
     * If the method returns properly, it is assumed as a 200 status.
     * When AwsServiceException is thrown, the status code is retrieved from the exception.
     * Other exceptions may be thrown, but they indicate something happened before the HTTP request, so there would be no status to record.
     */
    @Trace
    public <T> T getObject(GetObjectRequest getObjectRequest, ResponseTransformer responseTransformer) {
        String uri = "s3://" + getObjectRequest.bucket() + "/" + getObjectRequest.key();
        Integer statusCode = null;
        try {
            T t = Weaver.callOriginal();
            statusCode = 200;
            return t;
        } catch (AwsServiceException awsServiceException) {
            statusCode = awsServiceException.statusCode();
            throw awsServiceException;
        } finally {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "getObject", statusCode);
        }
    }

    @Trace
    public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) {
        String uri = "s3://" + listObjectsRequest.bucket();
        try {
            ListObjectsResponse listObjectsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listObjects", listObjectsResponse);
            return listObjectsResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listObjects", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "listObjects", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody RequestBody) {
        String uri = "s3://" + putObjectRequest.bucket() + "/" + putObjectRequest.key();
        try {
            PutObjectResponse putObjectResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "putObject", putObjectResponse);
            return putObjectResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "putObject", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "putObject", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest) {
        String uri = "s3://" + deleteObjectRequest.bucket() + "/" + deleteObjectRequest.key();
        try {
            DeleteObjectResponse deleteObjectResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObject", deleteObjectResponse);
            return deleteObjectResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObject", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObject", (Integer) null);
            throw exception;
        }
    }

    @Trace
    public DeleteObjectsResponse deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        String uri = "s3://" + deleteObjectsRequest.bucket();
        try {
            DeleteObjectsResponse deleteObjectsResponse = Weaver.callOriginal();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObjects", deleteObjectsResponse);
            return deleteObjectsResponse;
        } catch (AwsServiceException awsException) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObjects", awsException.statusCode());
            throw awsException;
        } catch (Exception exception) {
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, "deleteObjects", (Integer) null);
            throw exception;
        }
    }
}
