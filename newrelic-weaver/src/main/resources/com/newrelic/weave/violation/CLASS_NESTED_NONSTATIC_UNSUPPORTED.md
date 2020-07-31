## CLASS_NESTED_NONSTATIC_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is a non-static inner class. In order for an inner class to be weaved it must be static.

###Example###

####Original Class####
```
public class Example {

    public class InnerExample {
        
    }

}
```


####Bad####
```
@Weave
public class Example {

    @Weave
    public class InnerExample {
        
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    @Weave
    public static class InnerExample {
        
    }

}
```