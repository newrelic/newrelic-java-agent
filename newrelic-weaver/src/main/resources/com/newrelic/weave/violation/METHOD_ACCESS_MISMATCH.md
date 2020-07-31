## METHOD_ACCESS_MISMATCH ##

###Description###

This violation was raised because the method access level specified in the @Weave class does not match the method access level from the original class.

###Example###

####Original Class####
```
public class Example {

    private String getExample() {
        return "Example";
    }

}
```


####Bad####
```
@Weave
public class Example {
    
    public String getExample() {
        return "Example";
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    private String getExample() {
        return "Something else";
    }

}
```