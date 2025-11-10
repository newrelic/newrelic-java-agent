package com.amazonaws.services.kinesis;

import com.agent.instrumentation.awsjavasdk12.services.kinesis.KinesisUtil;
import com.agent.instrumentation.awsjavasdk12.services.kinesis.StreamRawData;
import com.amazonaws.AmazonWebServiceClient;
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
import com.amazonaws.services.kinesis.model.ListShardsRequest;
import com.amazonaws.services.kinesis.model.ListShardsResult;
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
import com.amazonaws.services.kinesis.model.StartStreamEncryptionRequest;
import com.amazonaws.services.kinesis.model.StartStreamEncryptionResult;
import com.amazonaws.services.kinesis.model.StopStreamEncryptionRequest;
import com.amazonaws.services.kinesis.model.StopStreamEncryptionResult;
import com.amazonaws.services.kinesis.model.UpdateShardCountRequest;
import com.amazonaws.services.kinesis.model.UpdateShardCountResult;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "com.amazonaws.services.kinesis.AmazonKinesisClient", type = MatchType.ExactClass)
public abstract class AmazonKinesisClient_Instrumentation {
    abstract protected String getSigningRegion();

    @Trace(async = true)
    final AddTagsToStreamResult executeAddTagsToStream(AddTagsToStreamRequest request) {
        KinesisUtil.setTraceInformation("addTagsToStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final CreateStreamResult executeCreateStream(CreateStreamRequest request) {
        KinesisUtil.setTraceInformation("createStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final DecreaseStreamRetentionPeriodResult executeDecreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceInformation("decreaseStreamRetentionPeriod", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final DeleteStreamResult executeDeleteStream(DeleteStreamRequest request) {
        KinesisUtil.setTraceInformation("deleteStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final DescribeLimitsResult executeDescribeLimits(DescribeLimitsRequest request) {
        KinesisUtil.setTraceInformation("describeLimits", request, new StreamRawData(null, this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final DescribeStreamResult executeDescribeStream(DescribeStreamRequest request) {
        KinesisUtil.setTraceInformation("describeStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final DisableEnhancedMonitoringResult executeDisableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceInformation("disableEnhancedMonitoring", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final EnableEnhancedMonitoringResult executeEnableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceInformation("enableEnhancedMonitoring", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final GetRecordsResult executeGetRecords(GetRecordsRequest request) {
        KinesisUtil.setTraceInformation("getRecords", request, new StreamRawData(null, this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final GetShardIteratorResult executeGetShardIterator(GetShardIteratorRequest request) {
        KinesisUtil.setTraceInformation("getShardIterator", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final IncreaseStreamRetentionPeriodResult executeIncreaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceInformation("increaseStreamRetentionPeriod", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final ListStreamsResult executeListStreams(ListStreamsRequest request) {
        KinesisUtil.setTraceInformation("listStreams", request, new StreamRawData(null, this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final ListTagsForStreamResult executeListTagsForStream(ListTagsForStreamRequest request) {
        KinesisUtil.setTraceInformation("listTagsForStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final MergeShardsResult executeMergeShards(MergeShardsRequest request) {
        KinesisUtil.setTraceInformation("mergeShards", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final PutRecordResult executePutRecord(PutRecordRequest request) {
        KinesisUtil.setTraceInformation("putRecord", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final PutRecordsResult executePutRecords(PutRecordsRequest request) {
        KinesisUtil.setTraceInformation("putRecords", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final RemoveTagsFromStreamResult executeRemoveTagsFromStream(RemoveTagsFromStreamRequest request) {
        KinesisUtil.setTraceInformation("removeTagsFromStream", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final SplitShardResult executeSplitShard(SplitShardRequest request) {
        KinesisUtil.setTraceInformation("splitShard", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final UpdateShardCountResult executeUpdateShardCount(UpdateShardCountRequest request) {
        KinesisUtil.setTraceInformation("updateShardCount", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final ListShardsResult executeListShards(ListShardsRequest request) {
        KinesisUtil.setTraceInformation("listShards", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

    @Trace(async = true)
    final StartStreamEncryptionResult executeStartStreamEncryption(StartStreamEncryptionRequest request) {
        KinesisUtil.setTraceInformation("startStreamEncryption", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();

    }

    @Trace(async = true)
    final StopStreamEncryptionResult executeStopStreamEncryption(StopStreamEncryptionRequest request) {
        KinesisUtil.setTraceInformation("stopStreamEncryption", request, new StreamRawData(request.getStreamName(), this, getSigningRegion()));
        return Weaver.callOriginal();
    }

}
