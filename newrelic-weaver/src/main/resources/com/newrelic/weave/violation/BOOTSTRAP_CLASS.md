## BOOTSTRAP_CLASS ##

###Description###

This violation was raised because the @Weave class is attempting to match a bootstrap class but java.lang.instrument.Instrumentation was not provided to the weaver. In order to weave a bootstrap class we must have an implementation of the Instrumentation interface provided to the weaver.

###No Example###