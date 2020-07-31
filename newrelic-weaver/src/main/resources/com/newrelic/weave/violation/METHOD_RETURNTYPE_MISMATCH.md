## METHOD_RETURNTYPE_MISMATCH ##

###Description###

This violation was raised because the method in the @Weave class is attempting to return a value using a different type than the original method.

###Example###

####Original Class####
```
public class Example {

    private String stringMethod() {
        return "This is a string";
    }

}
```


####Bad####
```
@Weave
public class Example {

    private int stringMethod() {
        return Weaver.callOriginal(); // Causes violation
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    private String stringMethod() {
        return Weaver.callOriginal();
    }

}
```