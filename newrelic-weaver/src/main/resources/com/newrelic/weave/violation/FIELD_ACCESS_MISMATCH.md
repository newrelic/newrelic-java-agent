## FIELD_ACCESS_MISMATCH ##

###Description###

This violation was raised because the field access level specified in the @Weave class does not match the field access level from the original class.

###Example###

####Original Class####
```
public class Example {

    private String privateExample;

}
```


####Bad####
```
@Weave
public class Example {
    
    public String privateExample;

}
```

----------

####Good####
```
@Weave
public class Example {

    private String privateExample;

}
```