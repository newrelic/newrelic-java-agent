## MULTIPLE_WEAVE_ALL_METHODS ##

###Description###

This violation was raised because the @Weave class contains multiple methods annotated with @WeaveIntoAllMethods.

###Example###

####Original Class####
```
public class Example {

    public int getNumber();
    public String getString();

}
```


####Bad####
```
@Weave
public class Example {

    @WeaveIntoAllMethods
    private static void instrumentation() {
       // record count
    };

    @WeaveIntoAllMethods
    private static void secondInstrumentation() {
       // log
    };

}
```

----------

####Good####
```
@Weave
public class Example {

    @WeaveIntoAllMethods
    private static void instrumentation() {
       // record count
       // log
    };
}
```

