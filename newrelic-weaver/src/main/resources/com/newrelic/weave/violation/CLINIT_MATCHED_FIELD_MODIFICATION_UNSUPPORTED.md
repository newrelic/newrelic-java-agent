## CLINIT_MATCHED_FIELD_MODIFICATION_UNSUPPORTED ##

###Description###

This violation was raised because the @Weave class is attempting to modify the value of a matched static field. 

###Example###

####Original Class####
```
public class Example {
    public static String assignedField;
    public static String originalField;
}
```


####Bad####
```
@Weave
public class Example {

    public static String assignedField = ""; // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    public static String originalField = Weaver.callOriginal(); // Only callOriginal is allowed

}
```