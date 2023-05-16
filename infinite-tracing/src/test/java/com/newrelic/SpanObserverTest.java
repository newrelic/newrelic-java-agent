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

class SpanObserverTest {

    @Mock
    private ClientCallStreamObserver<V1.Span> observer;

    private SpanObserver target;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        target = spy(new SpanObserver(observer));
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
    void onNext_spanInvokesObserver() {
        V1.Span span = V1.Span.newBuilder().build();
        target.onNext(span);

        verify(observer).onNext(span);
    }

    @Test
    void onNext_spanBatchThrowsException() {
        assertThrows(UnsupportedOperationException.class, new Executable() {
            @Override
            public void execute() {
                target.onNext(V1.SpanBatch.newBuilder().build());
            }
        });

        verify(observer, never()).onNext(ArgumentMatchers.any());
    }
}
