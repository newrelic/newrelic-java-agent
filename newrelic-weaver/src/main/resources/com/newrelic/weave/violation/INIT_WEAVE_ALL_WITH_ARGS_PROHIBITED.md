## INIT_WEAVE_ALL_WITH_ARGS_PROHIBITED ##

###Description###

This violation was raised because the @Weave class has a constructor annotated with @WeaveAllConstructor, and this constructor has arguments.
@WeaveAllConstructors can only be applied to default constructors.

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
    public Example(String exampleValue) {
      // @WeaveAllConstructor not allowed in constructors that take arguments.
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