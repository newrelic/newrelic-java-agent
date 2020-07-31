## CLINIT_METHOD_ACCESS_VIOLATION ##

###Description###

This violation was raised because the @Weave class is attempting to access a private or protected static method during class initialization. 

###Example###

####Original Class####
```
public class Example {

    public static String matchedPublicMethod() {
        return "publicMethod";
    }

    private static String matchedPrivateMethod() {
        return "privateMethod";
    }

    protected static String matchedProtectedMethod() {
        return "protectedMethod";
    }

    static String matchedPackageMethod() {
        return "packageMethod";
    }

}
```


####Bad####
```
@Weave
public class Example {

    private static String matchedPrivateMethod() {
        return Weaver.callOriginal();
    }

    protected static String matchedProtectedMethod() {
        return Weaver.callOriginal();
    }

    static String matchedPackageMethod() {
        return Weaver.callOriginal();
    }

    private static String newPrivateField = matchedPrivateMethod(); // Causes violation
    private static String newProtectedField = matchedProtectedMethod(); // Causes violation
    private static String newPackageField = matchedPackageMethod(); // Causes violation

}
```

----------

####Good####
```
@Weave
public class Example {

    public static String matchedPublicMethod() {
        return Weaver.callOriginal();
    }

    private static String newPublicField = matchedPublicMethod();

}
```