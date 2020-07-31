## METHOD_STATIC_MISMATCH ##

###Description###

This violation was raised because the method in the @Weave class is attempting to match a non-static method with a static method.

###Example###

####Original Class####
```
public class Example {

    private static String staticMethod {
        return "This is static";
    }

}
```


####Bad####
```
@Weave
public class Example {

    private String staticMethod {
        return Weaver.callOriginal(); // Causes violation
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    private static String staticMethod {
        return Weaver.callOriginal(); // Causes violation
    }

}
```