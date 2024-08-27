package software.amazon.awssdk.services.kinesis;

import com.agent.instrumentation.awsjavasdk2.services.lambda.KinesisUtil;
import com.newrelic.api.agent.Trace;
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
    @Trace(leaf=true)
    public AddTagsToStreamResponse addTagsToStream(AddTagsToStreamRequest addTagsToStreamRequest) {
        KinesisUtil.setTraceDetails("addTagsToStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public CreateStreamResponse createStream(CreateStreamRequest createStreamRequest) {
        KinesisUtil.setTraceDetails("createStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DecreaseStreamRetentionPeriodResponse decreaseStreamRetentionPeriod(DecreaseStreamRetentionPeriodRequest decreaseStreamRetentionPeriodRequest) {
        KinesisUtil.setTraceDetails("decreaseStreamRetentionPeriod");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DeleteStreamResponse deleteStream(DeleteStreamRequest deleteStreamRequest) {
        KinesisUtil.setTraceDetails("deleteStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DeregisterStreamConsumerResponse deregisterStreamConsumer(DeregisterStreamConsumerRequest deregisterStreamConsumerRequest) {
        KinesisUtil.setTraceDetails("deregisterStreamConsumer");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        KinesisUtil.setTraceDetails("describeLimits");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamResponse describeStream(DescribeStreamRequest describeStreamRequest) {
        KinesisUtil.setTraceDetails("describeStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamConsumerResponse describeStreamConsumer(DescribeStreamConsumerRequest describeStreamConsumerRequest) {
        KinesisUtil.setTraceDetails("describeStreamConsumer");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamSummaryResponse describeStreamSummary(DescribeStreamSummaryRequest describeStreamSummaryRequest) {
        KinesisUtil.setTraceDetails("describeStreamSummary");
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public DisableEnhancedMonitoringResponse disableEnhancedMonitoring(DisableEnhancedMonitoringRequest disableEnhancedMonitoringRequest) {
        KinesisUtil.setTraceDetails("disableEnhancedMonitoring");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public EnableEnhancedMonitoringResponse enableEnhancedMonitoring(EnableEnhancedMonitoringRequest enableEnhancedMonitoringRequest) {
        KinesisUtil.setTraceDetails("enableEnhancedMonitoring");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public GetRecordsResponse getRecords(GetRecordsRequest getRecordsRequest) {
        KinesisUtil.setTraceDetails("getRecords");
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public GetShardIteratorResponse getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {
        KinesisUtil.setTraceDetails("getShardIterator");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public IncreaseStreamRetentionPeriodResponse increaseStreamRetentionPeriod(IncreaseStreamRetentionPeriodRequest increaseStreamRetentionPeriodRequest) {
        KinesisUtil.setTraceDetails("increaseStreamRetentionPeriod");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListShardsResponse listShards(ListShardsRequest listShardsRequest) {
        KinesisUtil.setTraceDetails("listShards");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListStreamConsumersResponse listStreamConsumers(ListStreamConsumersRequest listStreamConsumersRequest) {
        KinesisUtil.setTraceDetails("listStreamConsumers");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListStreamsResponse listStreams(ListStreamsRequest listStreamsRequest) {
        KinesisUtil.setTraceDetails("listStreams");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListTagsForStreamResponse listTagsForStream(ListTagsForStreamRequest listTagsForStreamRequest) {
        KinesisUtil.setTraceDetails("listTagsForStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public MergeShardsResponse mergeShards(MergeShardsRequest mergeShardsRequest) {
        KinesisUtil.setTraceDetails("mergeShards");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public PutRecordResponse putRecord(PutRecordRequest putRecordRequest) {
        KinesisUtil.setTraceDetails("putRecord");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public PutRecordsResponse putRecords(PutRecordsRequest putRecordsRequest) {
        KinesisUtil.setTraceDetails("putRecords");
        return Weaver.callOriginal();
    }
    @Trace(leaf=true)
    public RegisterStreamConsumerResponse registerStreamConsumer(RegisterStreamConsumerRequest registerStreamConsumerRequest) {
        KinesisUtil.setTraceDetails("registerStreamConsumer");
        return Weaver.callOriginal();
    }


    @Trace(leaf=true)
    public RemoveTagsFromStreamResponse removeTagsFromStream(RemoveTagsFromStreamRequest removeTagsFromStreamRequest) {
        KinesisUtil.setTraceDetails("removeTagsFromStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public SplitShardResponse splitShard(SplitShardRequest splitShardRequest) {
        KinesisUtil.setTraceDetails("splitShard");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public StartStreamEncryptionResponse startStreamEncryption(StartStreamEncryptionRequest startStreamEncryptionRequest) {
        KinesisUtil.setTraceDetails("startStreamEncryption");
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public StopStreamEncryptionResponse stopStreamEncryption(StopStreamEncryptionRequest stopStreamEncryptionRequest) {
        KinesisUtil.setTraceDetails("stopStreamEncryption");
        return Weaver.callOriginal();
    }
    
    @Trace(leaf=true)
    public UpdateShardCountResponse updateShardCount(UpdateShardCountRequest updateShardCountRequest) {
        KinesisUtil.setTraceDetails("updateShardCount");
        return Weaver.callOriginal();
    }
    
}
