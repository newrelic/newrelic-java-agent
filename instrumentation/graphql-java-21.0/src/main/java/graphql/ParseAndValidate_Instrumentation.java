/*
 *
 *  * Copyright 2023 New Relic Corporation. All rights reserved.
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package graphql;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.nr.instrumentation.graphql.GraphQLTransactionName;
import graphql.language.Document;
import graphql.schema.GraphQLSchema;
import graphql.validation.ValidationError;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static com.nr.instrumentation.graphql.GraphQLErrorHandler.reportGraphQLError;
import static com.nr.instrumentation.graphql.GraphQLErrorHandler.reportGraphQLException;
import static com.nr.instrumentation.graphql.GraphQLSpanUtil.setOperationAttributes;

@Weave(originalName = "graphql.ParseAndValidate", type = MatchType.ExactClass)
public class ParseAndValidate_Instrumentation {

    public static ParseAndValidateResult parse(ExecutionInput executionInput) {
        ParseAndValidateResult result = Weaver.callOriginal();
        if (result != null) {
            String transactionName = GraphQLTransactionName.from(result.getDocument());
            NewRelic.getAgent().getTracedMethod().setMetricName("GraphQL/operation" + transactionName);
            setOperationAttributes(result.getDocument(), executionInput.getQuery());

            if (result.isFailure()) {
                reportGraphQLException(result.getSyntaxException());
                NewRelic.setTransactionName("GraphQL", "*");
            } else {
                NewRelic.setTransactionName("GraphQL", transactionName);
            }
        }
        return result;
    }

    public static List<ValidationError> validate(GraphQLSchema graphQLSchema, Document parsedDocument, Predicate<Class<?>> rulePredicate, Locale locale) {
        List<ValidationError> errors = Weaver.callOriginal();
        if (errors != null && !errors.isEmpty()) {
            reportGraphQLError(errors.get(0));
        }
        return errors;
    }

    public static List<ValidationError> validate(GraphQLSchema graphQLSchema, Document parsedDocument, Predicate<Class<?>> rulePredicate) {
        List<ValidationError> errors = Weaver.callOriginal();
        if (errors != null && !errors.isEmpty()) {
            reportGraphQLError(errors.get(0));
        }
        return errors;
    }
}
