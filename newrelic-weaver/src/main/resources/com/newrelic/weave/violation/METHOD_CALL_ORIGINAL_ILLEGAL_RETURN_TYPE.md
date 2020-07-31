## METHOD_CALL_ORIGINAL_ILLEGAL_RETURN_TYPE ##

###Description###

This violation was raised because the method in the @Weave class is attempting to use a different type than the original method.

###Example###

####Original Class####
```
public class Example {

    public boolean booleanMethod() {
        return true;
    }

}
```


####Bad####
```
@Weave
public class Example {

    public boolean booleanMethod() {
        String val = Weaver.callOriginal(); // violation - original returns boolean, not String
        return false;
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    public boolean booleanMethod() {
        boolean val = Weaver.callOriginal();
        return val;
    }

}
```