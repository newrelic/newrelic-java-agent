## NON_VOID_NO_PARAMETERS_WEAVE_ALL_METHODS ##

###Description###

This violation was raised because the @Weave class contains a @WeaveIntoAllMethods that has a
non-void return type, and/or takes parameters.


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
    public String instrumentAll() {
        // instrumentation
    }
}
```

----------

####Also Bad####
```
@Weave
public class Example {

    @WeaveIntoAllMethods
    public instrumentAll(int i) {
        // instrumentation
    }
}
```

####Also Bad####
```
@Weave
public class Example {

    @WeaveIntoAllMethods
    public String instrumentAll(int i) {
        // instrumentation
        // return i
    }
}
```

----------

#### Good####
```
@Weave
public class Example {

    @WeaveIntoAllMethods
    public void instrumentAll() {
        // instrumentation
    }
}
```
