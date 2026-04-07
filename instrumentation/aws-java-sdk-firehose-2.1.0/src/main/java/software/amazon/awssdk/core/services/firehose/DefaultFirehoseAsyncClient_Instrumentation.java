package software.amazon.awssdk.core.services.firehose;

import com.agent.instrumentation.awsjavasdk2.services.firehose.DeliveryStreamRawData;
import com.agent.instrumentation.awsjavasdk2.services.firehose.FirehoseUtil;
import com.agent.instrumentation.awsjavasdk2.services.firehose.SegmentHandler;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.services.firehose.model.CreateDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.CreateDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.DeleteDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DeleteDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.DescribeDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.DescribeDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.ListDeliveryStreamsRequest;
import software.amazon.awssdk.services.firehose.model.ListDeliveryStreamsResponse;
import software.amazon.awssdk.services.firehose.model.ListTagsForDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.ListTagsForDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordResponse;
import software.amazon.awssdk.services.firehose.model.StartDeliveryStreamEncryptionRequest;
import software.amazon.awssdk.services.firehose.model.StartDeliveryStreamEncryptionResponse;
import software.amazon.awssdk.services.firehose.model.StopDeliveryStreamEncryptionRequest;
import software.amazon.awssdk.services.firehose.model.StopDeliveryStreamEncryptionResponse;
import software.amazon.awssdk.services.firehose.model.TagDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.TagDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.UntagDeliveryStreamRequest;
import software.amazon.awssdk.services.firehose.model.UntagDeliveryStreamResponse;
import software.amazon.awssdk.services.firehose.model.UpdateDestinationRequest;
import software.amazon.awssdk.services.firehose.model.UpdateDestinationResponse;

import java.util.concurrent.CompletableFuture;

@Weave(originalName = "software.amazon.awssdk.services.firehose.DefaultFirehoseAsyncClient", type = MatchType.ExactClass)
class DefaultFirehoseAsyncClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    public CompletableFuture<CreateDeliveryStreamResponse> createDeliveryStream(CreateDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("createDeliveryStream", streamRawData);
        CompletableFuture<CreateDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DeleteDeliveryStreamResponse> deleteDeliveryStream(DeleteDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("deleteDeliveryStream", streamRawData);
        CompletableFuture<DeleteDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<DescribeDeliveryStreamResponse> describeDeliveryStream(DescribeDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("describeDeliveryStream", streamRawData);
        CompletableFuture<DescribeDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListDeliveryStreamsResponse> listDeliveryStreams(ListDeliveryStreamsRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(null, this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("listDeliveryStreams", streamRawData);
        CompletableFuture<ListDeliveryStreamsResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<ListTagsForDeliveryStreamResponse> listTagsForDeliveryStream(ListTagsForDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("listTagsForDeliveryStream", streamRawData);
        CompletableFuture<ListTagsForDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("putRecord", streamRawData);
        CompletableFuture<PutRecordResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<PutRecordBatchResponse> putRecordBatch(PutRecordBatchRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("putRecordBatch", streamRawData);
        CompletableFuture<PutRecordBatchResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StartDeliveryStreamEncryptionResponse> startDeliveryStreamEncryption(StartDeliveryStreamEncryptionRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("startDeliveryStreamEncryption", streamRawData);
        CompletableFuture<StartDeliveryStreamEncryptionResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<StopDeliveryStreamEncryptionResponse> stopDeliveryStreamEncryption(StopDeliveryStreamEncryptionRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("stopDeliveryStreamEncryption", streamRawData);
        CompletableFuture<StopDeliveryStreamEncryptionResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<TagDeliveryStreamResponse> tagDeliveryStream(TagDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("tagDeliveryStream", streamRawData);
        CompletableFuture<TagDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<UntagDeliveryStreamResponse> untagDeliveryStream(UntagDeliveryStreamRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("untagDeliveryStream", streamRawData);
        CompletableFuture<UntagDeliveryStreamResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }

    public CompletableFuture<UpdateDestinationResponse> updateDestination(UpdateDestinationRequest request) {
        DeliveryStreamRawData streamRawData = new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration);
        Segment segment = FirehoseUtil.beginSegment("updateDestination", streamRawData);
        CompletableFuture<UpdateDestinationResponse>  response = Weaver.callOriginal();
        return new SegmentHandler<>(streamRawData, response, segment, Weaver.getImplementationTitle()).newSegmentCompletionStage();
    }
}
