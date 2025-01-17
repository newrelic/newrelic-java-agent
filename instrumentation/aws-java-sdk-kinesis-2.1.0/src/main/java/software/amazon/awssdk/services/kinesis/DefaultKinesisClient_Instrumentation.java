package software.amazon.awssdk.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.kinesis.KinesisUtil;
import com.agent.instrumentation.awsjavasdk2.services.kinesis.StreamRawData;
import com.newrelic.api.agent.Trace;
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
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
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
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountRequest;
import software.amazon.awssdk.services.kinesis.model.UpdateShardCountResponse;

@Weave(originalName = "software.amazon.awssdk.services.kinesis.DefaultKinesisClient", type = MatchType.ExactClass)
class DefaultKinesisClient_Instrumentation {
    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public AddTagsToStreamResponse addTagsToStream(AddTagsToStreamRequest request) {
        KinesisUtil.setTraceDetails("addTagsToStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public CreateStreamResponse createStream(CreateStreamRequest request) {
        KinesisUtil.setTraceDetails("createStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DecreaseStreamRetentionPeriodResponse decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceDetails("decreaseStreamRetentionPeriod", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteStreamResponse deleteStream(DeleteStreamRequest request) {
        KinesisUtil.setTraceDetails("deleteStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DeregisterStreamConsumerResponse deregisterStreamConsumer(DeregisterStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        KinesisUtil.setTraceDetails("deregisterStreamConsumer", new StreamRawData(null, arn, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest request) {
        KinesisUtil.setTraceDetails("describeLimits", new StreamRawData(null, null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeStreamResponse describeStream(DescribeStreamRequest request) {
        KinesisUtil.setTraceDetails("describeStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeStreamConsumerResponse describeStreamConsumer(DescribeStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        KinesisUtil.setTraceDetails("describeStreamConsumer", new StreamRawData(null, arn, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeStreamSummaryResponse describeStreamSummary(DescribeStreamSummaryRequest request) {
        KinesisUtil.setTraceDetails("describeStreamSummary", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace
    public DisableEnhancedMonitoringResponse disableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceDetails("disableEnhancedMonitoring", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public EnableEnhancedMonitoringResponse enableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        KinesisUtil.setTraceDetails("enableEnhancedMonitoring", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public GetRecordsResponse getRecords(GetRecordsRequest request) {
        KinesisUtil.setTraceDetails("getRecords", new StreamRawData(null, null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace
    public GetShardIteratorResponse getShardIterator(GetShardIteratorRequest request) {
        KinesisUtil.setTraceDetails("getShardIterator", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public IncreaseStreamRetentionPeriodResponse increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest request) {
        KinesisUtil.setTraceDetails("increaseStreamRetentionPeriod", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListShardsResponse listShards(ListShardsRequest request) {
        KinesisUtil.setTraceDetails("listShards", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListStreamConsumersResponse listStreamConsumers(ListStreamConsumersRequest request) {
        KinesisUtil.setTraceDetails("listStreamConsumers", new StreamRawData(null, request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListStreamsResponse listStreams(ListStreamsRequest request) {
        KinesisUtil.setTraceDetails("listStreams", new StreamRawData(null, null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListTagsForStreamResponse listTagsForStream(ListTagsForStreamRequest request) {
        KinesisUtil.setTraceDetails("listTagsForStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public MergeShardsResponse mergeShards(MergeShardsRequest request) {
        KinesisUtil.setTraceDetails("mergeShards", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public PutRecordResponse putRecord(PutRecordRequest request) {
        KinesisUtil.setTraceDetails("putRecord", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public PutRecordsResponse putRecords(PutRecordsRequest request) {
        KinesisUtil.setTraceDetails("putRecords", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    @Trace
    public RegisterStreamConsumerResponse registerStreamConsumer(RegisterStreamConsumerRequest request) {
        KinesisUtil.setTraceDetails("registerStreamConsumer", new StreamRawData(null, request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }


    @Trace
    public RemoveTagsFromStreamResponse removeTagsFromStream(RemoveTagsFromStreamRequest request) {
        KinesisUtil.setTraceDetails("removeTagsFromStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public SplitShardResponse splitShard(SplitShardRequest request) {
        KinesisUtil.setTraceDetails("splitShard", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public StartStreamEncryptionResponse startStreamEncryption(StartStreamEncryptionRequest request) {
        KinesisUtil.setTraceDetails("startStreamEncryption", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace
    public StopStreamEncryptionResponse stopStreamEncryption(StopStreamEncryptionRequest request) {
        KinesisUtil.setTraceDetails("stopStreamEncryption", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace
    public UpdateShardCountResponse updateShardCount(UpdateShardCountRequest request) {
        KinesisUtil.setTraceDetails("updateShardCount", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
}
