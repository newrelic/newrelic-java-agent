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
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "createBucket");
        return Weaver.callOriginal();
    }

    @Trace
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "deleteBucket");
        Weaver.callOriginal();
    }

    @Trace
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "listBuckets");
        return Weaver.callOriginal();
    }

    @Trace
    public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "getBucketLocation");
        return Weaver.callOriginal();
    }

    @Trace
    public S3Object getObject(GetObjectRequest getObjectRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "getObject");
        return Weaver.callOriginal();
    }

    @Trace
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "listObjects");
        return Weaver.callOriginal();
    }

    @Trace
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "putObject");
        return Weaver.callOriginal();
    }

    @Trace
    public void deleteObject(DeleteObjectRequest deleteObjectRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "deleteObject");
        Weaver.callOriginal();
    }

    @Trace
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) {
        S3MetricUtil.metrics(AgentBridge.getAgent().getTransaction(), AgentBridge.getAgent().getTracedMethod(),
                "deleteObjects");
        return Weaver.callOriginal();
    }
}
