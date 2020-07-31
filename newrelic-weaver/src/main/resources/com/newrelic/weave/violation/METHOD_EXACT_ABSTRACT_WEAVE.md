## METHOD_EXACT_ABSTRACT_WEAVE ##

###Description###

This violation was raised because the @Weave class is an Exact match but it is attempting to match on an abstract method.

###Example###

####Original Class####
```
public abstract class Example {

    public abstract String abstractMethod();

}
```


####Bad####
```
@Weave
public class Example {

    public String abstractMethod() {
        
    }

}
```

----------

####Good####
```
@Weave(type = MatchType.BaseClass)
public class Example {

    public String abstractMethod() {
        
    }

}
```