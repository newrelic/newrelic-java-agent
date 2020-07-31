## METHOD_NEW_ABSTRACT_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is attempting to define a new abstract method.

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
public abstract class Example {

    public abstract String newAbstractMethod(); // Causes violation

}
```