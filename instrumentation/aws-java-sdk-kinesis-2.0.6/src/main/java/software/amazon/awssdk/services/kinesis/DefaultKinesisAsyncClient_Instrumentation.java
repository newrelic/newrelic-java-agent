package software.amazon.awssdk.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.lambda.KinesisUtil;
import com.agent.instrumentation.awsjavasdk2.services.lambda.SegmentHandler;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.services.kinesis.model.AddTagsToStreamRequest;
import software.amazon.awssdk.services.kinesis.model.AddTagsToStreamResponse;
import software.amazon.awssdk.services.kinesis.model.CreateStreamRequest;
import software.amazon.awssdk.services.kinesis.model.CreateStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.DecreaseStreamRetentionPeriodResponse;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DeleteStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DeregisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DeregisterStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DisableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.DisableEnhancedMonitoringResponse;
import software.amazon.awssdk.services.kinesis.model.EnableEnhancedMonitoringRequest;
import software.amazon.awssdk.services.kinesis.model.EnableEnhancedMonitoringResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodRequest;
import software.amazon.awssdk.services.kinesis.model.IncreaseStreamRetentionPeriodResponse;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamConsumersRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamConsumersResponse;
import software.amazon.awssdk.services.kinesis.model.ListStreamsRequest;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.ListTagsForStreamRequest;
import software.amazon.awssdk.services.kinesis.model.ListTagsForStreamResponse;
import software.amazon.awssdk.services.kinesis.model.MergeShardsRequest;
import software.amazon.awssdk.services.kinesis.model.MergeShardsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.RegisterStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamRequest;
import software.amazon.awssdk.services.kinesis.model.RemoveTagsFromStreamResponse;
import software.amazon.awssdk.services.kinesis.model.SplitShardRequest;
import software.amazon.awssdk.services.kinesis.model.SplitShardResponse;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StartStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionRequest;
import software.amazon.awssdk.services.kinesis.model.StopStreamEncryptionResponse;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardRequest;
import software.amazon.awssdk.services.kinesis.model.SubscribeToShardResponseHandler;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountResponse;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.services.kinesis.DefaultKinesisAsyncClient", type = MatchType.ExactClass)
class DefaultKinesisAsyncClient_Instrumentation {
    
