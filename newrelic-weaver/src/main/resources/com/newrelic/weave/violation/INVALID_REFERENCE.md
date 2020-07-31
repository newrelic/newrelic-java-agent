## INVALID_REFERENCE ##

###Description###

This violation was raised because there is code in the weave package that references a class, method or field that doesn't exist in the current Classloader. If writing a Scala module, this could be caused by compiling against a newer version of Scala.

###No Example###