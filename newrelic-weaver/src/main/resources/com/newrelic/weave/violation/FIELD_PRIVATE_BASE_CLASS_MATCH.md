## FIELD_PRIVATE_BASE_CLASS_MATCH ##

###Description###

This violation was raised because the @Weave class is a base class match and references a private field from the original class which child classes will not have access to.

###Example###

####Original Class####
```
public class Example {

    private String exampleField = "This is private";

}
```


####Bad####
```
@Weave(type = MatchType.BaseClass)
public class Example {

    // Child classes will not be able to access this
    private String exampleField = Weaver.callOriginal();

}
```