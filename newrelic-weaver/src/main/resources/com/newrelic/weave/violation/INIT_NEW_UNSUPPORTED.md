## INIT_NEW_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class attempted to add a constructor that doesn't exist in the original class.

###Example###

####Original Class####
```
public class Example {

    private String exampleValue;

    public Example(String exampleValue) {
        this.exampleValue = exampleValue;
    }

}
```


####Bad####
```
@Weave
public class Example {

    private int someValue;

    // This constructor doesn't exist in the original class
    public Example(int someValue) {
        this.someValue = someValue;
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    public Example(String exampleValue) {
        
    }

}
```