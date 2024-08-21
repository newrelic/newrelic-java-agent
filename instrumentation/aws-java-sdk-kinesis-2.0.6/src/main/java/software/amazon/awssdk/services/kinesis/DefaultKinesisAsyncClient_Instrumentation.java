package software.amazon.awssdk.services.kinesis;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Trace;
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

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(originalName = "software.amazon.awssdk.services.kinesis.DefaultKinesisAsyncClient", type = MatchType.ExactClass)
class DefaultKinesisAsyncClient_Instrumentation {
    // Todo: report external call with the CloudParameters API

    @Trace
    public CompletableFuture<DescribeLimitsResponse> describeLimits(DescribeLimitsRequest describeLimitsRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis","describeLimits");
        CompletableFuture<DescribeLimitsResponse>  response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<DescribeStreamResponse> describeStream(DescribeStreamRequest describeStreamRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "describeStream");
        CompletableFuture<DescribeStreamResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamConsumer(DescribeStreamConsumerRequest describeStreamConsumerRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "describeStreamConsumer");
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<DescribeStreamConsumerResponse> describeStreamSummary(DescribeStreamSummaryRequest describeStreamSummaryRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("KinesisAsyncClient", "describeStreamSummary");
        CompletableFuture<DescribeStreamConsumerResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<GetRecordsResponse> getRecords(GetRecordsRequest getRecordsRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "getRecords");
        CompletableFuture<GetRecordsResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }


    @Trace
    public CompletableFuture<GetShardIteratorResponse> getShardIterator(GetShardIteratorRequest getShardIteratorRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "getShardIterator");
        CompletableFuture<GetShardIteratorResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<ListShardsResponse> listShards(ListShardsRequest listShardsRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "listShards");
        CompletableFuture<ListShardsResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }


    @Trace
    public CompletableFuture<PutRecordResponse> putRecord(PutRecordRequest putRecordRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "putRecord");
        CompletableFuture<PutRecordResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    @Trace
    public CompletableFuture<PutRecordsResponse> putRecords(PutRecordsRequest putRecordsRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("Kinesis", "putRecords");
        CompletableFuture<PutRecordsResponse> response = Weaver.callOriginal();
        if (response == null) {
            return null;
        }
        return response.whenComplete(new NrSegmentCompletion<>(segment, Weaver.getImplementationTitle()));
    }

    private static class NrSegmentCompletion<R, T> implements BiConsumer<R, T> {

        private final Segment segment;
        private final String implementationTitle;

        NrSegmentCompletion(Segment segment, String implementationTitle) {
            this.segment = segment;
            this.implementationTitle = implementationTitle;
        }

        @Override
        public void accept(R r, T t) {
            try {
                segment.end();
            } catch (Throwable t1) {
                AgentBridge.instrumentation.noticeInstrumentationError(t1, implementationTitle);
            }
        }
    }
}
