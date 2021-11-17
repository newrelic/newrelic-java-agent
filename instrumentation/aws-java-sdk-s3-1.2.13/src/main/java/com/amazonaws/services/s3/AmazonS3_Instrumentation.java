/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.amazonaws.services.s3;

import com.agent.instrumentation.awsjavasdk1330.services.s3.S3MetricUtil;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.*;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import java.util.List;

/**
 * This provides external instrumentation for Amazon's S3 Java API 1.3.30+. Metrics are all generated in
 * {@link S3MetricUtil} - all that's different from one method to another is the method name.
 */
@Weave(type = MatchType.Interface, originalName = "com.amazonaws.services.s3.AmazonS3")
public class AmazonS3_Instrumentation {

    @Trace
    public Bucket createBucket(CreateBucketRequest createBucketRequest) {
        Integer statusCode = null;
        try {
            Bucket bucket = Weaver.callOriginal();
            statusCode = 200;
            return bucket;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + createBucketRequest.getBucketName();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "createBucket");
        }

    }

    @Trace
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        Integer statusCode = null;
        try {
            Weaver.callOriginal();
            statusCode = 200;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + deleteBucketRequest.getBucketName();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "deleteBucket");
        }
    }

    @Trace
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest) {
        Integer statusCode = null;
        try {
            List<Bucket> buckets = Weaver.callOriginal();
            statusCode = 200;
            return buckets;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://amazon/";
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "listBuckets");
        }
    }

    @Trace
    public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        Integer statusCode = null;
        try {
            String bucketLocation = Weaver.callOriginal();
            statusCode = 200;
            return bucketLocation;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + getBucketLocationRequest.getBucketName();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "getBucketLocation");
        }
    }

    @Trace
    public S3Object getObject(GetObjectRequest getObjectRequest) {
        Integer statusCode = null;
        try {
            S3Object s3Object = Weaver.callOriginal();
            statusCode = 200;
            return s3Object;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + getObjectRequest.getBucketName() + "/" + getObjectRequest.getKey();
            System.out.println("URI:"  + uri);
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "getObject");
        }
    }

    @Trace
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) {
        Integer statusCode = null;
        try {
            ObjectListing objectListing = Weaver.callOriginal();
            statusCode = 200;
            return objectListing;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + listObjectsRequest.getBucketName();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "listObjects");
        }
    }

    @Trace
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
        Integer statusCode = null;
        try {
            PutObjectResult putObjectResult = Weaver.callOriginal();
            statusCode = 200;
            return putObjectResult;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + putObjectRequest.getBucketName() + "/" + putObjectRequest.getKey();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "putObject");
        }
    }

    @Trace
    public void deleteObject(DeleteObjectRequest deleteObjectRequest) {
        Integer statusCode = null;
        try {
            Weaver.callOriginal();
            statusCode = 200;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + deleteObjectRequest.getBucketName() + "/" + deleteObjectRequest.getKey();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "deleteObject");
        }
    }

    @Trace
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        Integer statusCode = null;
        try {
            DeleteObjectsResult deleteObjectsResult = Weaver.callOriginal();
            statusCode = 200;
            return deleteObjectsResult;
        } catch (AmazonServiceException exception) {
            statusCode = exception.getStatusCode();
            throw exception;
        } finally {
            String uri = "s3://" + deleteObjectsRequest.getBucketName();
            S3MetricUtil.reportExternalMetrics(NewRelic.getAgent().getTracedMethod(), uri, statusCode, "deleteObjects");
        }

    }
}
