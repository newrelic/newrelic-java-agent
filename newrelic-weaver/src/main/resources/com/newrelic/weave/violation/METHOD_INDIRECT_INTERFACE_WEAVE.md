## METHOD_INDIRECT_INTERFACE_WEAVE ##

###Description###

This violation was raised because the @Weave class with a match type of Interface is attempting to weave a method that is inherited from an extended interface.  Only methods declared in an interface can be weaved.

###Example###

####Original Class####
```
public interface Indirect {
    void indirectMethod();
}
```

```
public interface Direct extends Indirect {
    void directMethod();
}
```


####Bad####
```
@Weave(type = MatchType.Interface)
public class Direct {
    public void directMethod() {
    }

    public void indirectMethod() {
        // Causes violation
    }
}
```