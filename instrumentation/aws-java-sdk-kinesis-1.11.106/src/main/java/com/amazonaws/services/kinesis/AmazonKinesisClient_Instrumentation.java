package com.amazonaws.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.kinesis.KinesisUtil;
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
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.amazonaws.services.kinesis.AmazonKinesisClient", type = MatchType.ExactClass)
public class AmazonKinesisClient_Instrumentation {

    @Trace(async = true, leaf = true)
    final AddTagsToStreamResult executeAddTagsToStream(AddTagsToStreamRequest request) {
        KinesisUtil.setTraceInformation("addTagsToStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final CreateStreamResult executeCreateStream(CreateStreamRequest request) {
        KinesisUtil.setTraceInformation("createStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DecreaseStreamRetentionPeriodResult executeDecreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceInformation("decreaseStreamRetentionPeriod", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DeleteStreamResult executeDeleteStream(DeleteStreamRequest request) {
        KinesisUtil.setTraceInformation("deleteStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DescribeLimitsResult executeDescribeLimits(DescribeLimitsRequest request) {
        KinesisUtil.setTraceInformation("describeLimits", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DescribeStreamResult executeDescribeStream(DescribeStreamRequest request) {
        KinesisUtil.setTraceInformation("describeStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final DisableEnhancedMonitoringResult executeDisableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceInformation("disableEnhancedMonitoring", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final EnableEnhancedMonitoringResult executeEnableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceInformation("enableEnhancedMonitoring", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final GetRecordsResult executeGetRecords(GetRecordsRequest request) {
        KinesisUtil.setTraceInformation("getRecords", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final GetShardIteratorResult executeGetShardIterator(GetShardIteratorRequest request) {
        KinesisUtil.setTraceInformation("getShardIterator", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final IncreaseStreamRetentionPeriodResult executeIncreaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceInformation("increaseStreamRetentionPeriod", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final ListStreamsResult executeListStreams(ListStreamsRequest request) {
        KinesisUtil.setTraceInformation("listStreams", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final ListTagsForStreamResult executeListTagsForStream(ListTagsForStreamRequest request) {
        KinesisUtil.setTraceInformation("listTagsForStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final MergeShardsResult executeMergeShards(MergeShardsRequest request) {
        KinesisUtil.setTraceInformation("mergeShards", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final PutRecordResult executePutRecord(PutRecordRequest request) {
        KinesisUtil.setTraceInformation("putRecord", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final PutRecordsResult executePutRecords(PutRecordsRequest request) {
        KinesisUtil.setTraceInformation("putRecords", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final RemoveTagsFromStreamResult executeRemoveTagsFromStream(RemoveTagsFromStreamRequest request) {
        KinesisUtil.setTraceInformation("removeTagsFromStream", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final SplitShardResult executeSplitShard(SplitShardRequest request) {
        KinesisUtil.setTraceInformation("splitShard", request);
        return Weaver.callOriginal();
    }

    @Trace(async = true, leaf = true)
    final UpdateShardCountResult executeUpdateShardCount(UpdateShardCountRequest request) {
        KinesisUtil.setTraceInformation("updateShardCount", request);
        return Weaver.callOriginal();
    }

}
