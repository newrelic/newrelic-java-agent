## FIELD_STATIC_MISMATCH ##

###Description###

This violation was raised because the field in the @Weave class is attempting to match a non-static field with a static field.

###Example###

####Original Class####
```
public class Example {

    private static String staticExample = "This is static";

}
```


####Bad####
```
@Weave
public class Example {

    private String staticExample = Weaver.callOriginal(); // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    private static String staticExample = Weaver.callOriginal();

}
```