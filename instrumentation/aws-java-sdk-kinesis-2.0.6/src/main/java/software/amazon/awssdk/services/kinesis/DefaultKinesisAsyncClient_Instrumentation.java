package software.amazon.awssdk.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.kinesis.Kinesis2Util;
import com.agent.instrumentation.awsjavasdk2.services.kinesis.SegmentHandler;
import com.agent.instrumentation.awsjavasdk2.services.kinesis.StreamRawData;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
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

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();
    
    public CompletableFuture<AddTagsToStreamResponse> addTagsToStream(AddTagsToStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("addTagsToStream", streamRawData);
        CompletableFuture<AddTagsToStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<CreateStreamResponse> createStream(CreateStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), null, this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("createStream", streamRawData);
        CompletableFuture<CreateStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DecreaseStreamRetentionPeriodResponse> decreaseStreamRetentionPeriod(
            DecreaseStreamRetentionPeriodRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("decreaseStreamRetentionPeriod", streamRawData);
        CompletableFuture<DecreaseStreamRetentionPeriodResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DeleteStreamResponse> deleteStream(DeleteStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("deleteStream", streamRawData);
        CompletableFuture<DeleteStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DeregisterStreamConsumerResponse> deregisterStreamConsumer(DeregisterStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        StreamRawData streamRawData = new StreamRawData(null, arn, this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("deregisterStreamConsumer", streamRawData);
        CompletableFuture<DeregisterStreamConsumerResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest request) {
        StreamRawData streamRawData = new StreamRawData(null, null, this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("describeLimits", streamRawData);
        CompletableFuture<DescribeLimitsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DescribeStreamResponse> describeStream(DescribeStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("describeStream", streamRawData);
        CompletableFuture<DescribeStreamResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(DescribeStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        StreamRawData streamRawData = new StreamRawData(null, arn, this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("describeStreamConsumer", streamRawData);
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamSummary(DescribeStreamSummaryRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment( "describeStreamSummary", streamRawData);
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DisableEnhancedMonitoringResponse> disableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("disableEnhancedMonitoring", streamRawData);
        CompletableFuture<DisableEnhancedMonitoringResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    public CompletableFuture<EnableEnhancedMonitoringResponse> enableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("enableEnhancedMonitoring", streamRawData);
        CompletableFuture<EnableEnhancedMonitoringResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<GetRecordsResponse> getRecords(GetRecordsRequest request) {
        StreamRawData streamRawData = new StreamRawData(null, request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("getRecords", streamRawData);
        CompletableFuture<GetRecordsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<GetShardIteratorResponse> getShardIterator(GetShardIteratorRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("getShardIterator", streamRawData);
        CompletableFuture<GetShardIteratorResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<IncreaseStreamRetentionPeriodResponse> increaseStreamRetentionPeriod(
            IncreaseStreamRetentionPeriodRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("increaseStreamRetentionPeriod", streamRawData);
        CompletableFuture<IncreaseStreamRetentionPeriodResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListShardsResponse> listShards(ListShardsRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("listShards", streamRawData);
        CompletableFuture<ListShardsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListStreamConsumersResponse> listStreamConsumers(ListStreamConsumersRequest request) {
        StreamRawData streamRawData = new StreamRawData(null, request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("listStreamConsumers", streamRawData);
        CompletableFuture<ListStreamConsumersResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListStreamsResponse> listStreams(ListStreamsRequest request) {
        StreamRawData streamRawData = new StreamRawData(null, null, this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("listStreams", streamRawData);
        CompletableFuture<ListStreamsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListTagsForStreamResponse> listTagsForStream(ListTagsForStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("listTagsForStream", streamRawData);
        CompletableFuture<ListTagsForStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<MergeShardsResponse> mergeShards(MergeShardsRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("mergeShards", streamRawData);
        CompletableFuture<MergeShardsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("putRecord", streamRawData);
        CompletableFuture<PutRecordResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordsResponse> putRecords(PutRecordsRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("putRecords", streamRawData);
        CompletableFuture<PutRecordsResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<RegisterStreamConsumerResponse> registerStreamConsumer(RegisterStreamConsumerRequest request) {
        StreamRawData streamRawData = new StreamRawData(null, request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("registerStreamConsumer", streamRawData);
        CompletableFuture<RegisterStreamConsumerResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<RemoveTagsFromStreamResponse> removeTagsFromStream(RemoveTagsFromStreamRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("removeTagsFromStream", streamRawData);
        CompletableFuture<RemoveTagsFromStreamResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<SplitShardResponse> splitShard(SplitShardRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("splitShard", streamRawData);
        CompletableFuture<SplitShardResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StartStreamEncryptionResponse> startStreamEncryption(StartStreamEncryptionRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("startStreamEncryption", streamRawData);
        CompletableFuture<StartStreamEncryptionResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StopStreamEncryptionResponse> stopStreamEncryption(StopStreamEncryptionRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("stopStreamEncryption", streamRawData);
        CompletableFuture<StopStreamEncryptionResponse> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<Void> subscribeToShard(SubscribeToShardRequest request, SubscribeToShardResponseHandler asyncResponseHandler) {
        StreamRawData streamRawData = new StreamRawData(null, request.consumerARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("stopStreamEncryption", streamRawData);
        CompletableFuture<Void> response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<UpdateShardCountResponse> updateShardCount(UpdateShardCountRequest request) {
        StreamRawData streamRawData = new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration);
        Segment segment = Kinesis2Util.beginSegment("updateShardCount", streamRawData);
        CompletableFuture<UpdateShardCountResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
    
}
