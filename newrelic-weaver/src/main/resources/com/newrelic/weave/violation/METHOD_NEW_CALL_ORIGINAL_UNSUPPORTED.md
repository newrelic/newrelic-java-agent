## METHOD_NEW_CALL_ORIGINAL_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class contains a new method that is attempting to call Weaver.callOriginal().

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
        return Weaver.callOriginal(); // This method doesn't exist in the original
    }

}
```