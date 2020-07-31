package com.newrelic.agent.xml;

import com.newrelic.agent.extension.beans.Extension.Instrumentation.Pointcut.Method;
import com.newrelic.agent.extension.beans.MethodParameters;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class MethodHolder {

    /**
     * The key is the name of the method. The value is a list of parameter descriptors for which go with the name of the
     * method.
     */
    private final Map<String, List<String>> nameToMethods;

    /** True means the debug flag should be on. */
    private final boolean isDebug;

    /**
     * 
     * Creates this MethodHolder.
     * 
     * @param pDebug True means debug should be on.
     */
    public MethodHolder(final boolean pDebug) {
        nameToMethods = new HashMap<>();
        isDebug = pDebug;
    }

    /**
     * Adds the input methods to the list of methods which are present.
     * 
     * @param methods The methods to be added.
     */
    protected void addMethods(final List<Method> methods) {
        if (methods != null) {
            for (Method m : methods) {
                if ((m != null) && (m.getParameters() != null)) {
                    addMethod(m.getName(), MethodParameters.getDescriptor(m.getParameters()));
                }
            }
        }
    }

    /**
     * Adds the method to the map.
     * 
     * @param name The name of the method.
     * @param descr The parameter descriptor of the method.
     */
    private void addMethod(String name, String descr) {
        // be sure to trim first
        name = name.trim();
        descr = descr.trim();

        List<String> value = nameToMethods.get(name);
        if (value == null) {
            value = new ArrayList<>();
            nameToMethods.put(name, value);
        }

        if (!value.contains(descr)) {
            value.add(descr);
        }
    }

    /**
     * True means the method is present. False means the method is not found in the map.
     * 
     * @param name The name of the method.
     * @param descr The descriptor for the method.
     * @param remove True means the method should be removed if found in the map.
     * @return True if the method is present, else false.
     */
    protected boolean isMethodPresent(String name, String descr, boolean remove) {
        name = name.trim();
        descr = descr.trim();

        List<String> value = nameToMethods.get(name);
        if (value != null) {
            if (isDebug) {
                XmlInstrumentValidator.printMessage(MessageFormat.format("Found the method {0} from the xml"
                        + " in the list of class methods. Checking method parameters.", name));
            }
            Iterator<String> it = value.iterator();
            while (it.hasNext()) {
                String xmlDesc = it.next();
                if (descr.startsWith(xmlDesc)) {
                    XmlInstrumentValidator.printMessage(MessageFormat.format("Matched Method: {0} {1}", name, descr));
                    // remove them too, so we can keep track of which ones we
                    // left
                    if (remove) {
                        it.remove();
                        if (value.isEmpty()) {
                            nameToMethods.remove(name);
                        }
                    }
                    return true;
                } else if (isDebug) {
                    XmlInstrumentValidator.printMessage(MessageFormat.format("Descriptors for method {0} did not"
                            + " match. Xml descriptor: {1}, Method descriptor: {2} ", name, xmlDesc, descr));
                }
            }
        }
        return false;
    }

    /**
     * True means there are still methods left in the method holder.
     * 
     * @return True if there are still methods left in the method holder, else false.
     */
    protected boolean hasMethods() {
        Iterator<Entry<String, List<String>>> it = nameToMethods.entrySet().iterator();
        return (it.hasNext());
    }

    /**
     * Returns the methods which are still present.
     * 
     * @return List of methods which are still present in the map.
     */
    protected String getCurrentMethods() {
        StringBuilder sb = new StringBuilder();
        Iterator<Entry<String, List<String>>> it = nameToMethods.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, List<String>> values = it.next();
            List<String> descriptors = values.getValue();
            if ((descriptors != null) && (!descriptors.isEmpty())) {
                sb.append("\nMethod Name: ");
                sb.append(values.getKey());
                sb.append(" Param Descriptors: ");
                for (String v : descriptors) {
                    sb.append(v);
                    sb.append(" ");
                }
            }

        }
        return sb.toString();
    }
}
