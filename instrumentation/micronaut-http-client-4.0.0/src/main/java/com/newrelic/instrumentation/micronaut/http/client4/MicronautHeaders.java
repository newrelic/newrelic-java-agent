package com.newrelic.instrumentation.micronaut.http.client4;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.Headers;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpRequest;

public class MicronautHeaders implements Headers {
	
	private HttpRequest<?> request = null;
	private HttpResponse<?> response = null;
	
	public MicronautHeaders(HttpRequest<?> req) {
		this(req,null);
	}
	
	public MicronautHeaders(HttpResponse<?> resp) {
		this(null,resp);
	}
	
	public MicronautHeaders(HttpRequest<?> req,HttpResponse<?> resp) {
		request = req;
		response = resp;
	}
	

	@Override
	public HeaderType getHeaderType() {
		return HeaderType.HTTP;
	}

	@Override
	public String getHeader(String name) {
		if(response != null) {
			return response.header(name);
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
		if(request != null) {
			if(request instanceof MutableHttpRequest) {
				MutableHttpRequest<?> mutable = (MutableHttpRequest<?>)request;
				mutable.header(name, value);
			}
		}
	}

	@Override
	public void addHeader(String name, String value) {
		setHeader(name, value);
	}

	@Override
	public Collection<String> getHeaderNames() {
		List<String> headerNames = new ArrayList<String>();
		if(request != null) {
			HttpHeaders headers = request.getHeaders();
			if(headers != null) {
				headerNames.addAll(headers.names());
			}
		}
		if(response != null) {
			HttpHeaders headers = response.getHeaders();
			if(headers != null) {
				headerNames.addAll(headers.names());
			}
		}
		return headerNames;
	}

	@Override
	public boolean containsHeader(String name) {
		Collection<String> names = getHeaderNames();
		return names.contains(name);
	}

}
