## CLASS_NESTED_IMPLICIT_OUTER_ACCESS_UNSUPPORTED ##

###Description###

This violation was raised because a method in a nested @Weave class is attempting to implicitly access an outer field/method.


###Example###

####Original Class####
```
public class Example {
    private int notAllowed;
}
```


####Bad####
```
@Weave
public class Example {
    private int notAllowed;

    public class InnerExample {
        public int field = notAllowed; // Causes violation
    }
}
```