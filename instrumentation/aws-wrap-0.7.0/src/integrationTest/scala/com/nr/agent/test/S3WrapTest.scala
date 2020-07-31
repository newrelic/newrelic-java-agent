package com.nr.agent.test

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.services.s3.model.{ Bucket, ListBucketsRequest, ListObjectsRequest, ObjectListing }
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client }
import com.github.dwhjames.awswrap.s3.AmazonS3ScalaClient
import com.newrelic.agent.introspec.{ ExternalRequest, InstrumentationTestConfig, InstrumentationTestRunner, Introspector, MetricsHelper, TransactionEvent }
import com.newrelic.api.agent.Trace
import org.junit.{Test, Assert}
import org.junit.runner.RunWith

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._

import java.util.Collection;

@RunWith(classOf[InstrumentationTestRunner])
@InstrumentationTestConfig(includePrefixes = Array("com.github.dwhjames.awswrap", "com.amazonaws.services.s3"))
class S3WrapTest {
  private val bucketName: String = "nr-java-agent-aits"
  private val host: String = "amazon"
  private val client: String = "S3"

  private def assertAwsMetrics(operation: String): Unit = {
    val introspector :Introspector = InstrumentationTestRunner.getIntrospector()
    Assert.assertEquals(1, introspector.getFinishedTransactionCount())
    val clientTx: String = introspector.getTransactionNames().iterator().next()

    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/allOther"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/"+host+"/all"));
    Assert.assertEquals(1, MetricsHelper.getUnscopedMetricCount("External/"+host+"/"+client));

    // Events
    val transactionEvents :Collection[TransactionEvent] = introspector.getTransactionEvents(clientTx);
    Assert.assertEquals(1, transactionEvents.size());
    val transactionEvent :TransactionEvent = transactionEvents.iterator().next();
    Assert.assertEquals(1, transactionEvent.getExternalCallCount());

    // tracer's metric name: External/amazon/S3
    val scopedMetricName :String = "External/"+host+"/"+client;
    Assert.assertEquals(1, MetricsHelper.getScopedMetricCount(clientTx, scopedMetricName));
    // tracer's segment name example: External/amazon/S3/listBuckets
    val segmentName :String = "External/"+host+"/"+client+"/"+operation;

    val externalRequests :Collection[ExternalRequest]  = introspector.getExternalRequests(clientTx);
    Assert.assertEquals(1, externalRequests.size());
    val externalRequest : ExternalRequest = externalRequests.iterator().next();
    Assert.assertEquals(1, externalRequest.getCount());
    Assert.assertEquals(host, externalRequest.getHostname());
    Assert.assertEquals(client, externalRequest.getLibrary());
    Assert.assertEquals(operation, externalRequest.getOperation());
  }

  /**
    * Assert that AWS instrumentation is working normally.
    */
  @Test
  def testAwsInstrumentation() {
    syncAwsRequest()
    assertAwsMetrics("listObjects")
  }

  @Trace(dispatcher = true)
  def syncAwsRequest(): Unit = {
    val s3client: AmazonS3  = new AmazonS3Client(new ProfileCredentialsProvider())
    Assert.assertNotNull(s3client)
    val listObjReq: ListObjectsRequest = new ListObjectsRequest()
    listObjReq.setBucketName(bucketName)
    val objsInBucket: ObjectListing = s3client.listObjects(listObjReq)
    System.out.println("Got objects from bucket: "+objsInBucket.getBucketName)
    Assert.assertEquals(bucketName, objsInBucket.getBucketName)
  }

  @Test
  def testAsyncListObjects() {
    asyncListObjects()
    assertAwsMetrics("listObjects")
  }

  @Trace(dispatcher = true)
  def asyncListObjects(): Unit = {
    val s3client: AmazonS3ScalaClient  = new AmazonS3ScalaClient(new ProfileCredentialsProvider())
    Assert.assertNotNull(s3client)
    val listObjReq: ListObjectsRequest = new ListObjectsRequest()
    listObjReq.setBucketName(bucketName)
    val objsInBucket: ObjectListing = Await.result(s3client.listObjects(listObjReq), 30 seconds)
    System.out.println("Got objects from bucket: "+objsInBucket.getBucketName)
    Assert.assertEquals(bucketName, objsInBucket.getBucketName)
  }

  @Test
  def testAsyncListBuckets() {
    asyncListBuckets()
    assertAwsMetrics("listBuckets")
  }

  @Trace(dispatcher = true)
  def asyncListBuckets(): Unit = {
    val s3client: AmazonS3ScalaClient  = new AmazonS3ScalaClient(new ProfileCredentialsProvider())
    Assert.assertNotNull(s3client)
    val listBucketsReq: ListBucketsRequest = new ListBucketsRequest()
    val buckets: Seq[Bucket] = Await.result(s3client.listBuckets(), 30 seconds)
    Assert.assertTrue(buckets.size > 0)
  }
}
