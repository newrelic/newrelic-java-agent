## METHOD_SYNTHETIC_WEAVE_ILLEGAL ##

###Description###

This violation was raised because the @Weave class is attempting to match a synthetic method.

###Example###

####Original Class####
```
public class Example {
    private static String privateField = "I am private don't access me!!";

    public void methodThatUsesInnerClassToAccessPrivateField() {
        new Runnable() {
            @Override
            public void run() {
                privateField = "I accessed the private field!";
            }
        }.run();
    }
}
```


####Bad####
```
@Weave
public class Example {

    // This is not allowed
    public String access$002() {
        return Weaver.callOriginal();
    }

}
```