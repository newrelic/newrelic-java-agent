package com.newrelic.instrumentation.micronaut.http.client4;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;

import io.micronaut.http.uri.UriBuilder;
import org.reactivestreams.Publisher;

import com.newrelic.api.agent.NewRelic;

import io.micronaut.http.HttpRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Utils {

	
	public static <T> boolean isFlux(Publisher<T> pub) {
		return (pub instanceof Flux);
	}
	
	public static <T> boolean isMono(Publisher<T> pub) {
		return (pub instanceof Mono);
	}
	
	public static URI getRequestURI(HttpRequest<?> request) {
		
		InetSocketAddress serverAddress = request.getServerAddress();
		URI reqURI = request.getUri();

		String schema = reqURI != null ? reqURI.getScheme() : null;
		if(schema == null) schema = "http";
		String host = serverAddress.getHostName();
		int port = serverAddress.getPort();
		String path = request.getPath();

		URI uri = null;
		
		try {
			uri = new URI(schema, null, host, port, path, null, null );
		} catch (URISyntaxException e) {
			NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Error getting URI");
		}
		
		if(uri == null) uri = URI.create(schema+"://UnknownHost");
		
		
		return uri;
	}
}
