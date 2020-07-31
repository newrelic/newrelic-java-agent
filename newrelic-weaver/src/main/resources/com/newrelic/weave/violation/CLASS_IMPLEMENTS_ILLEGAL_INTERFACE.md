## CLASS_IMPLEMENTS_ILLEGAL_INTERFACE ##

###Description###

This violation was raised because the @Weave class implements an interface that the original class does not implement. @Weave classes must either implement the same interface as the original or not implement anything.

###Example###

####Original Class####
```
public class Example implements ExampleInterface {

}
```


####Bad####
```
@Weave
public class Example implements SomeRandomInterface {

}
```

----------

####Good####
```
@Weave
public class Example implements ExampleInterface {

}
```

----------

####Also Good####
```
@Weave
public class Example {

}
```