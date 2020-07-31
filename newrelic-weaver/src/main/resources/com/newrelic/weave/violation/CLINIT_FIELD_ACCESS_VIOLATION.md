## CLINIT_FIELD_ACCESS_VIOLATION ##

###Description###

This violation was raised because the @Weave class is attempting to access a private or protected static field during class initialization. 

###Example###

####Original Class####
```
public class Example {

    public static String matchedPublicField = "public";

    private static String matchedPrivateField = "private";

    protected static String matchedProtectedField = "protected";

    static String matchedPackageField = "package-private";

}
```


####Bad####
```
@Weave
public class Example {

    private static String matchedPrivateField = Weaver.callOriginal();
    protected static String matchedProtectedField = Weaver.callOriginal();
    static String matchedPackageField = Weaver.callOriginal();

    @NewField
    private static String newFieldAssignedwithPrivateField = matchedPrivateField; // Causes violation

    private static String newProtectedField = matchedProtectedField; // Causes violation

    private static String newPackageField = matchedPackageField; // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    public static String matchedPublicField = Weaver.callOriginal();

    @NewField
    private static String newFieldAssignedwithPublicField = matchedPublicField;

}
```