## MISSING_ORIGINAL_BYTECODE ##

###Description###

This violation was raised because a match for the @Weave class could be not be found on the Classloader. To fix this, ensure that the class file you are trying to match is named correctly, is in the correct package and that it actually exists.

If the @Weave class does not have the same name as the original class then the originalName property in the @Weave annotation can be used and must match the original class package + class name.

###No Example###