package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.ExtendedRequest;
import com.newrelic.api.agent.HeaderType;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NRVertxExtendedRequest extends ExtendedRequest {
  private HttpServerRequest request = null;
  
  public NRVertxExtendedRequest(HttpServerRequest req) {
    this.request = req;
  }
  
  public String getRequestURI() {
    return this.request.absoluteURI();
  }
  
  public String getRemoteUser() {
    return null;
  }
  
  @SuppressWarnings("rawtypes")
public Enumeration getParameterNames() {
    MultiMap params = this.request.params();
    return Collections.enumeration(params.names());
  }
  
  public String[] getParameterValues(String name) {
    MultiMap params = this.request.params();
    List<String> values = params.getAll(name);
    String[] array = new String[values.size()];
    return values.<String>toArray(array);
  }
  
  public Object getAttribute(String name) {
    return this.request.formAttributes().get(name);
  }
  
  public String getCookieValue(String name) {
    return null;
  }
  
  public HeaderType getHeaderType() {
    return HeaderType.HTTP;
  }
  
  public String getHeader(String name) {
    return this.request.getHeader(name);
  }
  
  public String getMethod() {
    return this.request.method().name();
  }
}
