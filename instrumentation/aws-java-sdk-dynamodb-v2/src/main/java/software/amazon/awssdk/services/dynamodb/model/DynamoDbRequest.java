package software.amazon.awssdk.services.dynamodb.model;

import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.model.DynamoDbRequest")
public class DynamoDbRequest {
    @NewField
    public Token token;
}
