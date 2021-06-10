package software.amazon.awssdk.services.dynamodb;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.function.Consumer;

@Weave(originalName = "software.amazon.awssdk.services.dynamodb.DynamoDbClient", type = MatchType.Interface)
public class DynamoDbClient_Instrumentation {
    public BatchExecuteStatementResponse batchExecuteStatement(
            Consumer<BatchExecuteStatementRequest.Builder> batchExecuteStatementRequest) throws RequestLimitExceededException,
            InternalServerErrorException, AwsServiceException, SdkClientException, DynamoDbException {
        return Weaver.callOriginal();
    }
}
