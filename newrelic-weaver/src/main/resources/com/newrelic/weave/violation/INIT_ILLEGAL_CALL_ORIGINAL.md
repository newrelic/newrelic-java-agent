## INIT_ILLEGAL_CALL_ORIGINAL ##

###Description###

This violation was raised because the @Weave class contains a @NewField that attempted to set it's value with a call to Weaver.callOriginal() or it attempted to use Weaver.callOriginal() in the constructor.

###Example 1###

####Original Class####
```
public class Example {

}
```


####Bad####
```
@Weave
public class Example {

    @NewField
    private String newField = Weaver.callOriginal(); // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    @NewField
    private String newField = "My value";

}
```

----------
----------


###Example 2###

####Original Class####
```
public class Example {

    private int parameter;

    public Example(int parameter) {
        this.parameter = parameter;
    }
}
```


####Bad####
```
@Weave
public class Example {

    public Example(int parameter) {
        Weaver.callOriginal(); // Causes violation
    }

}
```

----------

####Good####
```
@Weave
public class Example {

    public Example(int parameter) {
        // No need to Weaver.callOriginal() here
    }

}
```