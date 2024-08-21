package software.amazon.awssdk.services.kinesis;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeLimitsResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamConsumerResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamResponse;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryRequest;
import software.amazon.awssdk.services.kinesis.model.DescribeStreamSummaryResponse;
import software.amazon.awssdk.services.kinesis.model.GetRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.GetRecordsResponse;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorRequest;
import software.amazon.awssdk.services.kinesis.model.GetShardIteratorResponse;
import software.amazon.awssdk.services.kinesis.model.ListShardsRequest;
import software.amazon.awssdk.services.kinesis.model.ListShardsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordsRequest;
import software.amazon.awssdk.services.kinesis.model.PutRecordsResponse;

@Weave(originalName = "software.amazon.awssdk.services.kinesis.DefaultKinesisClient", type = MatchType.ExactClass)
class DefaultKinesisClient_Instrumentation {
    // Todo: report external call with the CloudParameters API

    @Trace(leaf=true)
    public DescribeLimitsResponse describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "describeLimits");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamResponse describeStream(DescribeStreamRequest describeStreamRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "describeStream");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamConsumerResponse describeStreamConsumer(DescribeStreamConsumerRequest describeStreamConsumerRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "describeStreamConsumer");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public DescribeStreamSummaryResponse describeStreamSummary(DescribeStreamSummaryRequest describeStreamSummaryRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "describeStreamSummary");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public GetRecordsResponse getRecords(GetRecordsRequest getRecordsRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "getRecords");
        return Weaver.callOriginal();
    }


    @Trace(leaf=true)
    public GetShardIteratorResponse getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "getShardIterator");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public ListShardsResponse listShards(ListShardsRequest listShardsRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "listShards");
        return Weaver.callOriginal();
    }


    @Trace(leaf=true)
    public PutRecordResponse putRecord(PutRecordRequest putRecordRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "putRecord");
        return Weaver.callOriginal();
    }

    @Trace(leaf=true)
    public PutRecordsResponse putRecords(PutRecordsRequest putRecordsRequest) {
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        tracedMethod.setMetricName("Kinesis", "putRecords");
        return Weaver.callOriginal();
    }
}
