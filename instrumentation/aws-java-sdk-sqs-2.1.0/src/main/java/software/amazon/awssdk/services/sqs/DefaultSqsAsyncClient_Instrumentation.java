/*
 *
 *  * Copyright 2020 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package software.amazon.awssdk.services.sqs;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.utils.SqsV2Util;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@Weave(type = MatchType.ExactClass, originalName = "software.amazon.awssdk.services.sqs.DefaultSqsAsyncClient")
class DefaultSqsAsyncClient_Instrumentation {

    public CompletableFuture<SendMessageResponse> sendMessage(SendMessageRequest sendMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "sendMessage");

        SQSRequestHeaders headers = new SQSRequestHeaders(sendMessageRequest);
        NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
        sendMessageRequest = headers.getUpdatedRequest();

        segment.reportAsExternal(SqsV2Util.generateExternalProduceMetrics(sendMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageResponse, Throwable>() {
            @Override
            public void accept(SendMessageResponse sendMessageResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<SendMessageBatchResponse> sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "sendMessageBatch");

        List<SendMessageBatchRequestEntry> updatedEntries = new ArrayList<>();
        for (SendMessageBatchRequestEntry entry : sendMessageBatchRequest.entries()) {
            SQSBatchRequestHeaders headers = new SQSBatchRequestHeaders(entry);
            NewRelic.getAgent().getTransaction().insertDistributedTraceHeaders(headers);
            updatedEntries.add(headers.updatedEntry());
        }
        sendMessageBatchRequest = sendMessageBatchRequest.toBuilder().entries(updatedEntries).build();

        segment.reportAsExternal(SqsV2Util.generateExternalProduceMetrics(sendMessageBatchRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<SendMessageBatchResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<SendMessageBatchResponse, Throwable>() {
            @Override
            public void accept(SendMessageBatchResponse sendMessageBatchResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }

    public CompletableFuture<ReceiveMessageResponse> receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment(SqsV2Util.LIBRARY, "receiveMessage");

        ArrayList<String> updatedMessageAttrNames = new ArrayList<>(receiveMessageRequest.messageAttributeNames());
        Collections.addAll(updatedMessageAttrNames, SqsV2Util.DT_HEADERS);
        receiveMessageRequest = receiveMessageRequest.toBuilder().messageAttributeNames(updatedMessageAttrNames).build();

        segment.reportAsExternal(SqsV2Util.generateExternalConsumeMetrics(receiveMessageRequest.queueUrl()));
        AgentBridge.getAgent().getTracedMethod().setTrackChildThreads(false);

        CompletableFuture<ReceiveMessageResponse> result = Weaver.callOriginal();
        if (result == null) {
            return null;
        }
        return result.whenComplete(new BiConsumer<ReceiveMessageResponse, Throwable>() {
            @Override
            public void accept(ReceiveMessageResponse receiveMessageResponse, Throwable throwable) {
                SqsV2Util.finishSegment(segment);
            }
        });
    }

    /*
    * Headers are used as inner classes to avoid class loading errors when running instrumentation tests
    * */

    public class SQSRequestHeaders implements Headers {

        private SendMessageRequest request = null;
        private Map<String, String> additionalAttributes = new Hashtable<String, String>();

        public SQSRequestHeaders(SendMessageRequest req) {
            request = req;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.MESSAGE;
        }

        @Override
        public String getHeader(String name) {
            Map<String, MessageAttributeValue> messageAttributes = request.messageAttributes();
            if(messageAttributes != null) {
                MessageAttributeValue value = messageAttributes.get(name);
                if (value != null && value.dataType().equalsIgnoreCase("string")) {
                    String valueString = value.stringValue();
                    if (valueString != null) {
                        return valueString;
                    }
                }
            }

            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            List<String> list = new ArrayList<String>();
            String value = getHeader(name);
            if(value != null) {
                list.add(value);
            }
            return list;
        }

        @Override
        public void setHeader(String name, String value) {
            if(request != null) {
                Map<String, MessageAttributeValue> existingAttributes = request.messageAttributes();
                if(!existingAttributes.containsKey(name)) {
                    additionalAttributes.put(name, value);
                }
            }
        }

        @Override
        public void addHeader(String name, String value) {
            setHeader(name, value);
        }

        @Override
        public Collection<String> getHeaderNames() {
            Map<String, MessageAttributeValue> messageAttributes = request.messageAttributes();
            return messageAttributes.keySet();
        }

        @Override
        public boolean containsHeader(String name) {
            return getHeaderNames().contains(name);
        }

        public SendMessageRequest getUpdatedRequest() {
            SendMessageRequest.Builder builder = request.toBuilder();
            Map<String, MessageAttributeValue> msgAttributes = request.messageAttributes();

            Map<String, MessageAttributeValue>  updatedAttributes = new HashMap<String, MessageAttributeValue>();

            if(msgAttributes != null && !msgAttributes.isEmpty()) {
                updatedAttributes.putAll(msgAttributes);
            }

            for(String key : additionalAttributes.keySet()) {
                MessageAttributeValue newValue = MessageAttributeValue.builder().dataType("String").stringValue(additionalAttributes.get(key)).build();
                updatedAttributes.put(key, newValue);
            }
            builder.messageAttributes(updatedAttributes);

            SendMessageRequest updated = builder.build();
            return updated;
        }
    }

    public class SQSBatchRequestHeaders implements Headers {

        private SendMessageBatchRequestEntry requestEntry = null;
        private Map<String, String> additionalAttributes = new Hashtable<String, String>();

        public SQSBatchRequestHeaders(SendMessageBatchRequestEntry re) {
            requestEntry = re;
        }

        @Override
        public HeaderType getHeaderType() {
            return HeaderType.MESSAGE;
        }

        @Override
        public String getHeader(String name) {
            Map<String, MessageAttributeValue> attributes = requestEntry.messageAttributes();
            if(attributes != null) {
                MessageAttributeValue value = attributes.get(name);
                if(value != null) {
                    String dataType = value.dataType();
                    if(dataType.equalsIgnoreCase("String")) {
                        return value.stringValue();
                    }
                }
            }
            return null;
        }

        @Override
        public Collection<String> getHeaders(String name) {
            List<String> list = new ArrayList<String>();
            String value = getHeader(name);
            if(value != null && !value.isEmpty()) {
                list.add(value);
            }
            return list;
        }

        @Override
        public void setHeader(String name, String value) {
            if(requestEntry != null) {
                if(!requestEntry.messageAttributes().containsKey(name)) {
                    additionalAttributes.put(name, value);
                }
            }
        }

        @Override
        public void addHeader(String name, String value) {
            if(requestEntry != null) {
                if(!requestEntry.messageAttributes().containsKey(name)) {
                    additionalAttributes.put(name, value);
                }
            }
        }

        @Override
        public Collection<String> getHeaderNames() {
            if(requestEntry != null) {
                Map<String, MessageAttributeValue> attributes = requestEntry.messageAttributes();
                if(attributes != null) {
                    return attributes.keySet();
                }
            }
            return Collections.emptyList();
        }

        @Override
        public boolean containsHeader(String name) {
            return getHeaderNames().contains(name);
        }

        public SendMessageBatchRequestEntry updatedEntry() {
            SendMessageBatchRequestEntry.Builder builder = requestEntry.toBuilder();
            Map<String, MessageAttributeValue> msgAttributes = requestEntry.messageAttributes();

            Map<String, MessageAttributeValue>  updatedAttributes = new HashMap<String, MessageAttributeValue>();

            if(msgAttributes != null && !msgAttributes.isEmpty()) {
                updatedAttributes.putAll(msgAttributes);
            }

            for(String key : additionalAttributes.keySet()) {
                MessageAttributeValue newValue = MessageAttributeValue.builder().dataType("String").stringValue(additionalAttributes.get(key)).build();
                updatedAttributes.put(key, newValue);
            }
            builder.messageAttributes(updatedAttributes);

            SendMessageBatchRequestEntry updated = builder.build();

            return updated;
        }
    }
}
