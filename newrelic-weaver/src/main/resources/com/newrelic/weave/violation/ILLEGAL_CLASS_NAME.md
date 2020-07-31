## ILLEGAL_CLASS_NAME ##

###Description###

This violation was raised because the class is not annotated with @Weave but the original class exists.

###Example###

####Original Class####
```
public class Example {

}
```


####Bad####
```
// Missing @Weave
public class Example {

}
```

----------

####Good####
```
@Weave
public class Example {

}
```