# Undertow Server Standalone Instrumentation

__Note:__ This module is disabled by default in order to not conflict with any Wildfly instrumentation module.
If you wish to enable the undertow-server module, add the following to your agent configuration file:
```yaml
  class_transformer:
    com.newrelic.instrumentation.undertow-server-1.1.0:
      enabled: true
```

This module is intended for applications using Undertow in stand alone mode, using any of the following
route handlers:
- io.undertow.server.RoutingHandler
- io.undertow.server.handlers.PathTemplateHandler
- io.undertow.predicate.PathTemplatePredicate

If any other handler is used, the transaction will be named `{connectors_placeholder_name}/`. This is
to prevent an explosion of unique transaction names when parameterized request paths are used.

This instrumentation module is not compatible with any Wildfly instrumentation modules.

Example code of supported handlers are below.

## Example Code

### RoutingHandler

```
    RoutingHandler routingHandler = new RoutingHandler();

    routingHandler.get("/greet/{name}", exchange -> {
        String name = exchange.getQueryParameters().get("name").getFirst();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Hello, " + name + "!");
    }).get("/static/content", exchange -> {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Static content");
    });
```

### PathTemplateHandler
```
    PathTemplateHandler pathTemplateHandler = new PathTemplateHandler();
    
    pathTemplateHandler.add("/greet/{name}/{count}", exchange -> {
        PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String name = pathMatch.getParameters().get("name");
        int count = Integer.parseInt(pathMatch.getParameters().get("count"));

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Hello " + name + String.valueOf("!").repeat(count));
    });
```

### PathTemplatePredicate (wrapped in a PredicateHandler Instance)
```
    HttpHandler matchedPathHandler = exchange -> {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Path matched!");
    };
    HttpHandler unmatchedPathHandler = exchange -> {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send("Path not matched.");
    };
    PredicateHandler predicateHandler = new PredicateHandler(
            new PathTemplatePredicate("/greet4/{name}", ExchangeAttributes.relativePath()),
            matchedPathHandler,
            unmatchedPathHandler
    );
```

### Full Example using RoutingHandler
```java
public class UndertowHttpServer {

    public static void main(String[] args) throws IOException {
        RoutingHandler routingHandler = new RoutingHandler();
        routingHandler.get("/greet/{name}", exchange -> {
            String name = exchange.getQueryParameters().get("name").getFirst();
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.setStatusCode(200);
            exchange.getResponseSender().send("Hello, " + name + "!");
        }).get("/static", exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.setStatusCode(200);
            exchange.getResponseSender().send("Static static static");
        });

        System.out.println("Building Undertow server");
        Undertow.Builder builder = Undertow.builder();
        Undertow undertow = builder
                .addHttpListener(8080, "localhost")
                .setHandler(routingHandler)
                .build();

        System.out.println("Undertow started");
        undertow.start();
    }
}
```