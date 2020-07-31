## METHOD_CALL_ORIGINAL_ALLOWED_ONLY_ONCE ##

###Description###

This violation was raised because the method in the @Weave class is attempting to call Weaver.callOriginal() multiple times.

###Example###

####Original Class####
```
public class Example {

    public String exampleMethod() {
        return "example";
    }

}
```


####Bad####
```
@Weave
public class Example {

    public String exampleMethod() {
        String value = "Some value";
        if (value.equals("Some value")) {
            value += Weaver.callOriginal();
        } else {
            value = Weaver.callOriginal();
        }
        return value;
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    public String exampleMethod() {
        String value = "Some value";
        String original = Weaver.callOriginal();

        if (value.equals("Some value")) {
            value += original;
        } else {
            value = original;
        }
        return value;
    }

}
```