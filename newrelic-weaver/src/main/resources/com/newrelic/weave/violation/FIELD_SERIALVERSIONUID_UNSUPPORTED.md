## FIELD_SERIALVERSIONUID_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is attempting to match on the Java internal serialVersionUID field.

###Example###

####Original Class####
```
public class Example {

    public static long serialVersionUID = -1L;

}
```


####Bad####
```
@Weave
public class Example {

    public static long serialVersionUID = Weaver.callOriginal();

}
```