## FIELD_TYPE_MISMATCH ##

###Description###

This violation was raised because the field in the @Weave class is attempting to match using a different type than the original field.

###Example###

####Original Class####
```
public class Example {

    private String stringExample = "This is a String";

}
```


####Bad####
```
@Weave
public class Example {

    private int stringExample = Weaver.callOriginal(); // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    private String stringExample = Weaver.callOriginal();

}
```