/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package com.agent.instrumentation.awsjavasdks3;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.api.agent.Trace;
import io.findify.s3mock.S3Mock;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import static com.agent.instrumentation.awsjavasdks3.S3MetricAssertions.assertMetrics;

@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = { "software.amazon.awssdk" })
public class AmazonS3SyncApiTest {

    private static final String BUCKET = "testbucket";
    private static final String KEY = "testfile";

    private static String PATH;
    private static S3Client client;
    private static S3Mock api;

    private File testfile;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void before() {
        try {
            testfile = folder.newFile("testfile.txt");
        } catch (IOException e) {
        }
        PATH = testfile.getPath();
    }

    @BeforeClass
    public static void beforeClass() throws URISyntaxException {
        int port = InstrumentationTestRunner.getIntrospector().getRandomPort();
        api = new S3Mock.Builder().withPort(port).withInMemoryBackend().build();
        api.start();
        client = S3Client.builder()
                .credentialsProvider(() -> AwsBasicCredentials.create("access key", "secret access key"))
                .region(Region.AWS_GLOBAL)
                .endpointOverride(new URI("http://localhost:" + port))
                .build();
    }

    @AfterClass
    public static void afterClass() {
        api.shutdown();
    }

    @Test
    public void testCreateBucket() {
        createBucket();
        assertMetrics("createBucket");
    }

    @Test
    public void testDeleteBucket() {
        createBucketNoTxn();
        deleteBucket();
        assertMetrics("deleteBucket");
    }

    @Test
    public void testListBuckets() {
        listBuckets();
        assertMetrics("listBuckets");
    }

    @Test
    public void testGetBucketLocation() {
        getBucketLocation();
        assertMetrics("getBucketLocation");
    }

    @Test
    public void testGetObject() {
        createBucketNoTxn();
        putObjectNoTxn();
        getObject();
        assertMetrics("getObject");
    }

    @Test
    public void testListObjects() {
        createBucketNoTxn();
        listObjects();
        assertMetrics("listObjects");
    }

    @Test
    public void testPutObject() {
        createBucketNoTxn();
        putObject();
        assertMetrics("putObject");
    }

    @Test
    public void testDeleteObject() {
        createBucketNoTxn();
        putObjectNoTxn();
        deleteObject();
        assertMetrics("deleteObject");
    }

    @Test
    public void testDeleteObjects() {
        createBucketNoTxn();
        putObjectNoTxn();
        deleteObjects();
        assertMetrics("deleteObjects");
    }

    @Trace(dispatcher = true)
    public void deleteBucket() {
        DeleteBucketRequest createBucketRequest = DeleteBucketRequest.builder()
                .bucket(BUCKET)
                .build();
        client.deleteBucket(createBucketRequest);
    }

    @Trace(dispatcher = true)
    public void listBuckets() {
        client.listBuckets();
    }

    @Trace(dispatcher = true)
    public void getBucketLocation() {
        GetBucketLocationRequest getBucketLocationRequest = GetBucketLocationRequest.builder()
                .bucket(BUCKET)
                .build();
        client.getBucketLocation(getBucketLocationRequest);
    }

    @Trace(dispatcher = true)
    public void getObject() {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(KEY)
                .build();
        client.getObject(getObjectRequest, (response, inputStream) -> null);
    }

    @Trace(dispatcher = true)
    public void listObjects() {
        ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder()
                .bucket(BUCKET)
                .build();
        client.listObjects(listObjectsRequest);
    }

    @Trace(dispatcher = true)
    public void putObject() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .key(KEY)
                .bucket(BUCKET)
                .build();
        client.putObject(putObjectRequest, Paths.get(PATH));
    }

    @Trace(dispatcher = true)
    public void deleteObject() {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .key(KEY)
                .bucket(BUCKET)
                .build();
        client.deleteObject(deleteObjectRequest);
    }

    @Trace(dispatcher = true)
    public void deleteObjects() {
        DeleteObjectsRequest deleteObjectsRequest = DeleteObjectsRequest.builder()
                .bucket(BUCKET)
                .delete(Delete.builder().build())
                .build();
        client.deleteObjects(deleteObjectsRequest);
    }

    @Trace(dispatcher = true)
    private void createBucket() {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build();
        client.createBucket(createBucketRequest);
    }

    private static void createBucketNoTxn() {
        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build();
        client.createBucket(createBucketRequest);
    }

    private static void putObjectNoTxn() {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .key(KEY)
                .bucket(BUCKET)
                .build();
        client.putObject(putObjectRequest, Paths.get(PATH));
    }
}

