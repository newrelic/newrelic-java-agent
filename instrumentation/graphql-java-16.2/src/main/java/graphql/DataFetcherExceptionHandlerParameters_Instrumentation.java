package graphql;

import com.newrelic.agent.bridge.NoOpTransaction;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Transaction;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import graphql.execution.DataFetcherExceptionHandlerParameters;

import static com.nr.instrumentation.graphql.GraphQLErrorHandler.*;

@Weave(originalName = "graphql.execution.DataFetcherExceptionHandlerParameters", type = MatchType.ExactClass)
public class DataFetcherExceptionHandlerParameters_Instrumentation {

    @Weave(originalName = "graphql.execution.DataFetcherExceptionHandlerParameters$Builder", type = MatchType.ExactClass)
    public static class DataFetcherExceptionHandlerParametersBuilder_Instrumentation {
        public DataFetcherExceptionHandlerParameters build() {
            DataFetcherExceptionHandlerParameters result = Weaver.callOriginal();
            Transaction tx = NewRelic.getAgent().getTransaction();
            if (result != null && !(tx instanceof NoOpTransaction)) {
                reportResolverThrowableToNR(result.getException());
            }
            return result;
        }
    }
}