    public CompletableFuture<AddTagsToStreamResponse> addTagsToStream(AddTagsToStreamRequest addTagsToStreamRequest) {
        Segment segment = KinesisUtil.startSegment("addTagsToStream");
        CompletableFuture<AddTagsToStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<CreateStreamResponse> createStream(CreateStreamRequest createStreamRequest) {
        Segment segment = KinesisUtil.startSegment("createStream");
        CompletableFuture<CreateStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
            DecreaseStreamRetentionPeriodRequest decreaseStreamRetentionPeriodRequest) {
        Segment segment = KinesisUtil.startSegment("decreaseStreamRetentionPeriod");
        CompletableFuture<DecreaseStreamRetentionPeriodResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DeleteStreamResponse> deleteStream(DeleteStreamRequest deleteStreamRequest) {
        Segment segment = KinesisUtil.startSegment("deleteStream");
        CompletableFuture<DeleteStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer(DeregisterStreamConsumerRequest deregisterStreamConsumerRequest) {
        Segment segment = KinesisUtil.startSegment("deregisterStreamConsumer");
        CompletableFuture<DeregisterStreamConsumerResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        Segment segment = KinesisUtil.startSegment("describeLimits");
        CompletableFuture<DescribeLimitsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DescribeStreamResponse> describeStream(DescribeStreamRequest describeStreamRequest) {
        Segment segment = KinesisUtil.startSegment("describeStream");
        CompletableFuture<DescribeStreamResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(DescribeStreamConsumerRequest describeStreamConsumerRequest) {
        Segment segment = KinesisUtil.startSegment("describeStreamConsumer");
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamSummary(DescribeStreamSummaryRequest describeStreamSummaryRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("KinesisAsyncClient", "describeStreamSummary");
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(DisableEnhancedMonitoringRequest disableEnhancedMonitoringRequest) {
        Segment segment = KinesisUtil.startSegment("disableEnhancedMonitoring");
        CompletableFuture<DisableEnhancedMonitoringResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    public CompletableFuture<EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(EnableEnhancedMonitoringRequest enableEnhancedMonitoringRequest) {
        Segment segment = KinesisUtil.startSegment("enableEnhancedMonitoring");
        CompletableFuture<EnableEnhancedMonitoringResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<GetRecordsResponse> getRecords(GetRecordsRequest getRecordsRequest) {
        Segment segment = KinesisUtil.startSegment("getRecords");
        CompletableFuture<GetRecordsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<GetShardIteratorResponse> getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {
        Segment segment = KinesisUtil.startSegment("getShardIterator");
        CompletableFuture<GetShardIteratorResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
            IncreaseStreamRetentionPeriodRequest increaseStreamRetentionPeriodRequest) {
        Segment segment = KinesisUtil.startSegment("increaseStreamRetentionPeriod");
        CompletableFuture<IncreaseStreamRetentionPeriodResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListShardsResponse> listShards(ListShardsRequest listShardsRequest) {
        Segment segment = KinesisUtil.startSegment("listShards");
        CompletableFuture<ListShardsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListStreamConsumersResponse> listStreamConsumers(ListStreamConsumersRequest listStreamConsumersRequest) {
        Segment segment = KinesisUtil.startSegment("listStreamConsumers");
        CompletableFuture<ListStreamConsumersResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListStreamsResponse> listStreams(ListStreamsRequest listStreamsRequest) {
        Segment segment = KinesisUtil.startSegment("listStreams");
        CompletableFuture<ListStreamsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListTagsForStreamResponse> listTagsForStream(ListTagsForStreamRequest listTagsForStreamRequest) {
        Segment segment = KinesisUtil.startSegment("listTagsForStream");
        CompletableFuture<ListTagsForStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<MergeShardsResponse> mergeShards(MergeShardsRequest mergeShardsRequest) {
        Segment segment = KinesisUtil.startSegment("mergeShards");
        CompletableFuture<MergeShardsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest putRecordRequest) {
        Segment segment = KinesisUtil.startSegment("putRecord");
        CompletableFuture<PutRecordResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordsResponse> putRecords(PutRecordsRequest putRecordsRequest) {
        Segment segment = KinesisUtil.startSegment("putRecords");
        CompletableFuture<PutRecordsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<RegisterStreamConsumerResponse> registerStreamConsumer(RegisterStreamConsumerRequest registerStreamConsumerRequest) {
        Segment segment = KinesisUtil.startSegment("registerStreamConsumer");
        CompletableFuture<RegisterStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<RemoveTagsFromStreamResponse> removeTagsFromStream(RemoveTagsFromStreamRequest removeTagsFromStreamRequest) {
        Segment segment = KinesisUtil.startSegment("removeTagsFromStream");
        CompletableFuture<RemoveTagsFromStreamResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<SplitShardResponse> splitShard(SplitShardRequest splitShardRequest) {
        Segment segment = KinesisUtil.startSegment("splitShard");
        CompletableFuture<SplitShardResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StartStreamEncryptionResponse> startStreamEncryption(StartStreamEncryptionRequest startStreamEncryptionRequest) {
        Segment segment = KinesisUtil.startSegment("startStreamEncryption");
        CompletableFuture<StartStreamEncryptionResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StopStreamEncryptionResponse> stopStreamEncryption(StopStreamEncryptionRequest stopStreamEncryptionRequest) {
        Segment segment = KinesisUtil.startSegment("stopStreamEncryption");
        CompletableFuture<StopStreamEncryptionResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<Void> subscribeToShard(SubscribeToShardRequest subscribeToShardRequest, SubscribeToShardResponseHandler asyncResponseHandler) {
        Segment segment = KinesisUtil.startSegment("stopStreamEncryption");
        CompletableFuture<Void> response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<UpdateShardCountResponse> updateShardCount(UpdateShardCountRequest updateShardCountRequest) {
        Segment segment = KinesisUtil.startSegment("updateShardCount");
        CompletableFuture<UpdateShardCountResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
}
