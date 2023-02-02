package com.newrelic;

import com.newrelic.trace.v1.V1;
import io.grpc.stub.ClientCallStreamObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class SpanBatchObserverTest {

    @Mock
    private ClientCallStreamObserver<V1.SpanBatch> observer;

    private SpanBatchObserver target;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        target = spy(new SpanBatchObserver(observer));
    }

    @Test
    void cancel_invokesObserver() {
        String message = "message";
        Throwable cause = new RuntimeException("error");

        target.cancel(message, cause);

        verify(observer).cancel(message, cause);
    }

    @Test
    void isReady_invokesObserver() {
        target.isReady();
        verify(observer).isReady();
    }

    @Test
    void onNext_spanThrowsException() {
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                target.onNext(V1.Span.newBuilder().build());
            }
        });

        verify(observer, never()).onNext(ArgumentMatchers.any());
    }

    @Test
    void onNext_spanBatchInvokesObserver() {
        V1.SpanBatch spanBatch = V1.SpanBatch.newBuilder().build();
        target.onNext(spanBatch);

        verify(observer).onNext(spanBatch);
    }
}
