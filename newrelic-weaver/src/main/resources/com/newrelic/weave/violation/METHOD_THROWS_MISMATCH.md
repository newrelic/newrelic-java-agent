## METHOD_THROWS_MISMATCH ##

###Description###

This violation was raised because the throws clause specified in the @Weave class does not match the throws clause 
from the original class. This violation will only be thrown when an Exception type is used that doesn't exist in the
original class. But it completely acceptable to not specify any Exceptions or even just a subset from the original.

###Example###

####Original Class####
```
public class Example {

    private String getExample() throws IOException {
        if (shouldThrowException()) {
            throws new IOException();
        }
        return "Example";
    }

}
```


####Bad####
```
@Weave
public class Example {
    
    public String getExample() throws OutOfMemoryError {
        if (shouldThrowException()) {
            throws new OutOfMemoryError();
        }
        return "Example";
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    private String getExample() throws IOException {
        if (shouldThrowException()) {
            throws new IOException(); // This will throw out to customer code
        }
        return "Something else";
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    private String getExample() throws IOException {
        return "Something else";
    }

}
```