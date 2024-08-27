package com.amazonaws.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.lambda.KinesisUtil;
import com.amazonaws.handlers.AsyncHandler_Instrumentation;
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
import com.newrelic.api.agent.weaver.Weaver;

import java.util.concurrent.Future;

public class AmazonKinesisAsyncClient_Instrumentation {

    @Trace
    public Future<AddTagsToStreamResult> addTagsToStreamAsync(AddTagsToStreamRequest request,
            AsyncHandler_Instrumentation<AddTagsToStreamRequest, AddTagsToStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<CreateStreamResult> createStreamAsync(CreateStreamRequest request,
            AsyncHandler_Instrumentation<CreateStreamRequest, CreateStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DecreaseStreamRetentionPeriodResult> decreaseStreamRetentionPeriodAsync(
            DecreaseStreamRetentionPeriodRequest request,
            AsyncHandler_Instrumentation<DecreaseStreamRetentionPeriodRequest, DecreaseStreamRetentionPeriodResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DeleteStreamResult> deleteStreamAsync(DeleteStreamRequest request,
            AsyncHandler_Instrumentation<DeleteStreamRequest, DeleteStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DescribeLimitsResult> describeLimitsAsync(DescribeLimitsRequest request,
            AsyncHandler_Instrumentation<DescribeLimitsRequest, DescribeLimitsResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DescribeStreamResult> describeStreamAsync(DescribeStreamRequest request,
            AsyncHandler_Instrumentation<DescribeStreamRequest, DescribeStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<DisableEnhancedMonitoringResult> disableEnhancedMonitoringAsync(DisableEnhancedMonitoringRequest request,
            AsyncHandler_Instrumentation<DisableEnhancedMonitoringRequest, DisableEnhancedMonitoringResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<EnableEnhancedMonitoringResult> enableEnhancedMonitoringAsync(EnableEnhancedMonitoringRequest request,
            AsyncHandler_Instrumentation<EnableEnhancedMonitoringRequest, EnableEnhancedMonitoringResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<GetRecordsResult> getRecordsAsync(GetRecordsRequest request, AsyncHandler_Instrumentation<GetRecordsRequest, GetRecordsResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<GetShardIteratorResult> getShardIteratorAsync(GetShardIteratorRequest request,
            AsyncHandler_Instrumentation<GetShardIteratorRequest, GetShardIteratorResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<IncreaseStreamRetentionPeriodResult> increaseStreamRetentionPeriodAsync(
            IncreaseStreamRetentionPeriodRequest request,
            AsyncHandler_Instrumentation<IncreaseStreamRetentionPeriodRequest, IncreaseStreamRetentionPeriodResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<ListStreamsResult> listStreamsAsync(ListStreamsRequest request, AsyncHandler_Instrumentation<ListStreamsRequest, ListStreamsResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<ListTagsForStreamResult> listTagsForStreamAsync(ListTagsForStreamRequest request,
            AsyncHandler_Instrumentation<ListTagsForStreamRequest, ListTagsForStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<MergeShardsResult> mergeShardsAsync(MergeShardsRequest request, AsyncHandler_Instrumentation<MergeShardsRequest, MergeShardsResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<PutRecordResult> putRecordAsync(PutRecordRequest request, AsyncHandler_Instrumentation<PutRecordRequest, PutRecordResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<PutRecordsResult> putRecordsAsync(PutRecordsRequest request, AsyncHandler_Instrumentation<PutRecordsRequest, PutRecordsResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<RemoveTagsFromStreamResult> removeTagsFromStreamAsync(RemoveTagsFromStreamRequest request,
            AsyncHandler_Instrumentation<RemoveTagsFromStreamRequest, RemoveTagsFromStreamResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<SplitShardResult> splitShardAsync(SplitShardRequest request, AsyncHandler_Instrumentation<SplitShardRequest, SplitShardResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }

    @Trace
    public Future<UpdateShardCountResult> updateShardCountAsync(UpdateShardCountRequest request,
            AsyncHandler_Instrumentation<UpdateShardCountRequest, UpdateShardCountResult> asyncHandler) {
        KinesisUtil.setupToken(asyncHandler, request);
        return Weaver.callOriginal();
    }
}
