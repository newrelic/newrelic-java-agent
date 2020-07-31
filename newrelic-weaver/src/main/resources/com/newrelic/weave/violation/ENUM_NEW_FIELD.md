## ENUM_NEW_FIELD ##

###Description###

This violation was raised because the @Weave enum cannot have new fields.

###Example###

####Original Class####
```
public enum Example {
  FOO, BAR

}
```


####Bad####
```
@Weave
public enum Example {
    FOO, BAR;

    @NewField
    int bad;

}
```

----------

####Good####
```
@Weave
public enum Example {
    FOO, BAR

}
```