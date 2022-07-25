package com.newrelic.api.agent;

import java.util.Collection;
import java.util.List;

/**
 * The type-specific headers of an inbound or outbound message.
 */
public interface Headers extends InboundHeaders, OutboundHeaders {

    /**
     * Return the type of header key syntax used for this.
     *
     * @return An {@code enum} specifying the type of headers present.
     */
    @Override
    HeaderType getHeaderType();

    /**
     * Returns the value of the first specified message header as a {@link String}. If the message does not include a header
     * with the specified input name, then this method returns {@code null}.
     *
     * @param name The name of the desired message header.
     * @return A {@link String} containing the value of the requested header, or {@code null} if the message header is not
     * present.
     */
    @Override
    String getHeader(String name);

    /**
     * Return all the values of the specified message header as a {@link List} of {@link String} objects. If the message
     * does not include any headers of the specified name, this method returns an empty list.
     *
     * <p>Changes to the returned collection must not affect the Headers instance.
     *
     * @param name The name of the desired message header.
     * @return A {@link List} containing the values of the requested header, or an empty list of the message header is not
     * present.
     */
    Collection<String> getHeaders(String name);

    /**
     * Set a header with the given name and value. If the header had already been set, the new value overwrites the previous
     * one. {@link #containsHeader(String)} can be used to test for the presence of a header before setting its value.
     *
     * @param name The name of the header.
     * @param value The value of the header.
     */
    @Override
    void setHeader(String name, String value);

    /**
     * Add a header with the given name and value. This method allows headers to have multiple values.
     *
     * @param name The name of the header.
     * @param value The value of the header.
     */
    void addHeader(String name, String value);

    /**
     * Get the names of the headers.
     *
     * <p>Changes to the returned collection must not affect the Headers instance.
     *
     * @return A possibly empty {@link Collection} of the names of present headers.
     */
    Collection<String> getHeaderNames();

    /**
     * Return a boolean indicating whether the named header has already been set.
     *
     * @param name The name of the header.
     * @return {@code true} if the header has already been set, {@code false} otherwise.
     */
    boolean containsHeader(String name);

}