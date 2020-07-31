## UNEXPECTED_NEW_FIELD_ANNOTATION ##

###Description###

This violation was raised because the @Weave class contains a field that matched against the original class and has a @NewField annotation. A field in an @Weave class must match one (and only one) of these options:

1. Annotated with @NewField and NOT existing in the original class
2. NOT Annotated with @Newfield and existing in the original class


###Example###

####Original Class####
```
public class Example {

    public int exampleField;

}
```


####Bad####
```
@Weave
public class Example {

    @NewField
    public int exampleField;

}
```

----------

####Good####
```
@Weave
public class Example {

    public int exampleField;

}
```

----------

####Also Good####
```
@Weave
public class Example {

    @NewField
    public int myOwnField;

}
```