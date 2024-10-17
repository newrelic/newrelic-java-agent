package software.amazon.awssdk.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.kinesis.Kinesis2Util;
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

    @Trace(leaf=true)
    public AddTagsToStreamResponse addTagsToStream(AddTagsToStreamRequest request) {
        Kinesis2Util.setTraceDetails("addTagsToStream", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public CreateStreamResponse createStream(CreateStreamRequest request) {
        Kinesis2Util.setTraceDetails("createStream", new StreamRawData(request.streamName(), null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DecreaseStreamRetentionPeriodResponse decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest request) {
        Kinesis2Util.setTraceDetails("decreaseStreamRetentionPeriod", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DeleteStreamResponse deleteStream(DeleteStreamRequest request) {
        Kinesis2Util.setTraceDetails("deleteStream", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DeregisterStreamConsumerResponse deregisterStreamConsumer(DeregisterStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        Kinesis2Util.setTraceDetails("deregisterStreamConsumer", new StreamRawData(null, arn, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest request) {
        Kinesis2Util.setTraceDetails("describeLimits", new StreamRawData(null, null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamResponse describeStream(DescribeStreamRequest request) {
        Kinesis2Util.setTraceDetails("describeStream", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamConsumerResponse describeStreamConsumer(DescribeStreamConsumerRequest request) {
        String streamArn = request.streamARN();
        String consumerArn = request.consumerARN();
        String arn = streamArn != null && !streamArn.isEmpty() ? streamArn : consumerArn;
        Kinesis2Util.setTraceDetails("describeStreamConsumer", new StreamRawData(null, arn, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamSummaryResponse describeStreamSummary(DescribeStreamSummaryRequest request) {
        Kinesis2Util.setTraceDetails("describeStreamSummary", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public DisableEnhancedMonitoringResponse disableEnhancedMonitoring(DisableEnhancedMonitoringRequest request) {
        Kinesis2Util.setTraceDetails("disableEnhancedMonitoring", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public EnableEnhancedMonitoringResponse enableEnhancedMonitoring(EnableEnhancedMonitoringRequest request) {
        Kinesis2Util.setTraceDetails("enableEnhancedMonitoring", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public GetRecordsResponse getRecords(GetRecordsRequest request) {
        Kinesis2Util.setTraceDetails("getRecords", new StreamRawData(null, request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public GetShardIteratorResponse getShardIterator(GetShardIteratorRequest request) {
        Kinesis2Util.setTraceDetails("getShardIterator", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public IncreaseStreamRetentionPeriodResponse increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest request) {
        Kinesis2Util.setTraceDetails("increaseStreamRetentionPeriod", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListShardsResponse listShards(ListShardsRequest request) {
        Kinesis2Util.setTraceDetails("listShards", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListStreamConsumersResponse listStreamConsumers(ListStreamConsumersRequest request) {
        Kinesis2Util.setTraceDetails("listStreamConsumers", new StreamRawData(null, request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListStreamsResponse listStreams(ListStreamsRequest request) {
        Kinesis2Util.setTraceDetails("listStreams", new StreamRawData(null, null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListTagsForStreamResponse listTagsForStream(ListTagsForStreamRequest request) {
        Kinesis2Util.setTraceDetails("listTagsForStream", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public MergeShardsResponse mergeShards(MergeShardsRequest request) {
        Kinesis2Util.setTraceDetails("mergeShards", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public PutRecordResponse putRecord(PutRecordRequest request) {
        Kinesis2Util.setTraceDetails("putRecord", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public PutRecordsResponse putRecords(PutRecordsRequest request) {
        Kinesis2Util.setTraceDetails("putRecords", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    @Trace(leaf=true)
    public RegisterStreamConsumerResponse registerStreamConsumer(RegisterStreamConsumerRequest request) {
        Kinesis2Util.setTraceDetails("registerStreamConsumer", new StreamRawData(null, request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }


    @Trace(leaf=true)
    public RemoveTagsFromStreamResponse removeTagsFromStream(RemoveTagsFromStreamRequest request) {
        Kinesis2Util.setTraceDetails("removeTagsFromStream", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public SplitShardResponse splitShard(SplitShardRequest request) {
        Kinesis2Util.setTraceDetails("splitShard", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public StartStreamEncryptionResponse startStreamEncryption(StartStreamEncryptionRequest request) {
        Kinesis2Util.setTraceDetails("startStreamEncryption", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public StopStreamEncryptionResponse stopStreamEncryption(StopStreamEncryptionRequest request) {
        Kinesis2Util.setTraceDetails("stopStreamEncryption", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public UpdateShardCountResponse updateShardCount(UpdateShardCountRequest request) {
        Kinesis2Util.setTraceDetails("updateShardCount", new StreamRawData(request.streamName(), request.streamARN(), this, clientConfiguration));
        return Weaver.callOriginal();
    }
    
}
