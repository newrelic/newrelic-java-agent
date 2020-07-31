## METHOD_MISSING_REQUIRED_ANNOTATIONS ##

###Description###

This violation was raised because the original method is missing the annotations specied in the weave method.

###Example###

####Original Class####
```
public class Example {

  @com.example.MethodAnnotation
  public void foo() {
  }

}
```


####Bad####
```
public class Example {

  @WeaveWithAnnotation(annotationClasses = "@com.example.SomeOtherAnnotation")
  public void foo() {
  }

}
```

----------

####Good####
```
public class Example {
  @WeaveWithAnnotation(annotationClasses = "com.example.MethodAnnotation")
  public void foo() {
  }

}
```
