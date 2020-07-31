## FIELD_FINAL_ASSIGNMENT ##

###Description###

This violation was raised because the field in the @Weave class is attempting to assign a value other than Weaver.callOriginal to a final field.

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

    private final String finalExample = "This doesn't work";

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