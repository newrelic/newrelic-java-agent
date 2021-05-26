package com.nr.fit.lettuce.instrumentation;

import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;

public class NRHolder
{
  private String segmentName = null;
  private Segment segment = null;
  private DatastoreParameters params = null;
  private Token token = null;
  private boolean hasEnded = false;
  
  public NRHolder(Segment s, DatastoreParameters p)
  {
    segment = s;
    params = p;
    token = NewRelic.getAgent().getTransaction().getToken();
    hasEnded = false;
  }
  
  public NRHolder(String s, DatastoreParameters p)
  {
    segmentName = s;
    params = p;
    token = NewRelic.getAgent().getTransaction().getToken();
    hasEnded = false;
  }
  
  public void startSegment()
  {
    segment = NewRelic.getAgent().getTransaction().startSegment(segmentName);
  }
  
  public boolean hasEnded()
  {
    return hasEnded;
  }
  
  public void end()
  {
    if (token != null)
    {
      token.linkAndExpire();
      token = null;
    }
    if (segment != null)
    {
      String operation = params.getOperation();
      if(operation.equalsIgnoreCase("expire")) {
    	  segment.ignore();
      } else {
	      segment.reportAsExternal(params);
	      segment.end();
      }
	  params = null;
      segment = null;
    }
    else
    {
        String operation = params.getOperation();
        if(!operation.equalsIgnoreCase("expire")) {
        	NewRelic.getAgent().getTracedMethod().reportAsExternal(params);
        }
    }
  }
}
