## INIT_WITH_ARGS_INTERFACE_MATCH_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is attempting to match an interface but contains a constructor with arguments. When doing an interface match, only the default no-argument constructor is allowed.

###Example###

####Original Class####
```
public interface Example {

}
```


####Bad####
```
@Weave(type = MatchType.Interface)
public class Example {

    private int someValue;

    // This will fail
    public Example(int someValue) {
        this.someValue = someValue;
    }

}
```

----------

####Good####
```
@Weave(type = MatchType.Interface)
public class Example {

    public Example() {
        
    }

}
```