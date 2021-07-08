package software.amazon.awssdk.services.dynamodb;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class DefaultDynamoDbClient_InstrumentationTagMethodsTest {
    @Test
    public void testTagMethodsAreInstrumented() {
        // when
        List<String> methodNames = extractMethodNamesFrom(DefaultDynamoDbClient_Instrumentation.class);
        // then
        tagMethodNamesAreIncludedIn(methodNames);
    }

    @Test
    public void testTagAsyncMethodsAreInstrumented() {
        // when
        List<String> methodNames = extractMethodNamesFrom(DefaultDynamoDbAsyncClient_Instrumentation.class);
        // then
        tagMethodNamesAreIncludedIn(methodNames);
    }

    private List<String> extractMethodNamesFrom(Class<?> klass) {
        return Arrays.stream(klass.getMethods())
                .map(Method::getName)
                .collect(Collectors.toList());
    }

    private void tagMethodNamesAreIncludedIn(List<String> methodNames) {
        assertThat(methodNames, hasItem("listTagsOfResource"));
        assertThat(methodNames, hasItem("tagResource"));
        assertThat(methodNames, hasItem("untagResource"));
    }
}
