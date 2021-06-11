package com.nr.instrumentation.vertx;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

public class NRRunnableWrapper implements Runnable {
  private Runnable delegate;
  
  private Token token = null;
  
  public Runnable getDelegate() {
    return this.delegate;
  }
  
  public Token getToken() {
    return this.token;
  }
  
  public void setToken(Token token) {
    this.token = token;
  }
  
  public NRRunnableWrapper(Runnable d, Token t) {
    this.delegate = d;
    this.token = t;
  }
  
  @Trace(async = true)
  public void run() {
    if (this.token != null) {
      this.token.linkAndExpire();
      this.token = null;
    } 
    this.delegate.run();
  }
  
  public Token getAndRemoveToken() {
    Token t = this.token;
    this.token = null;
    return t;
  }
}
