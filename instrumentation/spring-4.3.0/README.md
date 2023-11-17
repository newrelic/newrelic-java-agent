# spring-4.3.0 Instrumentation Module

This module provides instrumentation for Spring Controllers utilizing Spring Web-MVC v4.3.0 up to but not including v6.0.0.
(v6.0.0 instrumentation is provided by another module).

### Traditional Spring Controllers
The module will name transactions based on the controller mapping and HTTP method under the following scenarios:
- Single Spring controller class annotated with/without a class level `@RequestMapping` annotation and methods annotated
  with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` or `@PatchMapping`.
```java
@RestController
@RequestMapping("/root")
public class MyController {
    @GetMapping("/doGet")
    public String handleGet() {
        //Do something
    }
}
```

- A Spring controller class that implements an interface with/without an interface level `@RequestMapping` annotation and methods annotated
  with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` or `@PatchMapping`. In addition, the controller class
  can also implement methods not on the interface with the same annotations.
```java
@RequestMapping("/root")
public interface MyControllerInterface {
    @GetMapping("/doGet/{id}") 
    String get(@PathVariable String id);
    
    @PostMapping("/doPost") 
    String post();
}

@RestController
public class MyController implements MyControllerInterface {
    @Override
    String get(@PathVariable String id) {
        //Do something
    }

    @Override
    String post() {
        //Do something
    }
    
    //Method not defined in the interface
    @DeleteMapping("/doDelete")
    public String delete() {
        //Do something
    }
}
```

- A Spring controller class that extends another controller class with/without a class level `@RequestMapping` annotation and methods annotated
    with `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` or `@PatchMapping`. In addition, the controller class
    can also implement methods not on the parent controller with the same annotations.
```java
@RequestMapping("/root")
public abstract class MyCommonController {
    @GetMapping("/doGet")
    abstract public String doGet();
}

@RestController
public class MyController extends MyCommonController {
    @Override
    public String doGet() {
        //Do something
    }
}
```

The resulting transaction name will be the defined mapping route plus the HTTP method. For example: `root/doGet/{id} (GET)`.

### Other Controllers Invoked via DispatcherServlet

For any other controllers invoked via the `DispatcherServlet` ([Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.enabling) endpoints, for example)
will be named based on the controller class name and the executed method. For example: `NonStandardController/myMethod`.
