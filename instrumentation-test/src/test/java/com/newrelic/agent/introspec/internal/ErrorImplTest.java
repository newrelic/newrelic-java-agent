package com.newrelic.agent.introspec.internal;

import com.newrelic.agent.errors.HttpTracedError;
import com.newrelic.agent.errors.ThrowableError;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ErrorImplTest {

    private final String ERROR_MESSAGE = "Error Message";

    @Test
    public void getThrowable() {
        ThrowableError errorMock = getErrorMock(new Throwable("Message"));

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getThrowable(), instanceOf(Throwable.class));
        assertThat(error.getThrowable().getMessage(), equalTo("Message"));
    }

    @Test
    public void getThrowable_forTracedError() {
        HttpTracedError errorMock = getErrorMock(200);

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getThrowable(), is(nullValue()));
    }

    @Test
    public void getResponseStatus() {
        HttpTracedError errorMock = getErrorMock(200);

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getResponseStatus(), is(200));
    }

    @Test
    public void getResponseStatus_forThrowableError() {
        ThrowableError errorMock = getErrorMock(new Throwable("Message"));

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getResponseStatus(), is(0));
    }

    @Test
    public void getErrorMessage_forThrowableError() {
        ThrowableError errorMock = getErrorMock(new Throwable("different message"));

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getErrorMessage(), equalTo(ERROR_MESSAGE));
    }

    @Test
    public void getErrorMessage_forTracedError() {
        HttpTracedError errorMock = getErrorMock(200);

        ErrorImpl error = new ErrorImpl(errorMock);

        assertThat(error.getErrorMessage(), equalTo(ERROR_MESSAGE));
    }


    private ThrowableError getErrorMock(Throwable throwable) {
        ThrowableError error = mock(ThrowableError.class);
        when(error.getThrowable()).thenReturn(throwable);
        when(error.getMessage()).thenReturn(ERROR_MESSAGE);
        return error;
    }

    private HttpTracedError getErrorMock(int statusCode) {
        HttpTracedError error = mock(HttpTracedError.class);
        when(error.getStatusCode()).thenReturn(statusCode);
        when(error.getMessage()).thenReturn(ERROR_MESSAGE);
        return error;
    }

}