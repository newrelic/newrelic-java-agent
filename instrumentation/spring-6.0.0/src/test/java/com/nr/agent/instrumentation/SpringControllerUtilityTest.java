package com.nr.agent.instrumentation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class SpringControllerUtilityTest {
    private final String httpMethod;
    private final String expectedPath;

    @Parameterized.Parameters(name = "{index}: retrieveMappingPathFromHandlerMethod(httpMethod: {0}) == path value: {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"GET", "/get"},
                {"PUT", "/put"},
                {"PATCH", "/patch"},
                {"POST", "/post"},
                {"DELETE", "/delete"}
        });
    }

    public SpringControllerUtilityTest(String httpMethod, String expectedPath) {
        this.httpMethod = httpMethod;
        this.expectedPath = expectedPath;
    }

    @Test
    public void retrieveMappingPathFromHandlerMethod_withValidHttpMethodStrings() throws NoSuchMethodException {
        Method method = MyController.class.getMethod(httpMethod.toLowerCase());
        String path = SpringControllerUtility.retrieveMappingPathFromHandlerMethod(method, httpMethod);
        Assert.assertEquals(expectedPath, path);
    }

    @Test
    public void retrieveMappingPathFromHandlerMethod_withUnknownHttpMethod_returnsNullPath()  throws NoSuchMethodException {
        Method method = MyController.class.getMethod(this.httpMethod.toLowerCase());
        Assert.assertNull(SpringControllerUtility.retrieveMappingPathFromHandlerMethod(method, "Unknown"));
    }

    @RequestMapping("/root")
    public static class MyController {
        @GetMapping("/get")
        public void get() {}

        @PostMapping("/post")
        public void post() {}

        @DeleteMapping("/delete")
        public void delete() {}

        @PutMapping("/put")
        public void put() {}

        @PatchMapping("/patch")
        public void patch() {}
    }
}
