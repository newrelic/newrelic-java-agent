package com.newrelic.agent.xml;

import java.io.File;
import java.text.MessageFormat;

public class XmlInstrumentParams {

    /** This should be the full path to the file. */
    private File filePath;

    /** True means debug will be turned on. */
    private boolean debug = false;

    /**
     * 
     * Creates this XmlInstrumentParams.
     */
    public XmlInstrumentParams() {
        super();
    }

    /**
     * Validates and sets the field file.
     * 
     * @param pFile the file to set
     * @param tagName The command line name.
     */
    public void setFile(final String[] pFile, final String tagName) {
        String fileName = verifyOne(pFile, tagName);
        filePath = new File(fileName);
        if (!filePath.exists()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The file specified with the tag {0} does not exist.", tagName));
        } else if (!filePath.isFile()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The file specified with the tag {0} must be a file and is not.", tagName));
        } else if (!filePath.canRead()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "The file specified with the tag {0} must be readable and is not.", tagName));
        }
    }

    /**
     * Returns the file path.
     * 
     * @return File which contains the extension xml.
     */
    public File getFile() {
        return filePath;
    }

    /**
     * Sets the debug flag.
     * 
     * @param pDebug
     * @param tagName
     */
    public void setDebug(final String[] pDebug, final String tagName) {
        String value = verifyOneOrNone(pDebug, tagName);
        if (value != null) {
            debug = Boolean.parseBoolean(value);
        }
    }

    /**
     * Gets the debug flag.
     * 
     * @return Gets the debug flag.
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * Verifies that there is one in the array. Returns the value if there is only one. Throws an illegal argument
     * exception if there are multiple values in the array or zero values in the array.
     * 
     * @param value The input array.
     * @param tagName The name to print in exceptions.
     * @return The value if present, else null.
     */
    private String verifyOne(final String[] value, final String tagName) {
        String toReturn = null;
        if ((value != null) && (value.length == 1)) {
            toReturn = value[0];
            if (toReturn != null) {
                toReturn = toReturn.trim();
            } else {
                throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.",
                        tagName));
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.", tagName));
        }
        return toReturn;
    }

    /**
     * Verifies that there is one or zero objects in the array. Returns the value if there is only one. Returns null if
     * there are zero objects. Throws an illegal argument exception if there are multiple values in the array.
     * 
     * @param value The input array.
     * @param tagName The name to print in exceptions.
     * @return The value if present, else null.
     */
    private String verifyOneOrNone(final String[] value, final String tagName) {
        String toReturn = null;
        if (value == null) {
            return null;
        } else if (value.length == 1) {
            toReturn = value[0];
            if (toReturn == null) {
                return null;
            } else {
                toReturn = toReturn.trim();
            }
        } else {
            throw new IllegalArgumentException(MessageFormat.format("One {0}, and only one {0}, must be set.", tagName));
        }
        return toReturn;
    }

}
