package redis.clients.jedis;

import com.newrelic.agent.bridge.datastore.DatastoreVendor;
import com.newrelic.api.agent.DatastoreParameters;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@SuppressWarnings({ "ResultOfMethodCallIgnored", "unused" })
@Weave(type = MatchType.ExactClass, originalName = "redis.clients.jedis.JedisPubSubBase")
public class JedisPubSubBase_Instrumentation {

  private volatile Connection client;

  public final void proceed(Connection client, Object... channels) {
    Weaver.callOriginal();
  }

  public void proceedWithPatterns(Connection client, Object... channels) {
    Weaver.callOriginal();
  }

  @Trace
  public void onMessage(Object channel, Object message) {
    Weaver.callOriginal();

    reportMethodAsExternal("message");
  }

  @Trace
  public void onPMessage(Object pattern, Object channel, Object message) {
    Weaver.callOriginal();

    reportMethodAsExternal("message");
  }

  @Trace
  public void onSubscribe(Object channel, int subscribedChannels) {
    Weaver.callOriginal();

    reportMethodAsExternal("subscribe");
  }

  @Trace
  public void onUnsubscribe(Object channel, int subscribedChannels) {
    Weaver.callOriginal();

    reportMethodAsExternal("unsubscribe");
  }

  @Trace
  public void onPUnsubscribe(Object pattern, int subscribedChannels) {
    Weaver.callOriginal();

    reportMethodAsExternal("unsubscribe");
  }

  @Trace
  public void onPSubscribe(Object pattern, int subscribedChannels) {
    Weaver.callOriginal();

    reportMethodAsExternal("subscribe");
  }

  private void reportMethodAsExternal(String commandName) {
    NewRelic.getAgent().getTracedMethod().reportAsExternal(DatastoreParameters
        .product(DatastoreVendor.Redis.name())
        .collection(null)
        .operation(commandName)
        .instance(client.getHostAndPort().getHost(), client.getHostAndPort().getPort())
        .build());

  }
}
