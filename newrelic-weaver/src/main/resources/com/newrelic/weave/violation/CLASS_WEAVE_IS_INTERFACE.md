## CLASS_WEAVE_IS_INTERFACE ##

###Description###

This violation was raised because an @Weave annotation was found on the interface listed in the violation. @Weave annotations can only be applied to classes, not to interfaces.

###Example###

####Bad####
```
@Weave(type = MatchType.Interface)
public interface Example {

}
```

----------

####Good####
```
@Weave(type = MatchType.Interface)
public class Example {

}
```