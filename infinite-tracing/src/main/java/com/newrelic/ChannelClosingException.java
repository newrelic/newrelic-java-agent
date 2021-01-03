package com.newrelic;

/**
 * This is a signaling exception to the call that the channel is closing. By using this exception, we can pass the required
 * information to the {@link io.grpc.stub.StreamObserver#onError} call so it knows not to consider this an actual error.
 */
class ChannelClosingException extends Exception {
}
