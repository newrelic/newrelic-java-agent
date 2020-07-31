## CLASS_EXTENDS_ILLEGAL_SUPERCLASS ##

###Description###

This violation was raised because the @Weave class extends a superclass that the original class does not extend. @Weave classes must either extend the same superclass as the original or not extend anything (java.lang.Object by default).

###Example###

####Original Class####
```
public class Example extends SuperExample {

}
```


####Bad####
```
@Weave
public class Example extends SomeRandomSuperClass {

}
```

----------

####Good####
```
@Weave
public class Example extends SuperExample {

}
```

----------

####Also Good####
```
@Weave
public class Example {

}
```