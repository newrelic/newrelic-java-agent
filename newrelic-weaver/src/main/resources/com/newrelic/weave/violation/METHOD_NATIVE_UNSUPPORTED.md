## METHOD_NATIVE_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is attempting to match a native method.

###Example###

####Original Class####
```
public class Example {

    native void nativeMethod();

}
```


####Bad####
```
@Weave
public class Example {

    native void nativeMethod() {
        // This will fail
    }

}
```
