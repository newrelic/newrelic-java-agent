## METHOD_NEW_NON_PRIVATE_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class contains a new method that is not declared private.

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

    // This needs to be private
    public String newMethod() {
        return "Some value";
    }

}
```