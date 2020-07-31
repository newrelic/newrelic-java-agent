## CLASS_MISSING_REQUIRED_ANNOTATIONS ##

###Description###

This violation was raised because the annotations specified in @WeaveWithAnnotations weave class are missing in the original class.

###Example###

####Original Class####
```
@com.example.Annotation
public class Example {

}
```


####Bad####
```
@WeaveWithAnnotation(annotationClasses = "com.example.OtherAnnotation")
public class Example {

}
```

----------

####Good####
```
@WeaveWithAnnotation(annotationClasses = "com.example.Annotation")
public class Example {

}
```
