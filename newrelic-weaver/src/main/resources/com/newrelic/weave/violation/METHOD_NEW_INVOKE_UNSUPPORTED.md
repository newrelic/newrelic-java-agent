## METHOD_NEW_INVOKE_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class contains a new method that is attempting to call another new method.

###Example###

####Original Class####
```
public class Example {

    public String exampleMethod() {
        return "example";
    }

}
```


####Bad####
```
@Weave
public class Example {

    private String newMethod() {
        return anotherNewMethod(); // This is not allowed
    }

    private String anotherNewMethod() {
        return "Some value";
    }

}
```