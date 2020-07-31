## CLASS_ACCESS_MISMATCH ##

###Description###

This violation was raised because the class access level specified in the @Weave class does not match the class access level from the original class.

###Example###

####Original Class####
```
class Example {

}
```


####Bad####
```
@Weave
public class Example {

}
```

----------

####Good####
```
@Weave
class Example {

}
```