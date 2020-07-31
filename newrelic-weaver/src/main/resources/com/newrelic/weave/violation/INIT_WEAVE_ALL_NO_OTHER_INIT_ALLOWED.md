## INIT_WEAVE_ALL_NO_OTHER_INIT_ALLOWED ##

###Description###

This violation was raised because the @Weave class has a constructor annotated with @WeaveAllConstructor and contains other constructors.
When @WeaveAllConstructors is present, the @Weave class should only have one constructor.

###Example###

####Original Class####
```
public class Example {

    public Example() {
    }

    public Example(String exampleValue) {
    }

}
```


####Bad####
```
@Weave
public class Example {


    @WeaveAllConstructor
    public Example() {
    }

    public Example(String exampleValue) {
      // This additional constructor is not allowed.
    }

}
```

----------

####Good####
```
@Weave
public class Example {


    @WeaveAllConstructor
    public Example() {
    }

}
```