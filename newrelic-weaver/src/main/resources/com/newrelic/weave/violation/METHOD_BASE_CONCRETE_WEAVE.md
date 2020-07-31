## METHOD_BASE_CONCRETE_WEAVE ##

###Description###

This violation was raised because the @Weave class is trying to weave a method on a class that is implemented in a superclass. It is only possible to weave methods that are implemented in a class, not methods from its superclass.

###Example###

####Original Class####
```
public abstract class ExampleParent {

    public void concreteMethod() {
    
    }

}
```

```
public abstract class Example extends ExampleParent {

    public void anotherConcreteMethod() {
    
    }

}
```


####Bad####
```
@Weave(type = MatchType.BaseClass)
public class Example {

    public void concreteMethod() {
        // we can't weave this because it's implemented in ExampleParent and may not be implemented in concrete subclasses
    }

}
```