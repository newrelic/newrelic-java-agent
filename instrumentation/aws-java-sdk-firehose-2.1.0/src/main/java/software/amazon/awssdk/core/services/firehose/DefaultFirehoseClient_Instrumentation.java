package software.amazon.awssdk.core.services.firehose;

import com.agent.instrumentation.awsjavasdk2.services.firehose.DeliveryStreamRawData;
import com.agent.instrumentation.awsjavasdk2.services.firehose.FirehoseUtil;
import com.newrelic.api.agent.Trace;
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

@Weave(originalName = "software.amazon.awssdk.services.firehose.DefaultFirehoseClient", type = MatchType.ExactClass)
class DefaultFirehoseClient_Instrumentation {

    private final SdkClientConfiguration clientConfiguration = Weaver.callOriginal();

    @Trace
    public CreateDeliveryStreamResponse createDeliveryStream(CreateDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("createDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DeleteDeliveryStreamResponse deleteDeliveryStream(DeleteDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("deleteDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public DescribeDeliveryStreamResponse describeDeliveryStream(DescribeDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("describeDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListDeliveryStreamsResponse listDeliveryStreams(ListDeliveryStreamsRequest request) {
        FirehoseUtil.setTraceDetails("listDeliveryStreams",
                new DeliveryStreamRawData(null, this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public ListTagsForDeliveryStreamResponse listTagsForDeliveryStream(ListTagsForDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("listTagsForDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public PutRecordResponse putRecord(PutRecordRequest request) {
        FirehoseUtil.setTraceDetails("putRecord",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public PutRecordBatchResponse putRecordBatch(PutRecordBatchRequest request) {
        FirehoseUtil.setTraceDetails("putRecordBatch",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public StartDeliveryStreamEncryptionResponse startDeliveryStreamEncryption(StartDeliveryStreamEncryptionRequest request) {
        FirehoseUtil.setTraceDetails("startDeliveryStreamEncryption",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public StopDeliveryStreamEncryptionResponse stopDeliveryStreamEncryption(StopDeliveryStreamEncryptionRequest request) {
        FirehoseUtil.setTraceDetails("stopDeliveryStreamEncryption",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public TagDeliveryStreamResponse tagDeliveryStream(TagDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("tagDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public UntagDeliveryStreamResponse untagDeliveryStream(UntagDeliveryStreamRequest request) {
        FirehoseUtil.setTraceDetails("untagDeliveryStream",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

    @Trace
    public UpdateDestinationResponse updateDestination(UpdateDestinationRequest request) {
        FirehoseUtil.setTraceDetails("updateDestination",
                new DeliveryStreamRawData(request.deliveryStreamName(), this, clientConfiguration));
        return Weaver.callOriginal();
    }

}