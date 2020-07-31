## NON_STATIC_WEAVE_INTO_ALL_METHODS ##

###Description###

This violation was raised because the @WeaveIntoAllMethods found is non-static.
Make the @WeaveIntoAllMethods method static.

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
    private void instrumentation() {
       // instrumentation
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
       // instrumentation
    };
}
```

