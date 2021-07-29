package graphql;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;

import java.util.List;

import static com.nr.instrumentation.graphql.GraphQLErrorHelper.reportGraphQLException;
import static com.nr.instrumentation.graphql.GraphQLErrorHelper.reportGraphQLError;

@Weave(originalName = "graphql.ParseAndValidate", type = MatchType.ExactClass)
public class ParseAndValidate_Instrumentation {
    @Trace
    public static ParseAndValidateResult parse(ExecutionInput executionInput) {
        //fixme  with correct atttribute value
        AgentBridge.privateApi.addTracerParameter("graphql.attribute", "parse method");
        ParseAndValidateResult result = Weaver.callOriginal();
        if(result != null && result.isFailure()) {
            reportGraphQLException(result.getSyntaxException());
        }
        return result;
    }

    @Trace
    public static List<ValidationError> validate(GraphQLSchema graphQLSchema, Document parsedDocument) {
        //todo: fix with correct atttribute value
        AgentBridge.privateApi.addTracerParameter("graphql.attribute", "validate method");
        List<ValidationError> errors = Weaver.callOriginal();
        if (errors != null && !errors.isEmpty()) {
            reportGraphQLError(errors.get(0));
        }
        return errors;
    }
}
