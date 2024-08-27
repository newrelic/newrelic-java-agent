package com.amazonaws.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.lambda.KinesisUtil;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.kinesis.model.AddTagsToStreamRequest;
import com.amazonaws.services.kinesis.model.AddTagsToStreamResult;
import com.amazonaws.services.kinesis.model.CreateStreamRequest;
import com.amazonaws.services.kinesis.model.CreateStreamResult;
import com.amazonaws.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import com.amazonaws.services.kinesis.model.DecreaseStreamRetentionPeriodResult;
import com.amazonaws.services.kinesis.model.DeleteStreamRequest;
import com.amazonaws.services.kinesis.model.DeleteStreamResult;
import com.amazonaws.services.kinesis.model.DescribeLimitsRequest;
import com.amazonaws.services.kinesis.model.DescribeLimitsResult;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.DisableEnhancedMonitoringRequest;
import com.amazonaws.services.kinesis.model.DisableEnhancedMonitoringResult;
import com.amazonaws.services.kinesis.model.EnableEnhancedMonitoringRequest;
import com.amazonaws.services.kinesis.model.EnableEnhancedMonitoringResult;
import com.amazonaws.services.kinesis.model.GetRecordsRequest;
import com.amazonaws.services.kinesis.model.GetRecordsResult;
import com.amazonaws.services.kinesis.model.GetShardIteratorRequest;
import com.amazonaws.services.kinesis.model.GetShardIteratorResult;
import com.amazonaws.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import com.amazonaws.services.kinesis.model.IncreaseStreamRetentionPeriodResult;
import com.amazonaws.services.kinesis.model.ListStreamsRequest;
import com.amazonaws.services.kinesis.model.ListStreamsResult;
import com.amazonaws.services.kinesis.model.ListTagsForStreamRequest;
import com.amazonaws.services.kinesis.model.ListTagsForStreamResult;
import com.amazonaws.services.kinesis.model.MergeShardsRequest;
import com.amazonaws.services.kinesis.model.MergeShardsResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.RemoveTagsFromStreamRequest;
import com.amazonaws.services.kinesis.model.RemoveTagsFromStreamResult;
import com.amazonaws.services.kinesis.model.SplitShardRequest;
import com.amazonaws.services.kinesis.model.SplitShardResult;
import com.amazonaws.services.kinesis.model.UpdateShardCountRequest;
import com.amazonaws.services.kinesis.model.UpdateShardCountResult;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.amazonaws.services.kinesis.AmazonKinesisClient", type = MatchType.ExactClass)
public class AmazonKinesisClient_Instrumentation {

    @Trace(leaf=true)
    final AddTagsToStreamResult executeAddTagsToStream(AddTagsToStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("addTagsToStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final CreateStreamResult executeCreateStream(CreateStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("createStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final DecreaseStreamRetentionPeriodResult executeDecreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("decreaseStreamRetentionPeriod");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final DeleteStreamResult executeDeleteStream(DeleteStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("deleteStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final DescribeLimitsResult executeDescribeLimits(DescribeLimitsRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("describeLimits");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final DescribeStreamResult executeDescribeStream(DescribeStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("describeStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final DisableEnhancedMonitoringResult executeDisableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("disableEnhancedMonitoring");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final EnableEnhancedMonitoringResult executeEnableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("enableEnhancedMonitoring");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final GetRecordsResult executeGetRecords(GetRecordsRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("getRecords");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final GetShardIteratorResult executeGetShardIterator(GetShardIteratorRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("getShardIterator");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final IncreaseStreamRetentionPeriodResult executeIncreaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("increaseStreamRetentionPeriod");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final ListStreamsResult executeListStreams(ListStreamsRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("listStreams");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final ListTagsForStreamResult executeListTagsForStream(ListTagsForStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("listTagsForStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final MergeShardsResult executeMergeShards(MergeShardsRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("mergeShards");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final PutRecordResult executePutRecord(PutRecordRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("putRecord");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final PutRecordsResult executePutRecords(PutRecordsRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("putRecords");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final RemoveTagsFromStreamResult executeRemoveTagsFromStream(RemoveTagsFromStreamRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("removeTagsFromStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final SplitShardResult executeSplitShard(SplitShardRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("splitShard");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    final UpdateShardCountResult executeUpdateShardCount(UpdateShardCountRequest request) {
        KinesisUtil.linkAndExpireToken(request);
        KinesisUtil.setTraceDetails("updateShardCount");
        return Weaver.callOriginal();
    }

}
