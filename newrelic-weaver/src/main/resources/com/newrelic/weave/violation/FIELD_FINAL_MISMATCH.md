## FIELD_FINAL_MISMATCH ##

###Description###

This violation was raised because the field in the @Weave class is attempting to match a non-final field with a final field.

###Example###

####Original Class####
```
public class Example {

    private final String finalExample = "This is final";

}
```


####Bad####
```
@Weave
public class Example {

    private String finalExample = Weaver.callOriginal(); // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    private final String finalExample = Weaver.callOriginal();

}
```